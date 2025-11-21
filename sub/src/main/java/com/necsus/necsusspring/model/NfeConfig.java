package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidade para armazenar as configurações de integração com a SEFAZ
 * Deve existir apenas um registro (id fixo = 1)
 */
@Entity
@Table(name = "nfe_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NfeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "O CNPJ da empresa é obrigatório")
    @Column(nullable = false, unique = true)
    private String cnpj;

    @NotEmpty(message = "O caminho do certificado é obrigatório")
    @Column(nullable = false)
    private String certificadoPath;

    @NotEmpty(message = "A senha do certificado é obrigatória")
    @Column(nullable = false)
    private String certificadoSenha;

    /**
     * Último NSU (Número Sequencial Único) consultado na SEFAZ
     * Usado para paginação nas consultas (distribuição DFe)
     */
    @Column(nullable = false)
    private String ultimoNsu = "0";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AmbienteNfe ambiente = AmbienteNfe.HOMOLOGACAO;

    /**
     * UF do emitente (usado na consulta à SEFAZ)
     */
    @Column(length = 2)
    private String uf = "SP";

    /**
     * Flag para habilitar/desabilitar a consulta automática
     */
    @Column(nullable = false)
    private Boolean ativo = true;
}
