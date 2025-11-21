package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.IncomingInvoice;
import com.necsus.necsusspring.model.IncomingInvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para gerenciar as notas fiscais de entrada
 */
@Repository
public interface IncomingInvoiceRepository extends JpaRepository<IncomingInvoice, Long> {

    /**
     * Verifica se já existe uma nota com a chave de acesso informada
     */
    boolean existsByChaveAcesso(String chaveAcesso);

    /**
     * Busca nota pela chave de acesso
     */
    Optional<IncomingInvoice> findByChaveAcesso(String chaveAcesso);

    /**
     * Lista notas por status
     */
    List<IncomingInvoice> findByStatusOrderByImportedAtDesc(IncomingInvoiceStatus status);

    /**
     * Lista notas pendentes (paginado)
     */
    Page<IncomingInvoice> findByStatusOrderByImportedAtDesc(IncomingInvoiceStatus status, Pageable pageable);

    /**
     * Conta notas por status
     */
    long countByStatus(IncomingInvoiceStatus status);

    /**
     * Busca notas por CNPJ do emitente
     */
    List<IncomingInvoice> findByCnpjEmitenteOrderByDataEmissaoDesc(String cnpjEmitente);

    /**
     * Busca todas as notas pendentes
     */
    @Query("SELECT i FROM IncomingInvoice i WHERE i.status = 'PENDENTE' ORDER BY i.dataEmissao DESC")
    List<IncomingInvoice> findAllPendentes();

    /**
     * Busca notas com filtros opcionais
     */
    @Query("SELECT i FROM IncomingInvoice i WHERE " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:cnpjEmitente IS NULL OR i.cnpjEmitente = :cnpjEmitente) " +
           "ORDER BY i.importedAt DESC")
    Page<IncomingInvoice> findWithFilters(
        @Param("status") IncomingInvoiceStatus status,
        @Param("cnpjEmitente") String cnpjEmitente,
        Pageable pageable
    );
}
