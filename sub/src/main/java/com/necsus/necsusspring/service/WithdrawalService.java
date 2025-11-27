package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.model.Withdrawal;
import com.necsus.necsusspring.model.WithdrawalStatus;
import com.necsus.necsusspring.repository.UserAccountRepository;
import com.necsus.necsusspring.repository.WithdrawalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final UserAccountRepository userAccountRepository;

    public WithdrawalService(WithdrawalRepository withdrawalRepository, UserAccountRepository userAccountRepository) {
        this.withdrawalRepository = withdrawalRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Calcula o saldo disponível para saque (saldo - saques pendentes/aprovados)
     */
    public BigDecimal getAvailableBalance(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + userId));

        BigDecimal pendingWithdrawals = withdrawalRepository.sumPendingAndApprovedByUserId(userId);

        return user.getSaldo().subtract(pendingWithdrawals);
    }

    /**
     * Cria uma nova solicitação de saque
     */
    public Withdrawal createWithdrawalRequest(Long userId, BigDecimal amount, String pixKey) {
        // Validar usuário
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + userId));

        // Validar valor
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Valor do saque deve ser maior que zero");
        }

        // Validar saldo disponível
        BigDecimal availableBalance = getAvailableBalance(userId);
        if (amount.compareTo(availableBalance) > 0) {
            throw new RuntimeException("Saldo insuficiente. Disponível: R$ " + availableBalance);
        }

        // Criar solicitação de saque
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setUserId(userId);
        withdrawal.setAmount(amount);
        withdrawal.setPixKey(pixKey);
        withdrawal.setStatus(WithdrawalStatus.PENDENTE.name());
        withdrawal.setRequestDate(LocalDateTime.now());

        return withdrawalRepository.save(withdrawal);
    }

    /**
     * Aprova uma solicitação de saque
     */
    public Withdrawal approveWithdrawal(Long withdrawalId, String observation) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Saque não encontrado: " + withdrawalId));

        if (!WithdrawalStatus.PENDENTE.name().equals(withdrawal.getStatus())) {
            throw new RuntimeException("Apenas saques pendentes podem ser aprovados");
        }

        withdrawal.setStatus(WithdrawalStatus.APROVADO.name());
        if (observation != null) {
            withdrawal.setObservation(observation);
        }

        return withdrawalRepository.save(withdrawal);
    }

    /**
     * Rejeita uma solicitação de saque
     */
    public Withdrawal rejectWithdrawal(Long withdrawalId, String observation) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Saque não encontrado: " + withdrawalId));

        if (!WithdrawalStatus.PENDENTE.name().equals(withdrawal.getStatus())) {
            throw new RuntimeException("Apenas saques pendentes podem ser rejeitados");
        }

        withdrawal.setStatus(WithdrawalStatus.REJEITADO.name());
        withdrawal.setObservation(observation);
        withdrawal.setCompletedDate(LocalDateTime.now());

        return withdrawalRepository.save(withdrawal);
    }

    /**
     * Conclui um saque aprovado (transferência realizada)
     */
    public Withdrawal completeWithdrawal(Long withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Saque não encontrado: " + withdrawalId));

        if (!WithdrawalStatus.APROVADO.name().equals(withdrawal.getStatus())) {
            throw new RuntimeException("Apenas saques aprovados podem ser concluídos");
        }

        // Debitar do saldo do usuário
        UserAccount user = userAccountRepository.findById(withdrawal.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + withdrawal.getUserId()));

        if (withdrawal.getAmount().compareTo(user.getSaldo()) > 0) {
            throw new RuntimeException("Saldo insuficiente para concluir o saque");
        }

        user.setSaldo(user.getSaldo().subtract(withdrawal.getAmount()));
        userAccountRepository.save(user);

        withdrawal.setStatus(WithdrawalStatus.CONCLUIDO.name());
        withdrawal.setCompletedDate(LocalDateTime.now());

        return withdrawalRepository.save(withdrawal);
    }

    /**
     * Lista todos os saques de um usuário
     */
    public List<Withdrawal> findByUserId(Long userId) {
        return withdrawalRepository.findByUserIdOrderByRequestDateDesc(userId);
    }

    /**
     * Lista todos os saques
     */
    public List<Withdrawal> findAll() {
        return withdrawalRepository.findAll();
    }

    /**
     * Busca um saque por ID
     */
    public Optional<Withdrawal> findById(Long id) {
        return withdrawalRepository.findById(id);
    }

    /**
     * Lista saques por status
     */
    public List<Withdrawal> findByStatus(String status) {
        return withdrawalRepository.findByStatus(status);
    }

    /**
     * Cria um saque de teste (para desenvolvimento)
     */
    public Withdrawal createTestWithdrawal(Long userId, BigDecimal amount) {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setUserId(userId);
        withdrawal.setAmount(amount);
        withdrawal.setPixKey("teste@teste.com");
        withdrawal.setStatus(WithdrawalStatus.PENDENTE.name());
        withdrawal.setRequestDate(LocalDateTime.now());
        withdrawal.setObservation("Saque de teste");

        return withdrawalRepository.save(withdrawal);
    }

    /**
     * Conta saques pendentes de um usuário
     */
    public Long countPendingByUserId(Long userId) {
        return withdrawalRepository.countByUserIdAndStatus(userId, WithdrawalStatus.PENDENTE.name());
    }
}
