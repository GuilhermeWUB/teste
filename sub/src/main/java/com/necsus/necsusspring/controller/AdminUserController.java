package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.RoleType;
import com.necsus.necsusspring.service.UserAccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserAccountService userAccountService;

    public AdminUserController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("pageTitle", "SUB - Usuários");
        model.addAttribute("users", userAccountService.findAll());
        model.addAttribute("availableRoles", RoleType.assignableRoles());
        return "admin_users";
    }

    @PostMapping("/create")
    public String createUser(@RequestParam("fullName") String fullName,
                            @RequestParam("username") String username,
                            @RequestParam("email") String email,
                            @RequestParam("password") String password,
                            @RequestParam("role") String role,
                            RedirectAttributes redirectAttributes) {
        try {
            userAccountService.createUser(fullName, username, email, password, role);
            redirectAttributes.addFlashAttribute("successMessage", "Usuário criado com sucesso.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao criar usuário: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/role")
    public String updateRole(@PathVariable Long id,
                              @RequestParam("role") String role,
                              RedirectAttributes redirectAttributes) {
        try {
            userAccountService.updateRole(id, role);
            redirectAttributes.addFlashAttribute("successMessage", "Permissões atualizadas com sucesso.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível atualizar as permissões selecionadas.");
        }
        return "redirect:/admin/users";
    }
}
