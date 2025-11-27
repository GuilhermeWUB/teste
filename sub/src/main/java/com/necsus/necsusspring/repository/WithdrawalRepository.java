package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    List<Withdrawal> findByUserId(Long userId);

    List<Withdrawal> findByUserIdOrderByRequestDateDesc(Long userId);

    List<Withdrawal> findByStatus(String status);

    List<Withdrawal> findByUserIdAndStatus(Long userId, String status);

    Long countByUserIdAndStatus(Long userId, String status);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM Withdrawal w WHERE w.userId = ?1 AND w.status IN ('PENDENTE', 'APROVADO')")
    BigDecimal sumPendingAndApprovedByUserId(Long userId);
}
