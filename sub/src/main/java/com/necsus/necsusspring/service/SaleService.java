package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.SaleRequest;
import com.necsus.necsusspring.model.Sale;
import com.necsus.necsusspring.model.SaleStatus;
import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.repository.SaleRepository;
import com.necsus.necsusspring.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SaleService {

    private final SaleRepository saleRepository;
    private final UserAccountRepository userAccountRepository;

    public SaleService(SaleRepository saleRepository, UserAccountRepository userAccountRepository) {
        this.saleRepository = saleRepository;
        this.userAccountRepository = userAccountRepository;
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

        // Adiciona o valor da venda ao saldo do usuário
        if (sale.getUserId() != null && valorVenda != null) {
            UserAccount user = userAccountRepository.findById(sale.getUserId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + sale.getUserId()));

            BigDecimal valorBigDecimal = BigDecimal.valueOf(valorVenda);
            user.setSaldo(user.getSaldo().add(valorBigDecimal));
            userAccountRepository.save(user);
        }

        return saleRepository.save(sale);
    }

    /**
     * Cria e conclui uma venda de teste para o usuário especificado
     */
    public Sale createTestSaleAndComplete(Long userId, Double valorVenda) {
        // Verificar se usuário existe
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + userId));

        // Criar venda de teste
        Sale sale = new Sale();
        sale.setCooperativa("TESTE");
        sale.setTipoVeiculo("Carro");
        sale.setPlaca("TST-" + System.currentTimeMillis() % 10000);
        sale.setMarca("Fiat");
        sale.setAnoModelo("2024");
        sale.setModelo("Uno");
        sale.setNomeContato("Cliente Teste");
        sale.setEmail("teste@teste.com");
        sale.setCelular("(11) 99999-9999");
        sale.setEstado("SP");
        sale.setCidade("São Paulo");
        sale.setOrigemLead("TESTE");
        sale.setVeiculoTrabalho(true);
        sale.setEnviarCotacao(false);
        sale.setObservacoes("Venda de teste criada automaticamente");
        sale.setValorVenda(valorVenda);
        sale.setUserId(userId);
        sale.setStatus(SaleStatus.COTACOES_RECEBIDAS);

        Sale savedSale = saleRepository.save(sale);

        // Concluir a venda imediatamente
        return completeSale(savedSale.getId(), valorVenda);
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
