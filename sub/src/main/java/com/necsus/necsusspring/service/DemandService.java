package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.DemandBoardCardDto;
import com.necsus.necsusspring.dto.DemandBoardSnapshot;
import com.necsus.necsusspring.model.Demand;
import com.necsus.necsusspring.model.DemandStatus;
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

    public DemandService(DemandRepository demandRepository) {
        this.demandRepository = demandRepository;
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
        return demandRepository.save(demand);
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
            demand.setStatus(newStatus);
            return demandRepository.save(demand);
        }
        throw new RuntimeException("Demanda não encontrada com ID: " + id);
    }

    /**
     * Atribui uma demanda a um usuário
     */
    @Transactional
    public Demand assignToUser(Long demandId, UserAccount user) {
        Optional<Demand> optionalDemand = findById(demandId);
        if (optionalDemand.isPresent()) {
            Demand demand = optionalDemand.get();
            demand.setAssignedTo(user);
            demand.setStatus(DemandStatus.EM_ANDAMENTO);
            return demandRepository.save(demand);
        }
        throw new RuntimeException("Demanda não encontrada com ID: " + demandId);
    }
}
