package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.SaleRequest;
import com.necsus.necsusspring.model.Sale;
import com.necsus.necsusspring.model.SaleStatus;
import com.necsus.necsusspring.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SaleService {

    private final SaleRepository saleRepository;

    public SaleService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public List<Sale> findAll() {
        return saleRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Sale> findById(Long id) {
        return saleRepository.findById(id);
    }

    public List<Sale> findByStatus(SaleStatus status) {
        return saleRepository.findByStatus(status);
    }

    public Sale create(SaleRequest request) {
        Sale sale = new Sale();
        sale.setCooperativa(request.cooperativa());
        sale.setTipoVeiculo(request.tipoVeiculo());
        sale.setPlaca(request.placa());
        sale.setMarca(request.marca());
        sale.setAnoModelo(request.anoModelo());
        sale.setModelo(request.modelo());
        sale.setNomeContato(request.nomeContato());
        sale.setEmail(request.email());
        sale.setCelular(request.celular());
        sale.setEstado(request.estado());
        sale.setCidade(request.cidade());
        sale.setOrigemLead(request.origemLead());
        sale.setVeiculoTrabalho(request.veiculoTrabalho() != null ? request.veiculoTrabalho() : false);
        sale.setEnviarCotacao(request.enviarCotacao() != null ? request.enviarCotacao() : false);
        sale.setObservacoes(request.observacoes());
        sale.setValorVenda(request.valorVenda());
        sale.setStatus(SaleStatus.COTACOES_RECEBIDAS);

        return saleRepository.save(sale);
    }

    public Sale update(Long id, SaleRequest request) {
        Sale sale = saleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Venda nao encontrada: " + id));

        sale.setCooperativa(request.cooperativa());
        sale.setTipoVeiculo(request.tipoVeiculo());
        sale.setPlaca(request.placa());
        sale.setMarca(request.marca());
        sale.setAnoModelo(request.anoModelo());
        sale.setModelo(request.modelo());
        sale.setNomeContato(request.nomeContato());
        sale.setEmail(request.email());
        sale.setCelular(request.celular());
        sale.setEstado(request.estado());
        sale.setCidade(request.cidade());
        sale.setOrigemLead(request.origemLead());
        sale.setVeiculoTrabalho(request.veiculoTrabalho() != null ? request.veiculoTrabalho() : false);
        sale.setEnviarCotacao(request.enviarCotacao() != null ? request.enviarCotacao() : false);
        sale.setObservacoes(request.observacoes());
        sale.setValorVenda(request.valorVenda());

        return saleRepository.save(sale);
    }

    public Sale updateStatus(Long id, SaleStatus newStatus) {
        Sale sale = saleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Venda nao encontrada: " + id));

        sale.setStatus(newStatus);
        return saleRepository.save(sale);
    }

    public void delete(Long id) {
        if (!saleRepository.existsById(id)) {
            throw new RuntimeException("Venda nao encontrada: " + id);
        }
        saleRepository.deleteById(id);
    }

    public Sale completeSale(Long id, Double valorVenda) {
        Sale sale = saleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Venda nao encontrada: " + id));

        sale.setConcluida(true);
        sale.setValorVenda(valorVenda);
        sale.setDataConclusao(java.time.LocalDateTime.now());
        sale.setStatus(SaleStatus.FILIACAO_CONCRETIZADAS);

        return saleRepository.save(sale);
    }

    public List<Sale> findConcluidas() {
        return saleRepository.findByConcluida(true);
    }

    public Long countConcluidas() {
        return saleRepository.countByConcluida(true);
    }

    public Long countByStatus(SaleStatus status) {
        return saleRepository.countByStatus(status);
    }
}
