package com.example.be.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DebugController {

    @GetMapping("/debug/oauth")
    public Map<String, String> debugOauth(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description) {
        return Map.of(
                "error", error != null ? error : "none",
                "error_description", error_description != null ? error_description : "none",
                "full_url", "Check browser address bar"
        );
    }
}

