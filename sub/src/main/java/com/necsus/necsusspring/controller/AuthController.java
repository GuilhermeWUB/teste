package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.UserRegistrationDto;
import com.necsus.necsusspring.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserAccountService userAccountService;

    public AuthController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("pageTitle", "SUB - Login");
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("pageTitle", "SUB - Registrar");
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new UserRegistrationDto());
        }
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registrationForm") UserRegistrationDto registrationForm,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {

        if (!registrationForm.passwordsMatch()) {
            bindingResult.rejectValue("confirmPassword", "registrationForm.confirmPassword", "As senhas não conferem.");
        }

        if (userAccountService.existsByUsername(registrationForm.getUsername())) {
            bindingResult.rejectValue("username", "registrationForm.username", "Este usuário já está em uso.");
        }

        if (userAccountService.existsByEmail(registrationForm.getEmail())) {
            bindingResult.rejectValue("email", "registrationForm.email", "Este e-mail já está cadastrado.");
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registrationForm", bindingResult);
            redirectAttributes.addFlashAttribute("registrationForm", registrationForm);
            return "redirect:/register";
        }

        userAccountService.register(registrationForm);
        redirectAttributes.addFlashAttribute("successMessage", "Cadastro realizado com sucesso! Faça login para continuar.");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logoutView() {
        return "logout";
    }

    @GetMapping("/logout/success")
    public String logoutSuccess() {
        return "logout_success";
    }
}
