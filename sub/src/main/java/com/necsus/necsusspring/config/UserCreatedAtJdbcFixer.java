package com.necsus.necsusspring.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Corrige valores NULL na coluna created_at usando JDBC puro.
 * Este componente executa ANTES de qualquer tentativa de carregar UserAccount via JPA,
 * evitando erros de TimeStamp ao acessar a tela de usuários.
 *
 * Usa SQL nativo para garantir que a correção funcione mesmo quando o Hibernate
 * não consegue carregar os dados devido a valores NULL.
 */
@Component
@Order(0)  // Executa PRIMEIRO, antes de qualquer outro CommandLineRunner
public class UserCreatedAtJdbcFixer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserCreatedAtJdbcFixer.class);

    private final JdbcTemplate jdbcTemplate;

    public UserCreatedAtJdbcFixer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        LOGGER.info("========================================");
        LOGGER.info("INICIANDO CORREÇÃO DE created_at (JDBC)");
        LOGGER.info("========================================");

        try {
            // Verifica se a tabela existe
            String checkTableQuery =
                "SELECT EXISTS (" +
                "  SELECT FROM information_schema.tables " +
                "  WHERE table_schema = 'public' " +
                "  AND table_name = 'app_users'" +
                ")";

            Boolean tableExists = jdbcTemplate.queryForObject(checkTableQuery, Boolean.class);

            if (tableExists == null || !tableExists) {
                LOGGER.warn("Tabela app_users não encontrada. Pulando correção.");
                return;
            }

            // Conta registros com created_at NULL
            String countQuery = "SELECT COUNT(*) FROM app_users WHERE created_at IS NULL";
            Integer nullCount = jdbcTemplate.queryForObject(countQuery, Integer.class);

            if (nullCount == null || nullCount == 0) {
                LOGGER.info("✓ Nenhum registro com created_at NULL encontrado.");
                LOGGER.info("========================================");
                return;
            }

            LOGGER.warn("⚠️ Encontrados {} registro(s) com created_at NULL", nullCount);
            LOGGER.info("Corrigindo registros...");

            // Corrige valores NULL usando CURRENT_TIMESTAMP
            String updateQuery =
                "UPDATE app_users " +
                "SET created_at = CURRENT_TIMESTAMP " +
                "WHERE created_at IS NULL";

            int updatedRows = jdbcTemplate.update(updateQuery);

            LOGGER.info("✓ {} registro(s) corrigido(s) com sucesso!", updatedRows);

            // Verifica se ainda há NULLs
            Integer remainingNulls = jdbcTemplate.queryForObject(countQuery, Integer.class);

            if (remainingNulls != null && remainingNulls > 0) {
                LOGGER.error("⚠️ ATENÇÃO: Ainda há {} registro(s) com created_at NULL!", remainingNulls);
            } else {
                LOGGER.info("✓ Todos os registros foram corrigidos!");
            }

        } catch (Exception e) {
            LOGGER.error("========================================");
            LOGGER.error("ERRO ao corrigir created_at!");
            LOGGER.error("Tipo: {}", e.getClass().getName());
            LOGGER.error("Mensagem: {}", e.getMessage());
            LOGGER.error("========================================", e);
        }

        LOGGER.info("========================================");
        LOGGER.info("CORREÇÃO DE created_at CONCLUÍDA");
        LOGGER.info("========================================");
    }
}
