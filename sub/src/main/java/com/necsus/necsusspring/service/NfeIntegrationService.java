package com.necsus.necsusspring.service;

import br.com.swconsultoria.nfe.Nfe;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.dom.enuns.AmbienteEnum;
import br.com.swconsultoria.nfe.dom.enuns.EstadosEnum;
import br.com.swconsultoria.nfe.dom.enuns.PessoaEnum;
import br.com.swconsultoria.nfe.schema.distdfeint.RetDistDFeInt;
import br.com.swconsultoria.nfe.schema.distdfeint.RetDistDFeInt.LoteDistDFeInt.DocZip;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TEnviNFe;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe;
import br.com.swconsultoria.nfe.util.ConstantesUtil;
import br.com.swconsultoria.nfe.util.XmlUtil;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
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
            return 0;
        }
    }

    /**
     * Consulta com paginação (recursiva) para pegar todas as notas disponíveis
     */
    private int consultarComPaginacao(NfeConfig config) throws Exception {
        int totalImportadas = 0;
        String nsuAtual = config.getUltimoNsu();

        log.info("Consultando a partir do NSU: {}", nsuAtual);

        ConfiguracoesNfe configuracao = criarConfiguracao(config);

        RetDistDFeInt retorno = Nfe.distribuicaoDfe(
                configuracao,
                config.getCnpj(),
                nsuAtual,
                PessoaEnum.JURIDICA
        );

        if (retorno == null) {
            log.warn("Retorno da SEFAZ foi nulo");
            return 0;
        }

        log.info("Status SEFAZ: {} - {}", retorno.getCStat(), retorno.getXMotivo());

        // 138 = Documento localizado
        if ("138".equals(retorno.getCStat())) {
            if (retorno.getLoteDistDFeInt() != null && retorno.getLoteDistDFeInt().getDocZip() != null) {

                int importadas = processarDocumentosZipados(retorno.getLoteDistDFeInt().getDocZip());
                totalImportadas += importadas;

                // Atualiza o NSU para o próximo
                String maxNSU = retorno.getUltNSU();
                String ultNSU = retorno.getLoteDistDFeInt().getUltNSU();

                log.info("maxNSU: {}, ultNSU: {}", maxNSU, ultNSU);

                // Salva o último NSU processado
                nfeConfigService.atualizarUltimoNsu(config.getId(), ultNSU);

                // Se ainda há mais documentos (ultNSU < maxNSU), busca recursivamente
                if (ultNSU != null && maxNSU != null &&
                    Long.parseLong(ultNSU) < Long.parseLong(maxNSU)) {

                    log.info("Ainda há mais documentos. Consultando próxima página...");

                    // Recarrega a config com o NSU atualizado
                    NfeConfig configAtualizada = nfeConfigService.getConfigById(config.getId()).orElse(config);
                    totalImportadas += consultarComPaginacao(configAtualizada);
                }
            }
        } else if ("137".equals(retorno.getCStat())) {
            // 137 = Nenhum documento localizado
            log.info("Nenhum documento novo encontrado.");
        } else {
            log.warn("Status inesperado da SEFAZ: {} - {}", retorno.getCStat(), retorno.getXMotivo());
        }

        return totalImportadas;
    }

    /**
     * Processa os documentos zipados retornados pela SEFAZ
     */
    private int processarDocumentosZipados(java.util.List<DocZip> documentos) {
        int importadas = 0;

        for (DocZip doc : documentos) {
            try {
                String xmlDescompactado = descompactarGzip(doc.getValue());

                // Processa apenas NFe (schema resNFe ou procNFe)
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

        log.info("Total de notas importadas nesta leva: {}", importadas);
        return importadas;
    }

    /**
     * Descompacta o XML que vem em GZIP + Base64
     */
    private String descompactarGzip(byte[] dados) throws Exception {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(dados))) {
            return new String(gzip.readAllBytes(), "UTF-8");
        }
    }

    /**
     * Processa uma NFe individual e salva na caixa de entrada
     */
    private boolean processarNFe(String xml) {
        try {
            // Parse do XML para extrair informações
            JAXBContext context = JAXBContext.newInstance(TNFe.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            // Remove namespaces e processa
            String xmlLimpo = xml.replaceAll("xmlns=\"[^\"]*\"", "")
                                 .replaceAll("<\\?xml[^>]*>", "");

            // Tenta fazer o parse - se falhar, tenta extrair manualmente
            InfNFe infNFe = null;
            try {
                TNFe nfe = (TNFe) unmarshaller.unmarshal(new StringReader(xmlLimpo));
                infNFe = nfe.getInfNFe();
            } catch (Exception parseEx) {
                log.warn("Falha no parse automático. Tentando extração manual...");
                return processarNFeManual(xml);
            }

            if (infNFe == null) {
                log.warn("Não foi possível extrair InfNFe");
                return false;
            }

            String chave = infNFe.getId().replace("NFe", "");

            // Verifica se já existe
            if (incomingInvoiceRepository.existsByChaveAcesso(chave)) {
                log.debug("Nota {} já existe no banco. Ignorando.", chave);
                return false;
            }

            // Extrai dados
            String numero = infNFe.getIde().getNNF();
            String cnpjEmitente = infNFe.getEmit().getCNPJ();
            String nomeEmitente = infNFe.getEmit().getXNome();
            BigDecimal valorTotal = new BigDecimal(infNFe.getTotal().getICMSTot().getVNF());
            LocalDateTime dataEmissao = LocalDateTime.parse(infNFe.getIde().getDhEmi(), DATE_FORMATTER);

            // Cria e salva a nota
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

            log.info("Nota importada com sucesso: {} - {} - R$ {}",
                     numero, nomeEmitente, valorTotal);

            return true;

        } catch (Exception e) {
            log.error("Erro ao processar NFe: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extração manual usando regex/substring quando o JAXB falha
     */
    private boolean processarNFeManual(String xml) {
        try {
            String chave = extrairValor(xml, "<chNFe>", "</chNFe>");

            if (chave == null || chave.isEmpty()) {
                log.warn("Chave de acesso não encontrada no XML");
                return false;
            }

            if (incomingInvoiceRepository.existsByChaveAcesso(chave)) {
                log.debug("Nota {} já existe (extração manual). Ignorando.", chave);
                return false;
            }

            String numero = extrairValor(xml, "<nNF>", "</nNF>");
            String cnpjEmitente = extrairValor(xml, "<CNPJ>", "</CNPJ>");
            String nomeEmitente = extrairValor(xml, "<xNome>", "</xNome>");
            String valorStr = extrairValor(xml, "<vNF>", "</vNF>");
            String dataStr = extrairValor(xml, "<dhEmi>", "</dhEmi>");

            BigDecimal valorTotal = new BigDecimal(valorStr != null ? valorStr : "0");
            LocalDateTime dataEmissao = dataStr != null ?
                LocalDateTime.parse(dataStr, DATE_FORMATTER) : LocalDateTime.now();

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

            log.info("Nota importada com sucesso (extração manual): {}", chave);
            return true;

        } catch (Exception e) {
            log.error("Erro na extração manual: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Helper para extrair valores do XML
     */
    private String extrairValor(String xml, String tagInicio, String tagFim) {
        int inicio = xml.indexOf(tagInicio);
        if (inicio == -1) return null;

        inicio += tagInicio.length();
        int fim = xml.indexOf(tagFim, inicio);

        if (fim == -1) return null;

        return xml.substring(inicio, fim).trim();
    }

    /**
     * Cria a configuração da biblioteca Java_NFe
     */
    private ConfiguracoesNfe criarConfiguracao(NfeConfig config) throws Exception {
        EstadosEnum estado = EstadosEnum.valueOf(config.getUf());
        AmbienteEnum ambiente = config.getAmbiente() == AmbienteNfe.PRODUCAO ?
                AmbienteEnum.PRODUCAO : AmbienteEnum.HOMOLOGACAO;

        ConfiguracoesNfe configuracao = ConfiguracoesNfe.criarConfiguracoes(
                estado,
                ambiente,
                config.getCertificadoPath(),
                config.getCertificadoSenha()
        );

        log.debug("Configuração NFe criada: Estado={}, Ambiente={}", estado, ambiente);

        return configuracao;
    }

    /**
     * Força uma sincronização manual (para teste)
     */
    public int sincronizarManual() {
        log.info("Sincronização manual iniciada pelo usuário");
        return consultarNotasSefaz();
    }
}
