package com.example.be.controller;

import com.example.be.dto.CompareRequest;
import com.example.be.dto.PRSuggestion;
import com.example.be.model.CompareResult;
import com.example.be.repository.CompareRepository;
import com.example.be.service.GitHubService;
import com.example.be.service.LLMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compare")
public class CompareController {

    @Autowired
    private GitHubService gitHubService;

    @Autowired
    private CompareRepository compareRepository;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private LLMService llmService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> compareCommits(
            @RequestBody CompareRequest request,
            Authentication authentication) {

        if (authentication == null || !(authentication instanceof OAuth2AuthenticationToken)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(errorResponse);
        }

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = oauthToken.getPrincipal();

        // Get the OAuth2 access token
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName());

        if (client == null || client.getAccessToken() == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Unable to retrieve access token");
            return ResponseEntity.status(401).body(errorResponse);
        }

        String token = client.getAccessToken().getTokenValue();

        // Call GitHub API
        Map<String, Object> githubResponse = gitHubService.compareCommits(
                request.getOwner(),
                request.getRepo(),
                request.getBaseBranch(),
                request.getHeadBranch(),
                token);

        if (!(Boolean) githubResponse.get("success")) {
            return ResponseEntity.status(500).body(githubResponse);
        }

        // Save to database
        Long userId = null;
        Object idAttr = principal.getAttribute("id");
        if (idAttr != null) {
            userId = ((Number) idAttr).longValue();
        }

        CompareResult compareResult = CompareResult.builder()
                .owner(request.getOwner())
                .repo(request.getRepo())
                .baseBranch(request.getBaseBranch())
                .headBranch(request.getHeadBranch())
                .patches((String) githubResponse.get("patches"))
                .deletedCode((String) githubResponse.get("deletedCode"))
                .addedCode((String) githubResponse.get("addedCode"))
                .filesChanged((Integer) githubResponse.get("filesChanged"))
                .additions((Integer) githubResponse.get("additions"))
                .deletions((Integer) githubResponse.get("deletions"))
                .userId(userId)
                .build();

        compareRepository.save(compareResult);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", compareResult);
        response.put("message", "Comparison completed and saved successfully");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<CompareResult>> getHistory(Authentication authentication) {
        if (authentication == null || !(authentication instanceof OAuth2AuthenticationToken)) {
            return ResponseEntity.status(401).build();
        }

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = oauthToken.getPrincipal();

        Long userId = null;
        Object idAttr = principal.getAttribute("id");
        if (idAttr != null) {
            userId = ((Number) idAttr).longValue();
        }

        if (userId == null) {
            return ResponseEntity.status(400).build();
        }

        List<CompareResult> history = compareRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{id}/generate-pr-suggestion")
    public ResponseEntity<Map<String, Object>> generatePRSuggestion(
            @PathVariable Long id,
            Authentication authentication) {

        if (authentication == null || !(authentication instanceof OAuth2AuthenticationToken)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(errorResponse);
        }

        // Find the comparison result
        CompareResult compareResult = compareRepository.findById(id).orElse(null);
        if (compareResult == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Comparison result not found");
            return ResponseEntity.status(404).body(errorResponse);
        }

        // Generate PR suggestion using LLM
        PRSuggestion suggestion = llmService.generatePRSuggestion(compareResult);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", suggestion);

        return ResponseEntity.ok(response);
    }
}
