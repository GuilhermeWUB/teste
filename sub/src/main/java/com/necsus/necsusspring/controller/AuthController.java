package com.necsus.necsusspring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "login";
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
