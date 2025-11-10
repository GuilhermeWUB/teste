package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Comunicado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComunicadoRepository extends JpaRepository<Comunicado, Long> {

    // Busca todos os comunicados ativos ordenados por data de criação (mais recentes primeiro)
    List<Comunicado> findByAtivoTrueOrderByDataCriacaoDesc();

    // Busca comunicados visíveis (ativos e não expirados)
    @Query("SELECT c FROM Comunicado c WHERE c.ativo = true AND (c.dataExpiracao IS NULL OR c.dataExpiracao > CURRENT_TIMESTAMP) ORDER BY c.dataCriacao DESC")
    List<Comunicado> findComunicadosVisiveis();

    // Busca todos os comunicados ordenados por data de criação
    List<Comunicado> findAllByOrderByDataCriacaoDesc();
}
