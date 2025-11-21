package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.repository.BillToPayRepository;
import com.necsus.necsusspring.repository.IncomingInvoiceRepository;
import com.necsus.necsusspring.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

/**
 * Service responsável por transformar notas da caixa de entrada em contas a pagar
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceProcessorService {

    private final IncomingInvoiceRepository incomingInvoiceRepository;
    private final PartnerRepository partnerRepository;
    private final BillToPayRepository billToPayRepository;

    /**
     * Processa uma nota da caixa de entrada e transforma em conta a pagar
     */
    @Transactional
    public BillToPay processarNota(Long incomingInvoiceId) {
        log.info("Processando nota de entrada ID: {}", incomingInvoiceId);

        IncomingInvoice invoice = incomingInvoiceRepository.findById(incomingInvoiceId)
                .orElseThrow(() -> new RuntimeException("Nota não encontrada: " + incomingInvoiceId));

        // Verifica se já foi processada
        if (invoice.getStatus() == IncomingInvoiceStatus.PROCESSADA) {
            log.warn("Nota {} já foi processada anteriormente", invoice.getChaveAcesso());
            throw new RuntimeException("Esta nota já foi processada");
        }

        // Busca ou cria o fornecedor (Partner)
        Partner fornecedor = buscarOuCriarFornecedor(invoice);

        // Cria a conta a pagar
        BillToPay conta = new BillToPay();
        conta.setDescricao("NFe " + invoice.getNumeroNota() + " - " + invoice.getNomeEmitente());
        conta.setValor(invoice.getValorTotal());
        conta.setFornecedor(fornecedor.getName());
        conta.setNumeroDocumento(invoice.getChaveAcesso());
        conta.setCategoria("Nota Fiscal Eletrônica");
        conta.setObservacao("Importado automaticamente da SEFAZ em " +
                           java.time.LocalDateTime.now().format(
                               java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        // Define data de vencimento: data de emissão + 30 dias (padrão)
        LocalDate dataVencimento = invoice.getDataEmissao().toLocalDate().plusDays(30);
        conta.setDataVencimento(Date.from(dataVencimento.atStartOfDay(ZoneId.systemDefault()).toInstant()));

        conta.setStatus(0); // Pendente

        // Salva a conta
        BillToPay contaSalva = billToPayRepository.save(conta);

        // Atualiza o status da nota para PROCESSADA
        invoice.setStatus(IncomingInvoiceStatus.PROCESSADA);
        invoice.setProcessedAt(java.time.LocalDateTime.now());
        invoice.setBillToPayId(contaSalva.getId());
        incomingInvoiceRepository.save(invoice);

        log.info("Nota {} processada com sucesso. Conta a pagar ID: {}",
                 invoice.getNumeroNota(), contaSalva.getId());

        return contaSalva;
    }

    /**
     * Busca um fornecedor pelo CNPJ ou cria um novo se não existir
     */
    private Partner buscarOuCriarFornecedor(IncomingInvoice invoice) {
        String cnpj = invoice.getCnpjEmitente();

        // Tenta buscar por CNPJ
        Optional<Partner> partnerOpt = partnerRepository.findByCnpj(cnpj);

        if (partnerOpt.isPresent()) {
            log.info("Fornecedor já existe: {}", partnerOpt.get().getName());
            return partnerOpt.get();
        }

        // Cria novo fornecedor
        log.info("Criando novo fornecedor: {}", invoice.getNomeEmitente());

        Partner novoFornecedor = new Partner();
        novoFornecedor.setName(invoice.getNomeEmitente());
        novoFornecedor.setCpf("00000000000"); // CPF placeholder (obrigatório no banco)
        novoFornecedor.setCnpj(cnpj); // CNPJ real
        novoFornecedor.setEmail(cnpj + "@fornecedor.auto");
        novoFornecedor.setPhone("Não informado");
        novoFornecedor.setCell("Não informado");
        novoFornecedor.setStatus(PartnerStatus.ATIVO);

        // Salva o fornecedor
        Partner fornecedorSalvo = partnerRepository.save(novoFornecedor);

        log.info("Fornecedor criado com ID: {}", fornecedorSalvo.getId());

        return fornecedorSalvo;
    }

    /**
     * Marca uma nota como ignorada (não será processada)
     */
    @Transactional
    public void ignorarNota(Long incomingInvoiceId, String motivo) {
        log.info("Marcando nota {} como ignorada", incomingInvoiceId);

        IncomingInvoice invoice = incomingInvoiceRepository.findById(incomingInvoiceId)
                .orElseThrow(() -> new RuntimeException("Nota não encontrada: " + incomingInvoiceId));

        invoice.setStatus(IncomingInvoiceStatus.IGNORADA);
        invoice.setObservacoes("Ignorada: " + motivo);

        incomingInvoiceRepository.save(invoice);

        log.info("Nota {} marcada como ignorada", invoice.getNumeroNota());
    }

    /**
     * Reprocessa uma nota (volta para PENDENTE)
     */
    @Transactional
    public void reprocessarNota(Long incomingInvoiceId) {
        log.info("Reprocessando nota {}", incomingInvoiceId);

        IncomingInvoice invoice = incomingInvoiceRepository.findById(incomingInvoiceId)
                .orElseThrow(() -> new RuntimeException("Nota não encontrada: " + incomingInvoiceId));

        invoice.setStatus(IncomingInvoiceStatus.PENDENTE);
        invoice.setProcessedAt(null);
        invoice.setBillToPayId(null);

        incomingInvoiceRepository.save(invoice);

        log.info("Nota {} marcada como pendente novamente", invoice.getNumeroNota());
    }

    /**
     * Processa múltiplas notas em lote
     */
    @Transactional
    public int processarNotasPendentes() {
        log.info("Processando todas as notas pendentes em lote...");

        var notasPendentes = incomingInvoiceRepository.findByStatusOrderByImportedAtDesc(
                IncomingInvoiceStatus.PENDENTE);

        int processadas = 0;

        for (IncomingInvoice nota : notasPendentes) {
            try {
                processarNota(nota.getId());
                processadas++;
            } catch (Exception e) {
                log.error("Erro ao processar nota {}: {}", nota.getId(), e.getMessage());
                // Continua processando as outras
            }
        }

        log.info("Total de notas processadas: {}/{}", processadas, notasPendentes.size());

        return processadas;
    }
}
