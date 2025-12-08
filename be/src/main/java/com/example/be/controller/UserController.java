package com.example.be.controller;

import com.example.be.model.User;
import com.example.be.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/user")
    public Map<String, Object> getUser(@AuthenticationPrincipal OAuth2User principal) {
        log.info("Fetching current authenticated user info");
        Map<String, Object> response = new java.util.HashMap<>();
        String name = principal.getAttribute("name") != null ? principal.getAttribute("name")
                : principal.getAttribute("login");
        response.put("name", name);
        response.put("avatar_url", principal.getAttribute("avatar_url"));
        response.put("email", principal.getAttribute("email"));
        response.put("id", principal.getAttribute("id"));

        log.info("User info retrieved: {}", name);
        return response;
    }

    @GetMapping("/user/{providerId}")
    public User findUserByProviderId(@PathVariable String providerId){
        log.info("Fetching user with providerId: {}", providerId);
        User user = userService.findByProviderId(providerId);
        if (user != null) {
            log.info("User found: {}", user.getName());
        } else {
            log.warn("User not found for providerId: {}", providerId);
        }
        return user;
    }
}
