package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.service.DemandService;
import com.necsus.necsusspring.service.UserAccountService;
import jakarta.validation.Valid;
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

        // Admin e Diretoria vão para painel de gestão
        if (isDirectorOrAdmin(userRole)) {
            return "redirect:/demands/director";
        }

        // Outros usuários vão para suas demandas
        return "redirect:/demands/my-demands";
    }

    /**
     * Painel do Diretor - Criar e gerenciar demandas
     * Acessível apenas para DIRETORIA e ADMIN
     */
    @GetMapping("/director")
    public String showDirectorPanel(Authentication authentication, Model model) {
        String userRole = getUserRole(authentication);

        if (!isDirectorOrAdmin(userRole)) {
            logger.warn("Acesso negado ao painel de diretor. Role: {}", userRole);
            return "redirect:/demands/my-demands";
        }

        UserAccount currentUser = getCurrentUser(authentication);

        // Lista todas as demandas
        List<Demand> allDemands = demandService.findAll();

        // Separa por status para estatísticas
        long pendentes = allDemands.stream().filter(d -> d.getStatus() == DemandStatus.PENDENTE).count();
        long emAndamento = allDemands.stream().filter(d -> d.getStatus() == DemandStatus.EM_ANDAMENTO).count();
        long concluidas = allDemands.stream().filter(d -> d.getStatus() == DemandStatus.CONCLUIDA).count();

        model.addAttribute("demands", allDemands);
        model.addAttribute("pendentes", pendentes);
        model.addAttribute("emAndamento", emAndamento);
        model.addAttribute("concluidas", concluidas);
        model.addAttribute("newDemand", new Demand());
        model.addAttribute("availableRoles", getAvailableRoles());
        model.addAttribute("statusOptions", DemandStatus.values());
        model.addAttribute("priorityOptions", DemandPriority.values());
        model.addAttribute("currentUser", currentUser);

        return "demandas_diretor";
    }

    /**
     * Criar nova demanda
     */
    @PostMapping("/create")
    public String createDemand(
            @ModelAttribute("newDemand") Demand demand,
            @RequestParam(value = "targetRolesList", required = false) List<String> targetRolesList,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String userRole = getUserRole(authentication);

        if (!isDirectorOrAdmin(userRole)) {
            logger.warn("Tentativa de criar demanda sem permissão. Role: {}", userRole);
            redirectAttributes.addFlashAttribute("error", "Você não tem permissão para criar demandas.");
            return "redirect:/demands/my-demands";
        }

        try {
            UserAccount currentUser = getCurrentUser(authentication);
            demand.setCreatedBy(currentUser);

            // Define os cargos destinatários
            if (targetRolesList != null && !targetRolesList.isEmpty()) {
                demand.setTargetRolesList(targetRolesList);
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
     * Minhas Demandas - Visualização para usuários comuns
     */
    @GetMapping("/my-demands")
    public String showMyDemands(
            Authentication authentication,
            @RequestParam(value = "status", required = false) DemandStatus statusFilter,
            Model model) {

        UserAccount currentUser = getCurrentUser(authentication);
        String userRole = getUserRole(authentication);

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

        UserAccount currentUser = getCurrentUser(authentication);
        String userRole = getUserRole(authentication);

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

        return "detalhes_demanda";
    }

    /**
     * Atualizar status de uma demanda
     */
    @PostMapping("/{id}/update-status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam DemandStatus status,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        UserAccount currentUser = getCurrentUser(authentication);
        String userRole = getUserRole(authentication);

        try {
            Demand demand = demandService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Demanda não encontrada"));

            // Verifica acesso
            if (!hasAccessToDemand(demand, currentUser, userRole)) {
                redirectAttributes.addFlashAttribute("error", "Você não tem permissão para atualizar esta demanda.");
                return "redirect:/demands/my-demands";
            }

            demandService.updateStatus(id, status);

            logger.info("Status da demanda {} atualizado para {} por {}", id, status, currentUser.getUsername());
            redirectAttributes.addFlashAttribute("success", "Status atualizado com sucesso!");

        } catch (Exception e) {
            logger.error("Erro ao atualizar status da demanda " + id, e);
            redirectAttributes.addFlashAttribute("error", "Erro ao atualizar status: " + e.getMessage());
        }

        String returnUrl = isDirectorOrAdmin(userRole) ? "/demands/director" : "/demands/my-demands";
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

        UserAccount currentUser = getCurrentUser(authentication);
        String userRole = getUserRole(authentication);

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
     * Editar demanda (apenas diretor/admin)
     */
    @GetMapping("/{id}/edit")
    public String editDemand(
            @PathVariable Long id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        String userRole = getUserRole(authentication);

        if (!isDirectorOrAdmin(userRole)) {
            redirectAttributes.addFlashAttribute("error", "Você não tem permissão para editar demandas.");
            return "redirect:/demands/my-demands";
        }

        Demand demand = demandService.findById(id)
                .orElseThrow(() -> new RuntimeException("Demanda não encontrada"));

        model.addAttribute("demand", demand);
        model.addAttribute("availableRoles", getAvailableRoles());
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

        if (!isDirectorOrAdmin(userRole)) {
            redirectAttributes.addFlashAttribute("error", "Você não tem permissão para editar demandas.");
            return "redirect:/demands/my-demands";
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
     * Deletar demanda (apenas diretor/admin)
     */
    @PostMapping("/{id}/delete")
    public String deleteDemand(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String userRole = getUserRole(authentication);

        if (!isDirectorOrAdmin(userRole)) {
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
        // Admin e Diretoria têm acesso a tudo
        if (isDirectorOrAdmin(userRole)) {
            return true;
        }

        // Criador tem acesso
        if (demand.getCreatedBy().getId().equals(user.getId())) {
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

    private List<RoleType> getAvailableRoles() {
        // Retorna todos os roles exceto USER e ADMIN
        return Arrays.stream(RoleType.values())
                .filter(role -> !role.getCode().equals("USER") && !role.getCode().equals("ADMIN"))
                .collect(Collectors.toList());
    }
}
