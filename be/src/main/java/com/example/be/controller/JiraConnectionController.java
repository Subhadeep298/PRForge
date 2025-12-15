package com.example.be.controller;

import com.example.be.dto.JiraConnectionRequestDto;
import com.example.be.dto.JiraIssueDetails;
import com.example.be.dto.JiraIssueDetailsResponse;
import com.example.be.dto.ValidateResponse;
import com.example.be.model.JiraConnection;
import com.example.be.repository.JiraConnectionRepo;
import com.example.be.service.JiraConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@RestController
@RequestMapping("/jiraConnection")
@RequiredArgsConstructor
@Slf4j
public class JiraConnectionController {

    private final JiraConnectionService jiraConnectionService;
    private final JiraConnectionRepo jiraConnectionRepo;
    private final RestTemplate restTemplate;

    @PostMapping("/save")
    public ResponseEntity<ValidateResponse> saveJiraConnection(
            @RequestBody JiraConnectionRequestDto jiraConnectionRequestDto) {
        log.info("Received request to save Jira connection: {}", jiraConnectionRequestDto);
        ValidateResponse response = jiraConnectionService.saveJiraConnection(jiraConnectionRequestDto);
        log.info("Jira connection saved successfully: {}", response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getAllTickets/{connectionId}")
    public ResponseEntity<Map<String, String>> getAllTicketsByConnectionId(@PathVariable UUID connectionId) {
        log.info("Fetching all tickets for Jira connectionId: {}", connectionId);
        Map<String, String> tickets = jiraConnectionService.getAllTicketsByConnectionId(connectionId);
        log.info("Retrieved {} tickets for connectionId {}", tickets.size(), connectionId);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/getTicket/{ticketId}")
    public ResponseEntity<JiraIssueDetailsResponse> getTicketById(
            @PathVariable String ticketId,
            @RequestParam UUID connectionId) {
        log.info("Fetching ticket with ID: {} for connectionId: {}", ticketId, connectionId);
        JiraIssueDetailsResponse ticketDetails = jiraConnectionService.getTicketById(connectionId, ticketId);
        return ResponseEntity.ok(ticketDetails);
    }

    @GetMapping("/getAllConnections/{userId}")
    public ResponseEntity<Map<String, UUID>> getAllConnectionsByUserId(@PathVariable String userId) {
        log.info("Fetching all connections for Jira connectionId: {}", userId);
        return ResponseEntity.ok(jiraConnectionService.getAllConnectionsByUserid(userId));
    }

    // NEW: OAuth-based endpoints (no connectionId required)

    @GetMapping("/oauth/allTickets/{connectionId}")
    public ResponseEntity<Map<String, String>> getAllTicketsForCurrentUserOAuth(@PathVariable UUID connectionId) {
        Map<String, String> response =
                jiraConnectionService.getAllTicketsForCurrentUserOAuth(connectionId);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<JiraIssueDetailsResponse> getTicketByIdUsingOAuth(UUID connectionId, String ticketKey){

    }

    @GetMapping("/connection/{id}/token-test")
    public ResponseEntity<Map<String, Object>> testToken(@PathVariable UUID id) {
        JiraConnection conn = jiraConnectionRepo.findById(id).orElseThrow();

        Map<String, Object> result = new HashMap<>();
        result.put("connectionId", conn.getId());
        result.put("tokenLength", conn.getOauthAccessToken() != null ? conn.getOauthAccessToken().length() : 0);
        result.put("tokenPreview", conn.getOauthAccessToken() != null ? conn.getOauthAccessToken().substring(0, 30) + "..." : "NULL");
        result.put("baseUrl", conn.getBaseUrl());
        result.put("expiresAt", conn.getOauthAccessTokenExpiresAt());

        // 🔥 TEST JIRA API DIRECTLY
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + conn.getOauthAccessToken().trim());
            headers.set("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    conn.getBaseUrl() + "/rest/api/3/myself",
                    HttpMethod.GET, entity, String.class
            );

            result.put("jiraApiWorks", true);
            result.put("jiraUser", response.getBody().substring(0, 100));
        } catch (Exception e) {
            result.put("jiraApiWorks", false);
            result.put("jiraError", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }


//    @GetMapping("/oauth/allTickets")
//    public ResponseEntity<Map<String, String>> getAllTicketsForCurrentUserOAuth(
//            @AuthenticationPrincipal OAuth2User principal) {
//
//        String githubId = String.valueOf(principal.getAttribute("id"));
//        log.info("Fetching all tickets via OAuth for githubId {}", githubId);
//        Map<String, String> tickets =
//                jiraConnectionService.getAllTicketsForUserUsingOAuth(githubId);
//        return ResponseEntity.ok(tickets);
//    }
}
