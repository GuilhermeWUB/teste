package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.DemandBoardCardDto;
import com.necsus.necsusspring.dto.DemandBoardSnapshot;
import com.necsus.necsusspring.model.Demand;
import com.necsus.necsusspring.model.DemandStatus;
import com.necsus.necsusspring.model.NotificationType;
import com.necsus.necsusspring.model.RoleType;
import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.repository.DemandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DemandService {

    private final DemandRepository demandRepository;
    private final NotificationService notificationService;

    public DemandService(DemandRepository demandRepository, NotificationService notificationService) {
        this.demandRepository = demandRepository;
        this.notificationService = notificationService;
    }

    /**
     * Cria uma nova demanda
     */
    @Transactional
    public Demand createDemand(Demand demand) {
        return demandRepository.save(demand);
    }

    /**
     * Atualiza uma demanda existente
     */
    @Transactional
    public Demand updateDemand(Demand demand) {
        // Busca demanda anterior para comparar status
        Optional<Demand> oldDemandOpt = findById(demand.getId());
        DemandStatus oldStatus = oldDemandOpt.map(Demand::getStatus).orElse(null);

        Demand savedDemand = demandRepository.save(demand);

        // Notifica o criador se o status mudou
        if (oldStatus != null && oldStatus != savedDemand.getStatus() && savedDemand.getCreatedBy() != null) {
            notifyStatusChange(savedDemand, oldStatus, savedDemand.getStatus());
        }

        return savedDemand;
    }

    /**
     * Busca demanda por ID
     */
    public Optional<Demand> findById(Long id) {
        return demandRepository.findById(id);
    }

    /**
     * Lista todas as demandas (para admin/diretor)
     */
    public List<Demand> findAll() {
        return demandRepository.findAll();
    }

    @Transactional(readOnly = true)
    public DemandBoardSnapshot getBoardSnapshot() {
        List<Demand> demands = findAll();
        return createBoardSnapshot(demands);
    }

    @Transactional(readOnly = true)
    public DemandBoardSnapshot getBoardSnapshotByRole(String role) {
        List<Demand> demands = findByTargetRole(role);
        return createBoardSnapshot(demands);
    }

    private DemandBoardSnapshot createBoardSnapshot(List<Demand> demands) {
        List<DemandBoardCardDto> cards = demands.stream()
                .map(DemandBoardCardDto::from)
                .toList();

        LinkedHashMap<String, List<DemandBoardCardDto>> grouped = new LinkedHashMap<>();
        for (DemandStatus status : DemandStatus.values()) {
            grouped.put(status.name(), new ArrayList<>());
        }

        cards.forEach(card -> grouped
                .computeIfAbsent(card.status(), key -> new ArrayList<>())
                .add(card));

        Map<String, Integer> priorityOrder = Map.of(
                "URGENTE", 0,
                "ALTA", 1,
                "MEDIA", 2,
                "BAIXA", 3
        );

        Comparator<DemandBoardCardDto> comparator = Comparator
                .comparing(DemandBoardCardDto::dueDate, Comparator.nullsLast(String::compareTo))
                .thenComparing(card -> priorityOrder.getOrDefault(card.prioridade(), Integer.MAX_VALUE))
                .thenComparing(DemandBoardCardDto::titulo, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(DemandBoardCardDto::id, Comparator.nullsLast(Long::compareTo));

        grouped.values().forEach(list -> list.sort(comparator));

        LinkedHashMap<String, List<DemandBoardCardDto>> immutableGrouped = new LinkedHashMap<>();
        grouped.forEach((status, list) -> immutableGrouped.put(status, List.copyOf(list)));

        LinkedHashMap<String, Long> counters = new LinkedHashMap<>();
        immutableGrouped.forEach((status, list) -> counters.put(status, (long) list.size()));

        return new DemandBoardSnapshot(
                List.copyOf(cards),
                Collections.unmodifiableMap(immutableGrouped),
                Collections.unmodifiableMap(counters)
        );
    }

    /**
     * Lista demandas criadas por um usuário
     */
    public List<Demand> findByCreator(UserAccount user) {
        return demandRepository.findByCreatedByOrderByCreatedAtDesc(user);
    }

    /**
     * Lista demandas atribuídas a um usuário
     */
    public List<Demand> findByAssignedTo(UserAccount user) {
        return demandRepository.findByAssignedToOrderByCreatedAtDesc(user);
    }

    /**
     * Lista demandas por status
     */
    public List<Demand> findByStatus(DemandStatus status) {
        return demandRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * Lista demandas direcionadas a um role específico
     */
    public List<Demand> findByTargetRole(String role) {
        return demandRepository.findByTargetRolesContaining(role);
    }

    /**
     * Lista demandas acessíveis por um usuário
     * - Demandas criadas por ele
     * - Demandas atribuídas a ele
     * - Demandas direcionadas ao seu role
     */
    public List<Demand> findAccessibleByUser(UserAccount user, String userRole) {
        // Admin e Diretoria veem tudo
        if (RoleType.hasAdminPrivileges(userRole) || "DIRETORIA".equals(userRole)) {
            return findAll();
        }

        return demandRepository.findAccessibleByUser(user, userRole);
    }

    /**
     * Deleta uma demanda
     */
    @Transactional
    public void deleteDemand(Long id) {
        demandRepository.deleteById(id);
    }

    /**
     * Atualiza o status de uma demanda
     */
    @Transactional
    public Demand updateStatus(Long id, DemandStatus newStatus) {
        Optional<Demand> optionalDemand = findById(id);
        if (optionalDemand.isPresent()) {
            Demand demand = optionalDemand.get();
            DemandStatus oldStatus = demand.getStatus();
            demand.setStatus(newStatus);
            Demand savedDemand = demandRepository.save(demand);

            // Notifica o criador da demanda sobre a mudança de status
            if (oldStatus != newStatus && demand.getCreatedBy() != null) {
                notifyStatusChange(demand, oldStatus, newStatus);
            }

            return savedDemand;
        }
        throw new RuntimeException("Demanda não encontrada com ID: " + id);
    }

    /**
     * Envia notificação ao criador da demanda sobre mudança de status
     */
    private void notifyStatusChange(Demand demand, DemandStatus oldStatus, DemandStatus newStatus) {
        try {
            String title = "Status da Demanda Atualizado";
            String message = String.format(
                "A demanda \"%s\" mudou de status: %s → %s",
                demand.getTitulo(),
                getStatusLabel(oldStatus),
                getStatusLabel(newStatus)
            );

            notificationService.createNotification(
                demand.getCreatedBy(),
                title,
                message,
                NotificationType.DEMAND,
                "/demands/" + demand.getId(),
                demand.getId()
            );
        } catch (Exception e) {
            // Log do erro mas não interrompe o fluxo principal
            System.err.println("Erro ao enviar notificação de mudança de status da demanda: " + e.getMessage());
        }
    }

    /**
     * Retorna o label legível do status
     */
    private String getStatusLabel(DemandStatus status) {
        if (status == null) {
            return "Desconhecido";
        }
        return switch (status) {
            case PENDENTE -> "Pendente";
            case EM_ANDAMENTO -> "Em Andamento";
            case AGUARDANDO_RESPOSTA -> "Aguardando Resposta";
            case CONCLUIDA -> "Concluída";
            case CANCELADA -> "Cancelada";
        };
    }

    /**
     * Atribui uma demanda a um usuário
     */
    @Transactional
    public Demand assignToUser(Long demandId, UserAccount user) {
        Optional<Demand> optionalDemand = findById(demandId);
        if (optionalDemand.isPresent()) {
            Demand demand = optionalDemand.get();
            DemandStatus oldStatus = demand.getStatus();
            demand.setAssignedTo(user);
            demand.setStatus(DemandStatus.EM_ANDAMENTO);
            Demand savedDemand = demandRepository.save(demand);

            // Notifica o criador da demanda sobre a atribuição
            if (demand.getCreatedBy() != null) {
                notifyAssignment(demand, user);
            }

            // Notifica sobre mudança de status se houve mudança
            if (oldStatus != DemandStatus.EM_ANDAMENTO && demand.getCreatedBy() != null) {
                notifyStatusChange(demand, oldStatus, DemandStatus.EM_ANDAMENTO);
            }

            return savedDemand;
        }
        throw new RuntimeException("Demanda não encontrada com ID: " + demandId);
    }

    /**
     * Envia notificação ao criador da demanda sobre atribuição
     */
    private void notifyAssignment(Demand demand, UserAccount assignedUser) {
        try {
            String title = "Demanda Atribuída";
            String message = String.format(
                "A demanda \"%s\" foi atribuída a %s",
                demand.getTitulo(),
                assignedUser.getFullName() != null ? assignedUser.getFullName() : assignedUser.getUsername()
            );

            notificationService.createNotification(
                demand.getCreatedBy(),
                title,
                message,
                NotificationType.DEMAND,
                "/demands/" + demand.getId(),
                demand.getId()
            );
        } catch (Exception e) {
            // Log do erro mas não interrompe o fluxo principal
            System.err.println("Erro ao enviar notificação de atribuição da demanda: " + e.getMessage());
        }
    }
}
