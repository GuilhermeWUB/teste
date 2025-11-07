package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Team;
import com.necsus.necsusspring.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TeamService {

    private static final Logger logger = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /**
     * Cria um novo time
     */
    @Transactional
    public Team createTeam(Team team) {
        logger.info("Criando time: {}", team.getName());
        return teamRepository.save(team);
    }

    /**
     * Atualiza um time existente
     */
    @Transactional
    public Team updateTeam(Team team) {
        logger.info("Atualizando time: {}", team.getName());
        return teamRepository.save(team);
    }

    /**
     * Busca time por ID
     */
    public Optional<Team> findById(Long id) {
        return teamRepository.findById(id);
    }

    /**
     * Busca time por nome
     */
    public Optional<Team> findByName(String name) {
        return teamRepository.findByName(name);
    }

    /**
     * Busca time por código de role
     */
    public Optional<Team> findByRoleCode(String roleCode) {
        return teamRepository.findByRoleCode(roleCode);
    }

    /**
     * Lista todos os times
     */
    public List<Team> findAll() {
        return teamRepository.findAll();
    }

    /**
     * Lista todos os times ordenados por nome
     */
    public List<Team> findAllOrderedByName() {
        return teamRepository.findAllByOrderByNameAsc();
    }

    /**
     * Lista apenas times ativos
     */
    public List<Team> findActiveTeams() {
        return teamRepository.findByActiveTrue();
    }

    /**
     * Deleta um time
     */
    @Transactional
    public void deleteTeam(Long id) {
        logger.info("Deletando time com ID: {}", id);
        teamRepository.deleteById(id);
    }

    /**
     * Ativa ou desativa um time
     */
    @Transactional
    public Team toggleTeamStatus(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Time não encontrado com ID: " + id));
        team.setActive(!team.getActive());
        logger.info("Alterando status do time {} para: {}", team.getName(), team.getActive());
        return teamRepository.save(team);
    }

    /**
     * Verifica se existe um time com o nome especificado
     */
    public boolean existsByName(String name) {
        return teamRepository.existsByName(name);
    }

    /**
     * Verifica se existe um time com o código de role especificado
     */
    public boolean existsByRoleCode(String roleCode) {
        return teamRepository.existsByRoleCode(roleCode);
    }
}
