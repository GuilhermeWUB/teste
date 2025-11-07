package com.necsus.necsusspring.dto;

import com.necsus.necsusspring.model.Demand;
import com.necsus.necsusspring.model.DemandPriority;
import com.necsus.necsusspring.model.DemandStatus;
import com.necsus.necsusspring.model.RoleType;
import com.necsus.necsusspring.model.UserAccount;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DemandBoardCardDto(
        Long id,
        String titulo,
        String descricao,
        String status,
        String statusLabel,
        String prioridade,
        String prioridadeLabel,
        String prioridadeColor,
        List<String> targetRoles,
        List<String> targetRolesDisplay,
        String createdByName,
        Long createdById,
        String assignedToName,
        Long assignedToId,
        String assignedToRole,
        String dueDate,
        String createdAt,
        String updatedAt,
        String completedAt,
        String detailsUrl,
        String editUrl
) {

    public static DemandBoardCardDto from(Demand demand) {
        if (demand == null) {
            return new DemandBoardCardDto(null, null, null,
                    DemandStatus.PENDENTE.name(), DemandStatus.PENDENTE.getDisplayName(),
                    null, null, "#64748b",
                    List.of(), List.of(),
                    null, null,
                    null, null, null,
                    null, null, null, null,
                    null, null);
        }

        DemandStatus status = Optional.ofNullable(demand.getStatus()).orElse(DemandStatus.PENDENTE);
        DemandPriority prioridade = Optional.ofNullable(demand.getPrioridade()).orElse(DemandPriority.MEDIA);
        List<String> targetRoles = demand.getTargetRolesList();
        List<String> targetRolesDisplay = targetRoles.stream()
                .map(RoleType::displayNameFor)
                .toList();

        UserAccount createdBy = demand.getCreatedBy();
        UserAccount assignedTo = demand.getAssignedTo();

        return new DemandBoardCardDto(
                demand.getId(),
                demand.getTitulo(),
                demand.getDescricao(),
                status.name(),
                status.getDisplayName(),
                prioridade.name(),
                prioridade.getDisplayName(),
                demand.getPrioridadeColor(),
                List.copyOf(targetRoles),
                List.copyOf(targetRolesDisplay),
                createdBy != null ? createdBy.getFullName() : null,
                createdBy != null ? createdBy.getId() : null,
                assignedTo != null ? assignedTo.getFullName() : null,
                assignedTo != null ? assignedTo.getId() : null,
                assignedTo != null ? assignedTo.getRole() : null,
                format(demand.getDueDate()),
                format(demand.getCreatedAt()),
                format(demand.getUpdatedAt()),
                format(demand.getCompletedAt()),
                demand.getId() != null ? "/demands/" + demand.getId() : null,
                demand.getId() != null ? "/demands/" + demand.getId() + "/edit" : null
        );
    }

    private static String format(LocalDateTime value) {
        return Objects.isNull(value) ? null : value.toString();
    }
}
