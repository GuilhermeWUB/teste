package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.BillToPay;
import com.necsus.necsusspring.repository.BillToPayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class BillToPayService {

    @Autowired
    private BillToPayRepository billToPayRepository;

    /**
     * Lista boletos a pagar pendentes
     */
    @Transactional(readOnly = true)
    public List<BillToPay> listPendingBills() {
        return billToPayRepository.findPendingBills();
    }

    /**
     * Lista boletos a pagar já pagos
     */
    @Transactional(readOnly = true)
    public List<BillToPay> listPaidBills() {
        return billToPayRepository.findPaidBills();
    }

    /**
     * Cria um novo boleto a pagar
     */
    @Transactional
    public BillToPay create(BillToPay billToPay) {
        billToPay.setStatus(0);
        billToPay.setDataCriacao(new Date());
        return billToPayRepository.save(billToPay);
    }

    /**
     * Busca um boleto a pagar por ID
     */
    @Transactional(readOnly = true)
    public BillToPay findById(Long id) {
        return billToPayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Boleto a pagar não encontrado"));
    }

    /**
     * Marca um boleto a pagar como pago
     */
    @Transactional
    public BillToPay markAsPaid(Long id, BigDecimal valorPago) {
        BillToPay bill = findById(id);

        if (bill.getStatus() != null && bill.getStatus() == 1) {
            throw new IllegalStateException("Este boleto já está marcado como pago");
        }

        bill.setStatus(1);
        bill.setDataPagamento(new Date());

        if (valorPago != null) {
            bill.setValorPago(valorPago);
        } else {
            bill.setValorPago(bill.getValor());
        }

        return billToPayRepository.save(bill);
    }

    /**
     * Cancela/apaga um boleto a pagar
     */
    @Transactional
    public void cancel(Long id) {
        BillToPay bill = findById(id);

        if (bill.getStatus() != null && bill.getStatus() == 1) {
            throw new IllegalStateException("Não é possível cancelar um boleto que já foi pago");
        }

        billToPayRepository.delete(bill);
    }

    /**
     * Atualiza um boleto a pagar
     */
    @Transactional
    public BillToPay update(Long id, BillToPay billToPay) {
        BillToPay existing = findById(id);

        existing.setDescricao(billToPay.getDescricao());
        existing.setValor(billToPay.getValor());
        existing.setDataVencimento(billToPay.getDataVencimento());
        existing.setFornecedor(billToPay.getFornecedor());
        existing.setCategoria(billToPay.getCategoria());
        existing.setObservacao(billToPay.getObservacao());
        existing.setNumeroDocumento(billToPay.getNumeroDocumento());
        existing.setPdfPath(billToPay.getPdfPath());

        return billToPayRepository.save(existing);
    }

    /**
     * Atualiza o caminho do PDF anexado a uma conta
     */
    @Transactional
    public BillToPay attachPdf(Long id, String pdfPath) {
        BillToPay bill = findById(id);
        bill.setPdfPath(pdfPath);
        return billToPayRepository.save(bill);
    }
}
