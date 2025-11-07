package com.necsus.necsusspring.config;

import com.necsus.necsusspring.model.RoleType;
import com.necsus.necsusspring.model.Team;
import com.necsus.necsusspring.service.TeamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Inicializa os times no sistema baseado nos roles disponíveis
 * Executa automaticamente ao iniciar a aplicação
 */
@Component
@Order(2) // Executa depois do UserDataInitializer (Order 1)
public class TeamDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TeamDataInitializer.class);

    private final TeamService teamService;

    public TeamDataInitializer(TeamService teamService) {
        this.teamService = teamService;
    }

    @Override
    public void run(String... args) {
        logger.info("=== Inicializando times do sistema ===");

        // Mapa com os times e suas descrições
        Map<String, String> teamDescriptions = new HashMap<>();
        teamDescriptions.put("ADMIN", "Time responsável pela administração geral do sistema");
        teamDescriptions.put("USER", "Time de associados da empresa");
        teamDescriptions.put("RH", "Time de RH responsável por gestão de pessoas");
        teamDescriptions.put("FINANCEIRO", "Time responsável pela gestão financeira");
        teamDescriptions.put("COMERCIAL", "Time de vendas e relacionamento comercial");
        teamDescriptions.put("MARKETING", "Time de marketing e comunicação");
        teamDescriptions.put("EVENTOS", "Time responsável pela organização de eventos");
        teamDescriptions.put("RETENCAO", "Time focado em retenção de clientes");
        teamDescriptions.put("DIRETORIA", "Time de diretoria e alta gestão");
        teamDescriptions.put("GERENTE", "Time de gerentes");
        teamDescriptions.put("GESTOR", "Time de gestores");
        teamDescriptions.put("TI", "Time de TI e desenvolvimento");
        teamDescriptions.put("CLOSERS", "Time de fechamento de vendas");
        teamDescriptions.put("EXTERNOS", "Time de membros externos");

        // Criar um time para cada role
        for (RoleType roleType : RoleType.values()) {
            String roleCode = roleType.getCode();

            // Verifica se o time já existe
            if (!teamService.existsByRoleCode(roleCode)) {
                String displayName = roleType.getDisplayName();
                String description = teamDescriptions.getOrDefault(roleCode,
                    "Time de " + displayName);

                Team team = Team.builder()
                        .name(displayName)
                        .description(description)
                        .roleCode(roleCode)
                        .active(true)
                        .build();

                teamService.createTeam(team);
                logger.info("Time criado: {} ({})", displayName, roleCode);
            } else {
                logger.debug("Time já existe para o role: {}", roleCode);
            }
        }

        logger.info("=== Inicialização de times concluída ===");
    }
}
