package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.BankSlip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BankSlipRepository extends JpaRepository<BankSlip, Long> {

    long countByStatus(Integer status);

    /**
     * Busca todas as faturas ordenadas por vencimento (mais recentes primeiro)
     */
    @Query("SELECT b FROM BankSlip b ORDER BY b.vencimento DESC")
    Page<BankSlip> findAllOrderByVencimentoDesc(Pageable pageable);

    /**
     * Busca faturas pendentes (status = 0)
     */
    @Query("SELECT b FROM BankSlip b WHERE b.status = 0 ORDER BY b.vencimento ASC")
    List<BankSlip> findPendingInvoices();

    /**
     * Busca faturas pagas (status = 1)
     */
    @Query("SELECT b FROM BankSlip b WHERE b.status = 1 ORDER BY b.dataPagamento DESC")
    List<BankSlip> findPaidInvoices();

    /**
     * Busca faturas por BankShipment
     */
    @Query("SELECT b FROM BankSlip b WHERE b.bankShipment.id = :bankShipmentId ORDER BY b.vencimento ASC")
    List<BankSlip> findByBankShipmentId(@Param("bankShipmentId") Long bankShipmentId);
}
