package com.example.be.controller;

import com.example.be.dto.CompareRequest;
import com.example.be.dto.JiraIssueDetailsResponse;
import com.example.be.dto.PRSuggestion;
import com.example.be.model.CompareResult;
import com.example.be.repository.CompareRepository;
import com.example.be.service.GitHubService;
import com.example.be.service.JiraConnectionService;
import com.example.be.service.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class CompareController {

    private final GitHubService gitHubService;
    private final CompareRepository compareRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final LLMService llmService;
    private final JiraConnectionService jiraConnectionService;

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

    /**
     * Generate PR suggestion WITHOUT Jira context (code changes only)
     */
    @PostMapping("/{id}/generate-pr-suggestion")
    public ResponseEntity<Map<String, Object>> generatePRSuggestion(
            @PathVariable Long id,
            Authentication authentication) {

        return generatePRSuggestionWithJira(id, null, authentication);
    }

    /**
     * Generate PR suggestion WITH Jira ticket context
     *
     * @param id Comparison result ID
     * @param ticketKey Jira ticket key (e.g., "DEM-123") - optional
     * @param authentication User authentication
     */
    @PostMapping("/{id}/generate-pr-suggestion-with-jira")
    public ResponseEntity<Map<String, Object>> generatePRSuggestionWithJira(
            @PathVariable Long id,
            @RequestParam(required = false) String ticketKey,
            Authentication authentication) {

        log.info("Generating PR suggestion for comparison ID: {}, Jira ticket: {}", id, ticketKey);

        if (authentication == null || !(authentication instanceof OAuth2AuthenticationToken)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(errorResponse);
        }

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = oauthToken.getPrincipal();

        // Get GitHub user ID
        String providerId = null;
        Object idAttr = principal.getAttribute("id");
        if (idAttr != null) {
            providerId = String.valueOf(idAttr);
        }

        // Find the comparison result
        CompareResult compareResult = compareRepository.findById(id).orElse(null);
        if (compareResult == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Comparison result not found");
            return ResponseEntity.status(404).body(errorResponse);
        }

        // Fetch Jira ticket details if ticketKey provided
        JiraIssueDetailsResponse jiraDetails = null;
        if (ticketKey != null && !ticketKey.trim().isEmpty() && providerId != null) {
            try {
                log.info("Fetching Jira ticket: {} for user: {}", ticketKey, providerId);
                jiraDetails = jiraConnectionService.getTicketByIdUsingOAuth(providerId, ticketKey);

                if (jiraDetails != null && jiraDetails.isSuccess()) {
                    log.info("✅ Jira ticket fetched successfully: {}", jiraDetails.getJiraIssueDetails().getTitle());
                } else {
                    log.warn("⚠️ Failed to fetch Jira ticket: {}",
                            jiraDetails != null ? jiraDetails.getMessage() : "null response");
                }
            } catch (Exception e) {
                log.error("❌ Error fetching Jira ticket {}: {}", ticketKey, e.getMessage());
                // Continue without Jira context - don't fail the entire request
            }
        } else {
            log.info("No Jira ticket key provided - generating PR suggestion from code changes only");
        }

        // Generate PR suggestion (with or without Jira context)
        PRSuggestion suggestion = llmService.generatePRSuggestion(compareResult, jiraDetails);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", suggestion);
        response.put("hasJiraContext", jiraDetails != null && jiraDetails.isSuccess());

        if (jiraDetails != null && jiraDetails.isSuccess()) {
            response.put("jiraTicket", Map.of(
                    "key", ticketKey,
                    "title", jiraDetails.getJiraIssueDetails().getTitle()
            ));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Generate PR suggestion with multiple Jira tickets (for complex PRs)
     */
    @PostMapping("/{id}/generate-pr-suggestion-multi-jira")
    public ResponseEntity<Map<String, Object>> generatePRSuggestionMultiJira(
            @PathVariable Long id,
            @RequestParam List<String> ticketKeys,
            Authentication authentication) {

        log.info("Generating PR suggestion with multiple Jira tickets: {}", ticketKeys);

        if (authentication == null || !(authentication instanceof OAuth2AuthenticationToken)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Not authenticated"));
        }

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String providerId = String.valueOf(oauthToken.getPrincipal().getAttribute("id"));

        CompareResult compareResult = compareRepository.findById(id).orElse(null);
        if (compareResult == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "error", "Comparison not found"));
        }

        // Fetch all Jira tickets
        List<JiraIssueDetailsResponse> jiraDetailsList = ticketKeys.stream()
                .map(key -> {
                    try {
                        return jiraConnectionService.getTicketByIdUsingOAuth(providerId, key);
                    } catch (Exception e) {
                        log.error("Failed to fetch ticket {}: {}", key, e.getMessage());
                        return null;
                    }
                })
                .filter(details -> details != null && details.isSuccess())
                .toList();

        // Merge Jira contexts (use first successful one for now, or enhance LLMService to handle multiple)
        JiraIssueDetailsResponse primaryJira = jiraDetailsList.isEmpty() ? null : jiraDetailsList.get(0);

        PRSuggestion suggestion = llmService.generatePRSuggestion(compareResult, primaryJira);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", suggestion,
                "jiraTicketsUsed", jiraDetailsList.size()
        ));
    }
}
