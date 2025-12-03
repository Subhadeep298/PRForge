package com.example.be.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping("/user")
    public Map<String, Object> getUser(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("name", principal.getAttribute("name") != null ? principal.getAttribute("name")
                : principal.getAttribute("login"));
        response.put("avatar_url", principal.getAttribute("avatar_url"));
        response.put("email", principal.getAttribute("email"));
        response.put("id", principal.getAttribute("id"));
        return response;
    }
}
