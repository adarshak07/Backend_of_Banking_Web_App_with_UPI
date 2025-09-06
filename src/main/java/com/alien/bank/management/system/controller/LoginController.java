package com.alien.bank.management.system.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @Value("${feature.sso.enabled:false}")
    private boolean ssoEnabled;

    @GetMapping("/login")
    public String login() {
        if (ssoEnabled) {
            // Redirect to frontend login page
            return "redirect:http://localhost:3030/login";
        } else {
            // Redirect to frontend login page
            return "redirect:http://localhost:3030/login";
        }
    }
}
