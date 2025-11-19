package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.AgreementRequest;
import com.necsus.necsusspring.model.Agreement;
import com.necsus.necsusspring.model.AgreementStatus;
import com.necsus.necsusspring.repository.AgreementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AgreementService {

    private final AgreementRepository agreementRepository;

    public AgreementService(AgreementRepository agreementRepository) {
        this.agreementRepository = agreementRepository;
    }

    public List<Agreement> findAll() {
        return agreementRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Agreement> findById(Long id) {
        return agreementRepository.findById(id);
    }

    public List<Agreement> findByStatus(AgreementStatus status) {
        return agreementRepository.findByStatus(status);
    }

    public Agreement create(AgreementRequest request) {
        Agreement agreement = new Agreement();
        agreement.setTitulo(request.titulo());
        agreement.setDescricao(request.descricao());
        agreement.setParteEnvolvida(request.parteEnvolvida());
        agreement.setValor(request.valor());
        agreement.setDataVencimento(request.dataVencimento());
        agreement.setDataPagamento(request.dataPagamento());
        agreement.setObservacoes(request.observacoes());
        agreement.setNumeroParcelas(request.numeroParcelas());
        agreement.setParcelaAtual(request.parcelaAtual());
        agreement.setNumeroProcesso(request.numeroProcesso());
        agreement.setStatus(AgreementStatus.PENDENTE);

        return agreementRepository.save(agreement);
    }

    public Agreement update(Long id, AgreementRequest request) {
        Agreement agreement = agreementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Acordo nao encontrado: " + id));

        agreement.setTitulo(request.titulo());
        agreement.setDescricao(request.descricao());
        agreement.setParteEnvolvida(request.parteEnvolvida());
        agreement.setValor(request.valor());
        agreement.setDataVencimento(request.dataVencimento());
        agreement.setDataPagamento(request.dataPagamento());
        agreement.setObservacoes(request.observacoes());
        agreement.setNumeroParcelas(request.numeroParcelas());
        agreement.setParcelaAtual(request.parcelaAtual());
        agreement.setNumeroProcesso(request.numeroProcesso());

        return agreementRepository.save(agreement);
    }

    public Agreement updateStatus(Long id, AgreementStatus newStatus) {
        Agreement agreement = agreementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Acordo nao encontrado: " + id));

        agreement.setStatus(newStatus);
        return agreementRepository.save(agreement);
    }

    public void delete(Long id) {
        if (!agreementRepository.existsById(id)) {
            throw new RuntimeException("Acordo nao encontrado: " + id);
        }
        agreementRepository.deleteById(id);
    }
}
