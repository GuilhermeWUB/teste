package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.NfeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository para gerenciar as configurações da NFe
 */
@Repository
public interface NfeConfigRepository extends JpaRepository<NfeConfig, Long> {

    /**
     * Busca a configuração pelo CNPJ da empresa
     */
    Optional<NfeConfig> findByCnpj(String cnpj);

    /**
     * Busca a configuração ativa
     */
    Optional<NfeConfig> findFirstByAtivoTrue();
}
