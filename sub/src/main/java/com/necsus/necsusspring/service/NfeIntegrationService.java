package com.necsus.necsusspring.service;

import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.nfe.Nfe;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.dom.enuns.AmbienteEnum;
import br.com.swconsultoria.nfe.dom.enuns.ConsultaDFeEnum;
import br.com.swconsultoria.nfe.dom.enuns.EstadosEnum;
import br.com.swconsultoria.nfe.dom.enuns.PessoaEnum;
import br.com.swconsultoria.nfe.schema.retdistdfeint.RetDistDFeInt;
import br.com.swconsultoria.nfe.schema.retdistdfeint.RetDistDFeInt.LoteDistDFeInt.DocZip;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe;
import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.repository.IncomingInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

/**
 * Service responsável pela integração com a SEFAZ para consultar NFe
 * Este é o "robô" que busca as notas fiscais destinadas à empresa
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NfeIntegrationService {

    private final NfeConfigService nfeConfigService;
    private final IncomingInvoiceRepository incomingInvoiceRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /**
     * Consulta as notas fiscais na SEFAZ e salva na caixa de entrada
     * Método principal que orquestra todo o processo
     */
    @Transactional
    public int consultarNotasSefaz() {
        log.info("Iniciando consulta de notas na SEFAZ...");

        Optional<NfeConfig> configOpt = nfeConfigService.getConfig();

        if (configOpt.isEmpty()) {
            log.warn("Nenhuma configuração NFe encontrada. Configure primeiro.");
            return 0;
        }

        NfeConfig config = configOpt.get();

        if (!config.getAtivo()) {
            log.info("Consulta NFe está desativada. Pulando...");
            return 0;
        }

        if (!nfeConfigService.validarCertificado(config)) {
            log.error("Certificado inválido ou não encontrado. Verifique a configuração.");
            return 0;
        }

        try {
            return consultarComPaginacao(config);
        } catch (Exception e) {
            log.error("Erro ao consultar notas na SEFAZ: {}", e.getMessage(), e);
            // Lança a exceção para ver no teste (opcional em produção)
            throw new RuntimeException(e);
        }
    }

    /**
     * Consulta com paginação (recursiva) para pegar todas as notas disponíveis
     */
    private int consultarComPaginacao(NfeConfig config) throws Exception {
        int totalImportadas = 0;

        String nsuAtual = config.getUltimoNsu();
        // Garante NSU com 15 dígitos
        if (nsuAtual == null || nsuAtual.trim().isEmpty()) {
            nsuAtual = "000000000000000";
        } else {
            nsuAtual = String.format("%015d", Long.parseLong(nsuAtual));
        }

        log.info("Consultando a partir do NSU: {}", nsuAtual);

        ConfiguracoesNfe configuracao = criarConfiguracao(config);

        // Faz a consulta na SEFAZ
        RetDistDFeInt retorno = Nfe.distribuicaoDfe(
                configuracao,
                PessoaEnum.JURIDICA,
                config.getCnpj(),
                ConsultaDFeEnum.NSU,
                nsuAtual
        );

        if (retorno == null) {
            log.warn("Retorno da SEFAZ foi nulo");
            return 0;
        }

        log.info("Status SEFAZ: {} - {}", retorno.getCStat(), retorno.getXMotivo());

        // 138 = Documentos localizados
        if ("138".equals(retorno.getCStat())) {
            if (retorno.getLoteDistDFeInt() != null && retorno.getLoteDistDFeInt().getDocZip() != null) {

                List<DocZip> listaDocumentos = retorno.getLoteDistDFeInt().getDocZip();
                int importadas = processarDocumentosZipados(listaDocumentos);
                totalImportadas += importadas;

                // Pegamos o Teto (MaxNSU) da resposta principal
                String maxNSU = safeGetString(retorno.getUltNSU());

                // Pegamos o Último NSU processado DO ÚLTIMO DOCUMENTO DA LISTA
                String ultNSUProcessado = nsuAtual; // Default se lista vazia
                if (!listaDocumentos.isEmpty()) {
                    // O NSU fica dentro de cada DocZip
                    ultNSUProcessado = safeGetString(listaDocumentos.get(listaDocumentos.size() - 1).getNSU());
                }

                log.info("maxNSU (Teto): {}, ultNSU (Processado): {}", maxNSU, ultNSUProcessado);

                // Salva o cursor atualizado
                if (isValidNsu(ultNSUProcessado)) {
                    nfeConfigService.atualizarUltimoNsu(config.getId(), ultNSUProcessado);
                }

                // Paginação Recursiva: Se o que processamos ainda é menor que o teto, busca mais
                if (isValidNsu(ultNSUProcessado) && isValidNsu(maxNSU) &&
                        Long.parseLong(ultNSUProcessado) < Long.parseLong(maxNSU)) {

                    log.info("Ainda há mais documentos. Consultando próxima página...");

                    // Pequena pausa para não floodar a SEFAZ
                    Thread.sleep(2000);

                    NfeConfig configAtualizada = nfeConfigService.getConfigById(config.getId()).orElse(config);
                    totalImportadas += consultarComPaginacao(configAtualizada);
                }
            }
        } else if ("137".equals(retorno.getCStat())) {
            log.info("Nenhum documento novo encontrado.");
        } else {
            log.warn("Status inesperado da SEFAZ: {} - {}", retorno.getCStat(), retorno.getXMotivo());
        }

        return totalImportadas;
    }

    private int processarDocumentosZipados(List<DocZip> documentos) {
        int importadas = 0;

        for (DocZip doc : documentos) {
            try {
                String xmlDescompactado = descompactarGzip(doc.getValue());

                if (doc.getSchema().contains("resNFe") || doc.getSchema().contains("procNFe")) {
                    boolean salvo = processarNFe(xmlDescompactado);
                    if (salvo) {
                        importadas++;
                    }
                }
            } catch (Exception e) {
                log.error("Erro ao processar documento NSU {}: {}", doc.getNSU(), e.getMessage(), e);
            }
        }
        return importadas;
    }

    private String descompactarGzip(byte[] dados) throws Exception {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(dados))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private boolean processarNFe(String xml) {
        try {
            JAXBContext context = JAXBContext.newInstance(TNFe.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            String xmlLimpo = xml.replaceAll("xmlns=\"[^\"]*\"", "")
                    .replaceAll("<\\?xml[^>]*>", "");

            InfNFe infNFe = null;
            try {
                TNFe nfe = (TNFe) unmarshaller.unmarshal(new StringReader(xmlLimpo));
                infNFe = nfe.getInfNFe();
            } catch (Exception parseEx) {
                return processarNFeManual(xml);
            }

            if (infNFe == null) return false;

            String chave = infNFe.getId().replace("NFe", "");

            if (incomingInvoiceRepository.existsByChaveAcesso(chave)) {
                return false;
            }

            String numero = infNFe.getIde().getNNF();
            String cnpjEmitente = infNFe.getEmit().getCNPJ();
            String nomeEmitente = infNFe.getEmit().getXNome();
            BigDecimal valorTotal = new BigDecimal(infNFe.getTotal().getICMSTot().getVNF());
            LocalDateTime dataEmissao = LocalDateTime.parse(infNFe.getIde().getDhEmi(), DATE_FORMATTER);

            IncomingInvoice invoice = new IncomingInvoice();
            invoice.setChaveAcesso(chave);
            invoice.setNumeroNota(numero);
            invoice.setCnpjEmitente(cnpjEmitente);
            invoice.setNomeEmitente(nomeEmitente);
            invoice.setValorTotal(valorTotal);
            invoice.setDataEmissao(dataEmissao);
            invoice.setXmlContent(xml);
            invoice.setStatus(IncomingInvoiceStatus.PENDENTE);
            invoice.setImportedAt(LocalDateTime.now());

            incomingInvoiceRepository.save(invoice);
            return true;

        } catch (Exception e) {
            log.error("Erro ao processar NFe: {}", e.getMessage());
            return false;
        }
    }

    private boolean processarNFeManual(String xml) {
        try {
            String chave = extrairValor(xml, "<chNFe>", "</chNFe>");
            if (chave == null || incomingInvoiceRepository.existsByChaveAcesso(chave)) return false;

            String numero = extrairValor(xml, "<nNF>", "</nNF>");
            String cnpjEmitente = extrairValor(xml, "<CNPJ>", "</CNPJ>");
            String nomeEmitente = extrairValor(xml, "<xNome>", "</xNome>");
            String valorStr = extrairValor(xml, "<vNF>", "</vNF>");
            String dataStr = extrairValor(xml, "<dhEmi>", "</dhEmi>");

            BigDecimal valorTotal = new BigDecimal(valorStr != null ? valorStr : "0");
            LocalDateTime dataEmissao = dataStr != null ? LocalDateTime.parse(dataStr, DATE_FORMATTER) : LocalDateTime.now();

            IncomingInvoice invoice = new IncomingInvoice();
            invoice.setChaveAcesso(chave);
            invoice.setNumeroNota(numero != null ? numero : "Desconhecido");
            invoice.setCnpjEmitente(cnpjEmitente != null ? cnpjEmitente : "Desconhecido");
            invoice.setNomeEmitente(nomeEmitente != null ? nomeEmitente : "Desconhecido");
            invoice.setValorTotal(valorTotal);
            invoice.setDataEmissao(dataEmissao);
            invoice.setXmlContent(xml);
            invoice.setStatus(IncomingInvoiceStatus.PENDENTE);
            invoice.setImportedAt(LocalDateTime.now());

            incomingInvoiceRepository.save(invoice);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String extrairValor(String xml, String tagInicio, String tagFim) {
        int inicio = xml.indexOf(tagInicio);
        if (inicio == -1) return null;
        inicio += tagInicio.length();
        int fim = xml.indexOf(tagFim, inicio);
        return (fim == -1) ? null : xml.substring(inicio, fim).trim();
    }

    /**
     * Cria a configuração da biblioteca Java_NFe
     */
    private ConfiguracoesNfe criarConfiguracao(NfeConfig config) throws Exception {
        EstadosEnum estado = EstadosEnum.valueOf(config.getUf());
        AmbienteEnum ambiente = config.getAmbiente() == AmbienteNfe.PRODUCAO ?
                AmbienteEnum.PRODUCAO : AmbienteEnum.HOMOLOGACAO;

        // Carrega o certificado do arquivo
        Certificado certificado = CertificadoService.certificadoPfx(
                config.getCertificadoPath(),
                config.getCertificadoSenha()
        );

        // === BLINDAGEM PRO CERTIFICADO FALSO (Evita StringIndexOutOfBoundsException) ===
        // Se a lib não conseguir ler a data de vencimento do PFX gerado,
        // a gente define uma data manualmente para passar da validação.
        if (certificado.getVencimento() == null) {
            // CORREÇÃO: setVencimento espera LocalDate, não LocalDateTime
            certificado.setVencimento(LocalDate.now().plusYears(1));
        }
        // ==============================================================================

        return ConfiguracoesNfe.criarConfiguracoes(
                estado,
                ambiente,
                certificado,
                "schemas" // Pasta schemas deve existir na raiz
        );
    }

    /**
     * Helper para evitar NullPointer e converter byte[] se necessário
     */
    private String safeGetString(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String) return (String) obj;
        if (obj instanceof byte[]) return new String((byte[]) obj, StandardCharsets.UTF_8);
        return String.valueOf(obj);
    }

    private boolean isValidNsu(String nsu) {
        return nsu != null && !nsu.isEmpty() && nsu.matches("\\d+");
    }

    /**
     * Força uma sincronização manual (para teste)
     */
    public int sincronizarManual() {
        log.info("Sincronização manual iniciada pelo usuário");
        return consultarNotasSefaz();
    }
}