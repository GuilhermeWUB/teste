package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Vistoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VistoriaRepository extends JpaRepository<Vistoria, Long> {

    // Busca todas as vistorias de um evento específico
    List<Vistoria> findByEventId(Long eventId);

    // Busca a vistoria mais recente de um evento
    Optional<Vistoria> findFirstByEventIdOrderByDataCriacaoDesc(Long eventId);

    // Verifica se existe vistoria para um evento
    boolean existsByEventId(Long eventId);
}
