package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.service.NotificationService;
import com.necsus.necsusspring.service.UserAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller para gerenciar notificações do sistema.
 * Fornece endpoints REST para APIs e endpoints web para visualização.
 */
@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final UserAccountService userAccountService;

    public NotificationController(NotificationService notificationService,
                                  UserAccountService userAccountService) {
        this.notificationService = notificationService;
        this.userAccountService = userAccountService;
    }

    // ========== Endpoints WEB (Thymeleaf) ==========

    /**
     * Página principal de notificações
     */
    @GetMapping
    public String index(Authentication authentication,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String filter,
                       Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        UserAccount currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            logger.error("Usuário não encontrado: {}", authentication.getName());
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationsPage;

        // Aplica filtro se especificado
        if ("unread".equals(filter)) {
            notificationsPage = notificationService.findUnreadByRecipient(currentUser, pageable);
            model.addAttribute("filter", "unread");
            model.addAttribute("pageTitle", "SUB - Notificações Não Lidas");
        } else if ("archived".equals(filter)) {
            notificationsPage = notificationService.findArchivedByRecipient(currentUser, pageable);
            model.addAttribute("filter", "archived");
            model.addAttribute("pageTitle", "SUB - Notificações Arquivadas");
        } else {
            // Mostra todas as notificações, incluindo arquivadas
            notificationsPage = notificationService.findByRecipient(currentUser, pageable);
            model.addAttribute("filter", "all");
            model.addAttribute("pageTitle", "SUB - Notificações");
        }

        // Contadores
        long unreadCount = notificationService.countUnread(currentUser);
        long totalCount = notificationService.countAll(currentUser);

        model.addAttribute("notifications", notificationsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notificationsPage.getTotalPages());
        model.addAttribute("totalElements", notificationsPage.getTotalElements());
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("notificationTypes", NotificationType.values());

        return "notifications/index";
    }

    /**
     * Visualiza uma notificação específica e marca como lida
     */
    @GetMapping("/{id}")
    public String view(@PathVariable Long id,
                      Authentication authentication,
                      RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        UserAccount currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            Notification notification = notificationService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));

            // Verifica se a notificação pertence ao usuário atual
            if (!notification.getRecipient().getId().equals(currentUser.getId())) {
                logger.warn("Usuário {} tentou acessar notificação de outro usuário", authentication.getName());
                redirectAttributes.addFlashAttribute("errorMessage", "Acesso negado.");
                return "redirect:/notifications";
            }

            // Marca como lida se ainda não foi
            if (notification.isUnread()) {
                notificationService.markAsRead(id);
            }

            // Redireciona para a URL de ação se existir
            if (notification.getActionUrl() != null && !notification.getActionUrl().isEmpty()) {
                return "redirect:" + notification.getActionUrl();
            }

            // Caso contrário, volta para a lista de notificações
            redirectAttributes.addFlashAttribute("successMessage", "Notificação visualizada.");
            return "redirect:/notifications";

        } catch (Exception e) {
            logger.error("Erro ao visualizar notificação {}: ", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao visualizar notificação.");
            return "redirect:/notifications";
        }
    }

    // ========== API REST Endpoints ==========

    /**
     * API: Lista todas as notificações do usuário
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<Notification>> listNotifications(Authentication authentication,
                                                                @RequestParam(required = false) String filter) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserAccount currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Notification> notifications;
        if ("unread".equals(filter)) {
            notifications = notificationService.findUnreadByRecipient(currentUser);
        } else if ("recent".equals(filter)) {
            notifications = notificationService.findRecentNotifications(currentUser);
        } else if ("archived".equals(filter)) {
            notifications = notificationService.findArchivedByRecipient(currentUser);
        } else {
            notifications = notificationService.findByRecipient(currentUser);
        }

        return ResponseEntity.ok(notifications);
    }

    /**
     * API: Busca as últimas N notificações
     */
    @GetMapping("/api/recent/{limit}")
    @ResponseBody
    public ResponseEntity<List<Notification>> getRecentNotifications(Authentication authentication,
                                                                     @PathVariable int limit) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserAccount currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Notification> notifications = notificationService.findTopN(currentUser, limit);
        return ResponseEntity.ok(notifications);
    }

    /**
     * API: Conta notificações não lidas
     */
    @GetMapping("/api/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserAccount currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        long unreadCount = notificationService.countUnread(currentUser);
        Map<String, Long> response = new HashMap<>();
        response.put("unreadCount", unreadCount);

        return ResponseEntity.ok(response);
    }

    /**
     * API: Marca uma notificação como lida
     */
    @PostMapping("/api/{id}/mark-read")
    @ResponseBody
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long id,
                                                          Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserAccount currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Notification notification = notificationService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));

            // Verifica permissão
            if (!notification.getRecipient().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            notificationService.markAsRead(id);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Notificação marcada como lida");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Erro ao marcar notificação {} como lida: ", id, e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erro ao marcar notificação como lida");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * API: Marca uma notificação como não lida
     */
    @PostMapping("/api/{id}/mark-unread")
    @ResponseBody
    public ResponseEntity<Map<String, String>> markAsUnread(@PathVariable Long id,
                                                            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserAccount currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Notification notification = notificationService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));

            // Verifica permissão
            if (!notification.getRecipient().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            notificationService.markAsUnread(id);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Notificação marcada como não lida");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Erro ao marcar notificação {} como não lida: ", id, e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erro ao marcar notificação como não lida");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * API: Marca todas as notificações como lidas
     */
    @PostMapping("/api/mark-all-read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserAccount currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            int count = notificationService.markAllAsRead(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", count + " notificações marcadas como lidas");
            response.put("count", count);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Erro ao marcar todas as notificações como lidas: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erro ao marcar notificações como lidas");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * API: Arquiva uma notificação
     */
    @PostMapping("/api/{id}/archive")
    @ResponseBody
    public ResponseEntity<Map<String, String>> archiveNotification(@PathVariable Long id,
                                                                   Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserAccount currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Notification notification = notificationService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));

            // Verifica permissão
            if (!notification.getRecipient().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            notificationService.archiveNotification(id);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Notificação arquivada");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Erro ao arquivar notificação {}: ", id, e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erro ao arquivar notificação");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * API: Deleta uma notificação
     */
    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long id,
                                                                  Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserAccount currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Notification notification = notificationService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));

            // Verifica permissão
            if (!notification.getRecipient().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            notificationService.deleteNotification(id);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Notificação deletada");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Erro ao deletar notificação {}: ", id, e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erro ao deletar notificação");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== Métodos auxiliares ==========

    private UserAccount getCurrentUser(Authentication authentication) {
        return userAccountService.findByUsername(authentication.getName())
                .orElse(null);
    }
}
