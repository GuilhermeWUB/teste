package com.necsus.necsusspring.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller temporário para corrigir o banco de dados
 * IMPORTANTE: Remover após execução
 */
@RestController
@RequestMapping("/api/database-fix")
public class DatabaseFixController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/fix-vehicle-nullable")
    public ResponseEntity<?> fixVehicleNullable() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Remove constraint NOT NULL da coluna vehicle_id
            jdbcTemplate.execute("ALTER TABLE event ALTER COLUMN vehicle_id DROP NOT NULL");
            result.put("step1", "✅ Constraint NOT NULL removida da coluna vehicle_id");

            // 2. Adiciona coluna placa_manual se não existir
            String checkColumnSql =
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name='event' AND column_name='placa_manual'";

            Integer columnExists = jdbcTemplate.queryForObject(checkColumnSql, Integer.class);

            if (columnExists == 0) {
                jdbcTemplate.execute("ALTER TABLE event ADD COLUMN placa_manual VARCHAR(10)");
                result.put("step2", "✅ Coluna placa_manual adicionada");
            } else {
                result.put("step2", "ℹ️ Coluna placa_manual já existe");
            }

            result.put("success", true);
            result.put("message", "Banco de dados corrigido com sucesso!");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "Erro ao corrigir banco de dados");
            return ResponseEntity.status(500).body(result);
        }
    }
}
