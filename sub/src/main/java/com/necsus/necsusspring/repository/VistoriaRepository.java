package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Vistoria;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VistoriaRepository extends JpaRepository<Vistoria, Long> {

    // Busca todas as vistorias de um evento específico (sem fotos)
    List<Vistoria> findByEventId(Long eventId);

    // Busca todas as vistorias de um evento com fotos (usa entity graph para evitar N+1)
    @EntityGraph(value = "Vistoria.fotos", type = EntityGraph.EntityGraphType.LOAD)
    List<Vistoria> findWithFotosByEventId(Long eventId);

    // Busca vistoria por ID com fotos
    @EntityGraph(value = "Vistoria.fotos", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Vistoria> findWithFotosById(Long id);

    // Busca a vistoria mais recente de um evento
    Optional<Vistoria> findFirstByEventIdOrderByDataCriacaoDesc(Long eventId);

    // Verifica se existe vistoria para um evento
    boolean existsByEventId(Long eventId);
}
