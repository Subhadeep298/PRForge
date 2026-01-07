package com.example.be.controller;

import com.example.be.dto.*;
import com.example.be.model.JiraConnection;
import com.example.be.model.User;
import com.example.be.repository.JiraConnectionRepo;
import com.example.be.repository.UserRepository;
import com.example.be.service.JiraConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;

import java.util.*;

@RestController
@RequestMapping("/jiraConnection")
@RequiredArgsConstructor
@Slf4j
public class JiraConnectionController {

    private final JiraConnectionService jiraConnectionService;
    private final RestTemplate restTemplate;
    private final UserRepository userRepository;

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

    @GetMapping("/oauth/allTickets/{userId}")
    public ResponseEntity<Map<String, String>> getAllTicketsForCurrentUserOAuth(@PathVariable String userId) {
        Map<String, String> response =
                jiraConnectionService.getAllTicketsForCurrentUserOAuth(userId);
        return ResponseEntity.ok(response);
    }

    // Controller
    @GetMapping("/jiraConnection/oauth/ticketById/{providerId}/{ticketKey}")
    public ResponseEntity<JiraIssueDetailsResponse> getTicketByIdUsingOAuth(
            @PathVariable String providerId,
            @PathVariable String ticketKey) {

        JiraIssueDetailsResponse response = jiraConnectionService.getTicketByIdUsingOAuth(providerId, ticketKey);
        return ResponseEntity.ok(response);
    }



//    @GetMapping("/debug/test-jira-tickets/{providerId}")
//    public Map<String, Object> testJiraTickets(@PathVariable String providerId) {
//        User user = userRepository.findByProviderId(providerId).get();
//        JiraConnectionOAuth connection = user.getJiraConnectionOAuth();
//
//        Map<String, Object> result = new HashMap<>();
//
//        try {
//            String searchUrl = "https://api.atlassian.com/ex/jira/" + connection.getCloudId()
//                    + "/rest/api/3/search/jql";
//
//            // Test ALL possible JQLs to find your tickets
//            String accountId = "712020:1f3ad339-300f-48f9-b428-ccd7919631d4";
//
//            String[] testJqls = {
//                    "assignee = \"" + accountId + "\"",
//                    "reporter = \"" + accountId + "\"",
//                    "creator = \"" + accountId + "\"",
//                    "assignee is EMPTY",  // Unassigned tickets
//                    "created >= -30d",     // Last 30 days
//                    "project is not EMPTY ORDER BY created DESC",  // All projects
//                    "status in (Open, \"To Do\", \"In Progress\", Done)",
//                    "text ~ \"*\""  // Any text (very broad)
//            };
//
//            Map<String, Object> allResults = new HashMap<>();
//
//            for (String jql : testJqls) {
//                try {
//                    Map<String, Object> requestBody = new HashMap<>();
//                    requestBody.put("jql", jql);
//                    requestBody.put("maxResults", 5);
//                    requestBody.put("fields", Arrays.asList("key", "summary", "assignee", "reporter", "status", "project"));
//
//                    HttpHeaders headers = new HttpHeaders();
//                    headers.setBearerAuth(connection.getOauthAccessToken());
//                    headers.setContentType(MediaType.APPLICATION_JSON);
//                    headers.set("Accept", "application/json");
//
//                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//                    ResponseEntity<JsonNode> searchResponse = restTemplate.exchange(
//                            searchUrl, HttpMethod.POST, entity, JsonNode.class);
//
//                    JsonNode searchBody = searchResponse.getBody();
//                    JsonNode issues = searchBody.get("issues");
//
//                    if (issues != null && issues.size() > 0) {
//                        List<Map<String, Object>> ticketList = new ArrayList<>();
//                        issues.forEach(issue -> {
//                            Map<String, Object> ticket = new HashMap<>();
//                            ticket.put("key", issue.get("key").asText());
//
//                            JsonNode fields = issue.get("fields");
//                            if (fields != null) {
//                                if (fields.has("summary")) {
//                                    ticket.put("summary", fields.get("summary").asText());
//                                }
//                                if (fields.has("project") && fields.get("project") != null) {
//                                    ticket.put("project", fields.get("project").get("key").asText());
//                                }
//                                if (fields.has("assignee") && !fields.get("assignee").isNull()) {
//                                    ticket.put("assignee", fields.get("assignee").get("displayName").asText());
//                                } else {
//                                    ticket.put("assignee", "Unassigned");
//                                }
//                                if (fields.has("reporter") && !fields.get("reporter").isNull()) {
//                                    ticket.put("reporter", fields.get("reporter").get("displayName").asText());
//                                }
//                            }
//                            ticketList.add(ticket);
//                        });
//
//                        allResults.put(jql, Map.of(
//                                "count", issues.size(),
//                                "tickets", ticketList
//                        ));
//                    }
//                } catch (Exception e) {
//                    allResults.put(jql, "ERROR: " + e.getMessage());
//                }
//            }
//
//            result.put("testResults", allResults);
//            result.put("status", allResults.isEmpty() ? "NO_TICKETS_ANYWHERE" : "FOUND_TICKETS");
//
//        } catch (Exception e) {
//            result.put("error", e.getMessage());
//            result.put("status", "FAILED");
//        }
//
//        return result;
//    }



//    @GetMapping("/connection/{id}/token-test")
//    public ResponseEntity<Map<String, Object>> testToken(@PathVariable UUID id) {
//        JiraConnection conn = jiraConnectionRepo.findById(id).orElseThrow();
//
//        Map<String, Object> result = new HashMap<>();
//        result.put("connectionId", conn.getId());
//        result.put("tokenLength", conn.getOauthAccessToken() != null ? conn.getOauthAccessToken().length() : 0);
//        result.put("tokenPreview", conn.getOauthAccessToken() != null ? conn.getOauthAccessToken().substring(0, 30) + "..." : "NULL");
//        result.put("baseUrl", conn.getBaseUrl());
//        result.put("expiresAt", conn.getOauthAccessTokenExpiresAt());
//
//        // 🔥 TEST JIRA API DIRECTLY
//        try {
//            HttpHeaders headers = new HttpHeaders();
//            headers.set("Authorization", "Bearer " + conn.getOauthAccessToken().trim());
//            headers.set("Accept", "application/json");
//
//            HttpEntity<Void> entity = new HttpEntity<>(headers);
//            ResponseEntity<String> response = restTemplate.exchange(
//                    conn.getBaseUrl() + "/rest/api/3/myself",
//                    HttpMethod.GET, entity, String.class
//            );
//
//            result.put("jiraApiWorks", true);
//            result.put("jiraUser", response.getBody().substring(0, 100));
//        } catch (Exception e) {
//            result.put("jiraApiWorks", false);
//            result.put("jiraError", e.getMessage());
//        }
//
//        return ResponseEntity.ok(result);
//    }


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
