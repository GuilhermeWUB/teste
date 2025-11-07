package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    /**
     * Busca um time pelo nome
     */
    Optional<Team> findByName(String name);

    /**
     * Busca um time pelo código do role associado
     */
    Optional<Team> findByRoleCode(String roleCode);

    /**
     * Busca todos os times ativos
     */
    List<Team> findByActiveTrue();

    /**
     * Busca todos os times ordenados por nome
     */
    List<Team> findAllByOrderByNameAsc();

    /**
     * Verifica se existe um time com o nome especificado
     */
    boolean existsByName(String name);

    /**
     * Verifica se existe um time com o código de role especificado
     */
    boolean existsByRoleCode(String roleCode);
}
