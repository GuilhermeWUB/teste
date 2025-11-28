
package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Informe o título")
    @Size(max = 200, message = "O título deve ter no máximo 200 caracteres")
    private String titulo;

    @NotBlank(message = "Informe a descrição")
    @Column(columnDefinition = "TEXT")
    private String descricao;

    @NotNull(message = "Informe o status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private Prioridade prioridade;

    @NotNull(message = "Informe o motivo")
    @Enumerated(EnumType.STRING)
    private Motivo motivo;

    @NotNull(message = "Informe o envolvimento")
    @Enumerated(EnumType.STRING)
    private Envolvimento envolvimento;


    private LocalDate dataAconteceu;

    private Integer horaAconteceu;

    private LocalDate dataComunicacao;

    private Integer horaComunicacao;


    private LocalDate dataVencimento;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    private Long idExterno;

    private String analistaResponsavel;

    @NotNull(message = "Informe o associado")
    @ManyToOne
    @JoinColumn(name = "partner_id")
    private Partner partner;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = true)
    private Vehicle vehicle;

    @Size(max = 10, message = "A placa deve ter no máximo 10 caracteres")
    private String placaManual;

    // Documentos anexados ao evento
    @Column(name = "doc_crlv_path")
    private String docCrlvPath;

    @Column(name = "doc_cnh_path")
    private String docCnhPath;

    @Column(name = "doc_bo_path")
    private String docBoPath;

    @Column(name = "doc_comprovante_residencia_path")
    private String docComprovanteResidenciaPath;

    @Column(name = "doc_termo_abertura_path")
    private String docTermoAberturaPath;

    // Indica se há terceiro envolvido no acidente
    @Column(name = "terceiro_envolvido")
    private Boolean terceiroEnvolvido;

    // Documentos do terceiro envolvido (se houver)
    @Column(name = "doc_terceiro_cnh_path")
    private String docTerceiroCnhPath;

    @Column(name = "doc_terceiro_crlv_path")
    private String docTerceiroCrlvPath;

    @Column(name = "doc_terceiro_outros_path")
    private String docTerceiroOutrosPath;

    // Informações pessoais do terceiro envolvido (se houver)
    @Column(name = "terceiro_nome")
    @Size(max = 200, message = "O nome do terceiro deve ter no máximo 200 caracteres")
    private String terceiroNome;

    @Column(name = "terceiro_cpf")
    @Size(max = 14, message = "O CPF deve ter no máximo 14 caracteres")
    private String terceiroCpf;

    @Column(name = "terceiro_telefone")
    @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
    private String terceiroTelefone;

    // Método auxiliar para exibir prioridade formatada
    public String getPrioridadeFormatted() {
        return prioridade != null ? prioridade.getDisplayName() : "";
    }

    // Método auxiliar para exibir status formatado
    public String getStatusFormatted() {
        return status != null ? status.getDisplayName() : "";
    }

    // Método auxiliar para obter cor da prioridade (para o frontend)
    public String getPrioridadeColor() {
        if (prioridade == null) return "secondary";
        return switch (prioridade) {
            case BAIXA -> "success";
            case MEDIA -> "info";
            case ALTA -> "warning";
            case URGENTE -> "danger";
        };
    }
}
