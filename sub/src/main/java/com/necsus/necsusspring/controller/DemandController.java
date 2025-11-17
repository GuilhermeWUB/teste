package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.DemandBoardCardDto;
import com.necsus.necsusspring.dto.DemandBoardSnapshot;
import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.service.DemandService;
import com.necsus.necsusspring.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller para gerenciar demandas
 * - Diretores e Admins podem criar e gerenciar demandas
 * - Usuários comuns podem visualizar e atualizar status das suas demandas
 */
@Controller
@RequestMapping("/demands")
public class DemandController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DemandController.class);

    private final DemandService demandService;
    private final UserAccountService userAccountService;

    public DemandController(DemandService demandService, UserAccountService userAccountService) {
        this.demandService = demandService;
        this.userAccountService = userAccountService;
    }

    /**
     * Página principal de demandas - redireciona baseado no role
     */
    @GetMapping
    public String index(Authentication authentication) {
        String userRole = getUserRole(authentication);

        // Bloqueia acesso de Associados (USER)
        if ("USER".equals(userRole)) {
            logger.warn("Acesso negado às demandas. Role USER/Associado não tem acesso. Role: {}", userRole);
            return "redirect:/";
        }

        // Usuários que podem criar demandas (ADMIN, DIRETORIA, GERENTE, GESTOR) vão para painel de gestão
        if (RoleType.canCreateDemands(userRole)) {
            return "redirect:/demands/director";
        }

        // Colaboradores vão para suas demandas (apenas visualização)
        return "redirect:/demands/my-demands";
    }

    /**
     * Painel do Diretor - Criar e gerenciar demandas
     * Acessível apenas para usuários que podem criar demandas (ADMIN, DIRETORIA, GERENTE, GESTOR)
     */
    @GetMapping("/director")
    public String showDirectorPanel(Authentication authentication, Model model) {
        String userRole = getUserRole(authentication);

        if (!RoleType.canCreateDemands(userRole)) {
            logger.warn("Acesso negado ao painel de criação de demandas. Role {} não pode criar demandas.", userRole);
            return "redirect:/demands/my-demands";
        }

        UserAccount currentUser = getCurrentUser(authentication);

        // ADMIN e DIRETORIA veem todas as demandas, outros roles veem apenas as direcionadas a eles
        List<Demand> roleDemands = isDirectorOrAdmin(userRole)
            ? demandService.findAll()
            : demandService.findByTargetRole(userRole);

        // Separa por status para estatísticas
        long pendentes = roleDemands.stream().filter(d -> d.getStatus() == DemandStatus.PENDENTE).count();
        long emAndamento = roleDemands.stream().filter(d -> d.getStatus() == DemandStatus.EM_ANDAMENTO).count();
        long concluidas = roleDemands.stream().filter(d -> d.getStatus() == DemandStatus.CONCLUIDA).count();
        long canceladas = roleDemands.stream().filter(d -> d.getStatus() == DemandStatus.CANCELADA).count();

        model.addAttribute("demands", roleDemands);
        model.addAttribute("pendentes", pendentes);
        model.addAttribute("emAndamento", emAndamento);
        model.addAttribute("concluidas", concluidas);
        model.addAttribute("canceladas", canceladas);
        model.addAttribute("totalDemands", roleDemands.size());
        model.addAttribute("newDemand", new Demand());
        model.addAttribute("availableRoles", getAvailableRolesForUser(userRole));
        model.addAttribute("statusOptions", DemandStatus.values());
        model.addAttribute("priorityOptions", DemandPriority.values());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("userRole", userRole);

        return "demandas_diretor";
    }

    @GetMapping("/api/board")
    @ResponseBody
    public ResponseEntity<?> getBoardSnapshot(Authentication authentication) {
        String userRole = getUserRole(authentication);

        if (!RoleType.canCreateDemands(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Acesso negado"));
        }

        // ADMIN e DIRETORIA veem todas as demandas, outros roles veem apenas as direcionadas a eles
        DemandBoardSnapshot snapshot = isDirectorOrAdmin(userRole)
            ? demandService.getBoardSnapshot()
            : demandService.getBoardSnapshotByRole(userRole);
        return ResponseEntity.ok(snapshot);
    }

    @PutMapping("/api/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateDemandStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Authentication authentication) {

        String userRole = getUserRole(authentication);
        if (!RoleType.canCreateDemands(userRole)) {
            logger.warn("Tentativa de atualizar status da demanda {} sem permissão. Role: {}", id, userRole);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Você não tem permissão para atualizar demandas."));
        }

        try {
            String statusValue = payload.get("status");
            if (statusValue == null || statusValue.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Status é obrigatório"));
            }

            DemandStatus newStatus = DemandStatus.valueOf(statusValue);
            String observation = payload.getOrDefault("observation", null);

            UserAccount currentUser = getCurrentUser(authentication);
            Demand demand = demandService.findById(id)
                    .orElse(null);

            if (demand == null) {
                logger.warn("Demanda {} não encontrada para atualização de status via API", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Demanda não encontrada"));
            }

            if (newStatus == DemandStatus.CANCELADA && !isDemandCreator(demand, currentUser)) {
                logger.warn("Usuário {} tentou cancelar a demanda {} mas não é o criador", currentUser.getUsername(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Somente quem criou a demanda pode cancelá-la."));
            }

            Demand updated = demandService.updateStatus(id, newStatus, observation);

            logger.info("Status da demanda {} atualizado para {}", id, newStatus);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Status atualizado com sucesso",
                    "demand", DemandBoardCardDto.from(updated)
            ));
        } catch (IllegalArgumentException e) {
            logger.error("Status inválido informado para demanda {}: {}", id, payload.get("status"));
            return ResponseEntity.badRequest().body(Map.of("error", "Status inválido"));
        } catch (Exception e) {
            logger.error("Erro ao atualizar status da demanda {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao atualizar status da demanda"));
        }
    }

    /**
     * API para retornar os roles disponíveis baseado na hierarquia do usuário logado
     */
    @GetMapping("/api/available-roles")
    @ResponseBody
    public ResponseEntity<?> getAvailableRoles(Authentication authentication) {
        String userRole = getUserRole(authentication);

        if (!RoleType.canCreateDemands(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Acesso negado"));
        }

        List<RoleType> availableRoles = RoleType.getTargetRolesFor(userRole);

        List<Map<String, String>> rolesDTO = availableRoles.stream()
                .map(role -> Map.of(
                        "code", role.getCode(),
                        "displayName", role.getDisplayName()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(rolesDTO);
    }

    /**
     * API para buscar usuários por role(s)
     * Retorna lista de usuários que possuem as roles selecionadas
     */
    @GetMapping("/api/users-by-roles")
    @ResponseBody
    public ResponseEntity<?> getUsersByRoles(
            @RequestParam(value = "roles", required = false) List<String> roles,
            Authentication authentication) {

        String userRole = getUserRole(authentication);
        if (!RoleType.canCreateDemands(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Acesso negado"));
        }

        try {
            if (roles == null || roles.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }

            // Busca todos os usuários que possuem alguma das roles selecionadas
            List<UserAccount> users = userAccountService.findByRoles(roles);

            // Converte para DTO simplificado
            List<Map<String, Object>> usersDTO = users.stream()
                    .map(user -> Map.of(
                            "id", (Object) user.getId(),
                            "fullName", user.getFullName(),
                            "username", user.getUsername(),
                            "email", user.getEmail(),
                            "role", user.getRole()
                    ))
                    .collect(Collectors.toList());

            logger.info("Retornando {} usuários para roles: {}", usersDTO.size(), roles);
            return ResponseEntity.ok(usersDTO);

        } catch (Exception e) {
            logger.error("Erro ao buscar usuários por roles: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao buscar usuários"));
        }
    }

    /**
     * Criar nova demanda
     */
    @PostMapping("/create")
    public String createDemand(
            @ModelAttribute("newDemand") Demand demand,
            @RequestParam(value = "targetRolesList", required = false) List<String> targetRolesList,
            @RequestParam(value = "assignedToId", required = false) Long assignedToId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String userRole = getUserRole(authentication);

        if (!RoleType.canCreateDemands(userRole)) {
            logger.warn("Tentativa de criar demanda sem permissão. Role: {}", userRole);
            redirectAttributes.addFlashAttribute("error", "Você não tem permissão para criar demandas.");
            return "redirect:/demands/my-demands";
        }

        // Verifica se os roles de destino são válidos para a hierarquia do usuário
        List<String> allowedRoles = RoleType.getTargetRoleCodesFor(userRole);
        if (targetRolesList != null && !allowedRoles.containsAll(targetRolesList)) {
            logger.warn("Tentativa de criar demanda para roles não permitidos. User role: {}, Target roles: {}", userRole, targetRolesList);
            redirectAttributes.addFlashAttribute("error", "Você não tem permissão para enviar demandas para alguns dos cargos selecionados.");
            return "redirect:/demands/director";
        }

        try {
            UserAccount currentUser = getCurrentUser(authentication);
            demand.setCreatedBy(currentUser);

            // Define os cargos destinatários
            if (targetRolesList != null && !targetRolesList.isEmpty()) {
                demand.setTargetRolesList(targetRolesList);
            }

            // Atribui a um funcionário específico se fornecido
            if (assignedToId != null) {
                UserAccount assignedUser = userAccountService.findById(assignedToId)
                        .orElse(null);

                if (assignedUser != null) {
                    demand.setAssignedTo(assignedUser);
                    logger.info("Demanda atribuída ao usuário: {} (ID: {})", assignedUser.getFullName(), assignedUser.getId());
                } else {
                    logger.warn("Usuário com ID {} não encontrado para atribuição", assignedToId);
                }
            }

            demandService.createDemand(demand);

            logger.info("Demanda criada com sucesso: {} por {}", demand.getTitulo(), currentUser.getUsername());
            redirectAttributes.addFlashAttribute("success", "Demanda criada com sucesso!");

        } catch (Exception e) {
            logger.error("Erro ao criar demanda", e);
            redirectAttributes.addFlashAttribute("error", "Erro ao criar demanda: " + e.getMessage());
        }

        return "redirect:/demands/director";
    }

    /**
     * Minhas Demandas - Visualização para usuários (exceto DIRETORIA e USER/Associado)
     * Acessível para: ADMIN, GERENTE, GESTOR e todos os COLABORADORES
     */
    @GetMapping("/my-demands")
    public String showMyDemands(
            Authentication authentication,
            @RequestParam(value = "status", required = false) DemandStatus statusFilter,
            Model model) {

        String userRole = getUserRole(authentication);

        // Bloqueia acesso de DIRETORIA e USER (Associado)
        if (!RoleType.canAccessMyDemands(userRole)) {
            logger.warn("Acesso negado a Minhas Demandas. Role {} não tem permissão.", userRole);
            // DIRETORIA redireciona para painel de diretor, outros para home
            if ("DIRETORIA".equals(userRole)) {
                return "redirect:/demands/director";
            }
            return "redirect:/";
        }

        UserAccount currentUser = getCurrentUser(authentication);

        List<Demand> myDemands = demandService.findAccessibleByUser(currentUser, userRole);

        // Aplica filtro de status se fornecido
        if (statusFilter != null) {
            myDemands = myDemands.stream()
                    .filter(d -> d.getStatus() == statusFilter)
                    .collect(Collectors.toList());
        }

        // Estatísticas
        long pendentes = myDemands.stream().filter(d -> d.getStatus() == DemandStatus.PENDENTE).count();
        long emAndamento = myDemands.stream().filter(d -> d.getStatus() == DemandStatus.EM_ANDAMENTO).count();
        long concluidas = myDemands.stream().filter(d -> d.getStatus() == DemandStatus.CONCLUIDA).count();

        model.addAttribute("demands", myDemands);
        model.addAttribute("pendentes", pendentes);
        model.addAttribute("emAndamento", emAndamento);
        model.addAttribute("concluidas", concluidas);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("statusOptions", DemandStatus.values());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("userRole", userRole);
        model.addAttribute("isDirector", isDirectorOrAdmin(userRole));

        return "minhas_demandas";
    }

    /**
     * Ver detalhes de uma demanda
     */
    @GetMapping("/{id}")
    public String viewDemand(
            @PathVariable Long id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        String userRole = getUserRole(authentication);

        // Bloqueia acesso de Associados (USER)
        if ("USER".equals(userRole)) {
            logger.warn("Acesso negado às demandas. Role USER/Associado não tem acesso. Role: {}", userRole);
            redirectAttributes.addFlashAttribute("error", "Você não tem permissão para acessar demandas.");
            return "redirect:/";
        }

        UserAccount currentUser = getCurrentUser(authentication);

        Demand demand = demandService.findById(id)
                .orElseThrow(() -> new RuntimeException("Demanda não encontrada"));

        // Verifica se o usuário tem acesso a essa demanda
        if (!hasAccessToDemand(demand, currentUser, userRole)) {
            logger.warn("Acesso negado à demanda {} pelo usuário {}", id, currentUser.getUsername());
            redirectAttributes.addFlashAttribute("error", "Você não tem permissão para ver esta demanda.");
            return "redirect:/demands/my-demands";
        }

        model.addAttribute("demand", demand);
        model.addAttribute("statusOptions", DemandStatus.values());
        model.addAttribute("isDirector", isDirectorOrAdmin(userRole));
        model.addAttribute("currentUser", currentUser);

        return "detalhes_demanda";
    }

    /**
     * Atualizar status de uma demanda
     */
    @PostMapping("/{id}/update-status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam DemandStatus status,
            @RequestParam(value = "completionObservation", required = false) String completionObservation,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String userRole = getUserRole(authentication);

        // Bloqueia acesso de Associados (USER)
        if ("USER".equals(userRole)) {
            logger.warn("Acesso negado às demandas. Role USER/Associado não tem acesso. Role: {}", userRole);
            redirectAttributes.addFlashAttribute("error", "Você não tem permissão para acessar demandas.");
            return "redirect:/";
        }

        UserAccount currentUser = getCurrentUser(authentication);

        String returnUrl = isDirectorOrAdmin(userRole) ? "/demands/director" : "/demands/my-demands";

        try {
            Demand demand = demandService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Demanda não encontrada"));

            // Verifica acesso
            if (!hasAccessToDemand(demand, currentUser, userRole)) {
                redirectAttributes.addFlashAttribute("error", "Você não tem permissão para atualizar esta demanda.");
                return "redirect:/demands/my-demands";
            }

            if (status == DemandStatus.CANCELADA && !isDemandCreator(demand, currentUser)) {
                redirectAttributes.addFlashAttribute("error", "Somente quem criou a demanda pode cancelá-la.");
                return "redirect:" + returnUrl;
            }

            demandService.updateStatus(id, status, completionObservation);

            logger.info("Status da demanda {} atualizado para {} por {}", id, status, currentUser.getUsername());
            redirectAttributes.addFlashAttribute("success", "Status atualizado com sucesso!");

        } catch (Exception e) {
            logger.error("Erro ao atualizar status da demanda " + id, e);
            redirectAttributes.addFlashAttribute("error", "Erro ao atualizar status: " + e.getMessage());
        }

        return "redirect:" + returnUrl;
    }

    /**
     * Atribuir demanda a si mesmo
     */
    @PostMapping("/{id}/assign-to-me")
    public String assignToMe(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String userRole = getUserRole(authentication);

        // Bloqueia acesso de Associados (USER)
        if ("USER".equals(userRole)) {
            logger.warn("Acesso negado às demandas. Role USER/Associado não tem acesso. Role: {}", userRole);
            redirectAttributes.addFlashAttribute("error", "Você não tem permissão para acessar demandas.");
            return "redirect:/";
        }

        UserAccount currentUser = getCurrentUser(authentication);

        try {
            Demand demand = demandService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Demanda não encontrada"));

            // Verifica se o usuário pode pegar essa demanda
            if (!hasAccessToDemand(demand, currentUser, userRole)) {
                redirectAttributes.addFlashAttribute("error", "Você não tem permissão para esta demanda.");
                return "redirect:/demands/my-demands";
            }

            demandService.assignToUser(id, currentUser);

            logger.info("Demanda {} atribuída a {}", id, currentUser.getUsername());
            redirectAttributes.addFlashAttribute("success", "Demanda atribuída a você!");

        } catch (Exception e) {
            logger.error("Erro ao atribuir demanda " + id, e);
            redirectAttributes.addFlashAttribute("error", "Erro ao atribuir demanda: " + e.getMessage());
        }

        return "redirect:/demands/my-demands";
    }

    /**
     * Editar demanda (apenas quem pode criar demandas)
     */
    @GetMapping("/{id}/edit")
    public String editDemand(
            @PathVariable Long id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        String userRole = getUserRole(authentication);

        if (!RoleType.canCreateDemands(userRole)) {
            redirectAttributes.addFlashAttribute("error", "Você não tem permissão para editar demandas.");
            return "redirect:/demands/my-demands";
        }

        Demand demand = demandService.findById(id)
                .orElseThrow(() -> new RuntimeException("Demanda não encontrada"));

        model.addAttribute("demand", demand);
        model.addAttribute("availableRoles", getAvailableRolesForUser(userRole));
        model.addAttribute("statusOptions", DemandStatus.values());
        model.addAttribute("priorityOptions", DemandPriority.values());
        model.addAttribute("selectedRoles", demand.getTargetRolesList());

        return "editar_demanda";
    }

    /**
     * Atualizar demanda
     */
    @PostMapping("/{id}/update")
    public String updateDemand(
            @PathVariable Long id,
            @ModelAttribute Demand updatedDemand,
            @RequestParam(value = "targetRolesList", required = false) List<String> targetRolesList,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String userRole = getUserRole(authentication);

        if (!RoleType.canCreateDemands(userRole)) {
            redirectAttributes.addFlashAttribute("error", "Você não tem permissão para editar demandas.");
            return "redirect:/demands/my-demands";
        }

        // Verifica se os roles de destino são válidos para a hierarquia do usuário
        List<String> allowedRoles = RoleType.getTargetRoleCodesFor(userRole);
        if (targetRolesList != null && !allowedRoles.containsAll(targetRolesList)) {
            logger.warn("Tentativa de atualizar demanda com roles não permitidos. User role: {}, Target roles: {}", userRole, targetRolesList);
            redirectAttributes.addFlashAttribute("error", "Você não tem permissão para enviar demandas para alguns dos cargos selecionados.");
            return "redirect:/demands/director";
        }

        try {
            Demand existing = demandService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Demanda não encontrada"));

            existing.setTitulo(updatedDemand.getTitulo());
            existing.setDescricao(updatedDemand.getDescricao());
            existing.setStatus(updatedDemand.getStatus());
            existing.setPrioridade(updatedDemand.getPrioridade());
            existing.setDueDate(updatedDemand.getDueDate());

            if (targetRolesList != null) {
                existing.setTargetRolesList(targetRolesList);
            }

            demandService.updateDemand(existing);

            logger.info("Demanda {} atualizada", id);
            redirectAttributes.addFlashAttribute("success", "Demanda atualizada com sucesso!");

        } catch (Exception e) {
            logger.error("Erro ao atualizar demanda " + id, e);
            redirectAttributes.addFlashAttribute("error", "Erro ao atualizar demanda: " + e.getMessage());
        }

        return "redirect:/demands/director";
    }

    /**
     * Deletar demanda (apenas quem pode criar demandas)
     */
    @PostMapping("/{id}/delete")
    public String deleteDemand(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String userRole = getUserRole(authentication);

        if (!RoleType.canCreateDemands(userRole)) {
            redirectAttributes.addFlashAttribute("error", "Você não tem permissão para deletar demandas.");
            return "redirect:/demands/my-demands";
        }

        try {
            demandService.deleteDemand(id);
            logger.info("Demanda {} deletada", id);
            redirectAttributes.addFlashAttribute("success", "Demanda deletada com sucesso!");

        } catch (Exception e) {
            logger.error("Erro ao deletar demanda " + id, e);
            redirectAttributes.addFlashAttribute("error", "Erro ao deletar demanda: " + e.getMessage());
        }

        return "redirect:/demands/director";
    }

    // ============ MÉTODOS AUXILIARES ============

    private UserAccount getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userAccountService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    private String getUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5)) // Remove "ROLE_" prefix
                .findFirst()
                .orElse("USER");
    }

    private boolean isDirectorOrAdmin(String role) {
        return "ADMIN".equals(role) || "DIRETORIA".equals(role);
    }

    private boolean hasAccessToDemand(Demand demand, UserAccount user, String userRole) {
        // Quem pode criar demandas tem acesso a tudo
        if (RoleType.canCreateDemands(userRole)) {
            return true;
        }

        // Criador tem acesso
        if (isDemandCreator(demand, user)) {
            return true;
        }

        // Atribuído tem acesso
        if (demand.getAssignedTo() != null && demand.getAssignedTo().getId().equals(user.getId())) {
            return true;
        }

        // Usuário do cargo destinatário tem acesso
        List<String> targetRoles = demand.getTargetRolesList();
        return targetRoles.contains(userRole);
    }

    /**
     * Retorna os roles disponíveis para o usuário baseado em sua hierarquia
     */
    private List<RoleType> getAvailableRolesForUser(String userRole) {
        return RoleType.getTargetRolesFor(userRole);
    }

    private boolean isDemandCreator(Demand demand, UserAccount user) {
        if (demand == null || user == null || demand.getCreatedBy() == null) {
            return false;
        }
        return demand.getCreatedBy().getId().equals(user.getId());
    }

    /**
     * Mantido para compatibilidade - usa a nova lógica de hierarquia
     * @deprecated Use getAvailableRolesForUser(String userRole) instead
     */
    @Deprecated
    private List<RoleType> getAvailableRoles() {
        // Retorna todos os roles exceto USER e ADMIN
        return Arrays.stream(RoleType.values())
                .filter(role -> !role.getCode().equals("USER") && !role.getCode().equals("ADMIN"))
                .collect(Collectors.toList());
    }
}
