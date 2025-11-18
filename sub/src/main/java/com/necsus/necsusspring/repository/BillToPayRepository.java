package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.BillToPay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillToPayRepository extends JpaRepository<BillToPay, Long> {

    /**
     * Busca boletos a pagar pendentes (status = 0)
     */
    @Query("SELECT b FROM BillToPay b WHERE b.status = 0 ORDER BY b.dataVencimento ASC")
    List<BillToPay> findPendingBills();

    /**
     * Busca boletos a pagar já pagos (status = 1)
     */
    @Query("SELECT b FROM BillToPay b WHERE b.status = 1 ORDER BY b.dataPagamento DESC")
    List<BillToPay> findPaidBills();

    /**
     * Conta boletos pendentes
     */
    long countByStatus(Integer status);
}
