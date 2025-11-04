package com.necsus.necsusspring.dto;

import com.necsus.necsusspring.model.Envolvimento;
import com.necsus.necsusspring.model.Event;
import com.necsus.necsusspring.model.Motivo;
import com.necsus.necsusspring.model.Prioridade;
import com.necsus.necsusspring.model.Status;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.model.Partner;

import java.time.LocalDate;
import java.util.Optional;

public record EventBoardCardDto(
        Long id,
        String titulo,
        String descricao,
        String status,
        String statusLabel,
        String prioridade,
        String prioridadeLabel,
        String prioridadeColor,
        String motivo,
        String motivoLabel,
        String envolvimento,
        String envolvimentoLabel,
        String partnerName,
        Long partnerId,
        String vehiclePlate,
        Long vehicleId,
        LocalDate dataVencimento,
        String analistaResponsavel
) {

    public static EventBoardCardDto from(Event event) {
        if (event == null) {
            return new EventBoardCardDto(null, null, null, Status.A_FAZER.name(), Status.A_FAZER.getDisplayName(),
                    null, null, "secondary", null, null, null, null, null, null, null, null, null, null);
        }

        final Status status = Optional.ofNullable(event.getStatus()).orElse(Status.A_FAZER);
        final Prioridade prioridade = event.getPrioridade();
        final Motivo motivo = event.getMotivo();
        final Envolvimento envolvimento = event.getEnvolvimento();
        final Partner partner = event.getPartner();
        final Vehicle vehicle = event.getVehicle();

        return new EventBoardCardDto(
                event.getId(),
                event.getTitulo(),
                event.getDescricao(),
                status.name(),
                status.getDisplayName(),
                prioridade != null ? prioridade.name() : null,
                prioridade != null ? prioridade.getDisplayName() : null,
                event.getPrioridadeColor(),
                motivo != null ? motivo.name() : null,
                motivo != null ? motivo.toString() : null,
                envolvimento != null ? envolvimento.name() : null,
                envolvimento != null ? envolvimento.toString() : null,
                partner != null ? partner.getName() : null,
                partner != null ? partner.getId() : null,
                vehicle != null ? vehicle.getPlaque() : null,
                vehicle != null ? vehicle.getId() : null,
                event.getDataVencimento(),
                event.getAnalistaResponsavel()
        );
    }
}
