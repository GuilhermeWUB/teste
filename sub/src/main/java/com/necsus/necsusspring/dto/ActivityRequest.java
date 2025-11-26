package com.necsus.necsusspring.dto;

import com.necsus.necsusspring.model.ActivityPriority;
import com.necsus.necsusspring.model.ActivityType;

import java.time.LocalDateTime;

public record ActivityRequest(
    String titulo,
    String descricao,
    ActivityType tipo,
    ActivityPriority prioridade,
    Long saleId,
    String contatoNome,
    String contatoEmail,
    String contatoTelefone,
    LocalDateTime dataAgendada,
    String responsavel,
    String resultado
) {}
