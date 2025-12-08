package com.example.be.controller;

import com.example.be.dto.JiraConnectionRequestDto;
import com.example.be.dto.JiraIssueDetailsResponse;
import com.example.be.dto.ValidateResponse;
import com.example.be.service.JiraConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
@RestController
@RequestMapping("/jiraConnection")
@RequiredArgsConstructor
@Slf4j
public class JiraConnectionController {

    private final JiraConnectionService jiraConnectionService;

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

    @GetMapping("/oauth/issue/{ticketKey}")
    public ResponseEntity<JiraIssueDetailsResponse> getIssueForCurrentUserOAuth(
            @PathVariable String ticketKey,
            @AuthenticationPrincipal OAuth2User principal) {

        String githubId = String.valueOf(principal.getAttribute("id"));
        log.info("Fetching ticket {} via OAuth for githubId {}", ticketKey, githubId);
        JiraIssueDetailsResponse response =
                jiraConnectionService.getTicketByIdUsingOAuth(githubId, ticketKey);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/oauth/allTickets")
    public ResponseEntity<Map<String, String>> getAllTicketsForCurrentUserOAuth(
            @AuthenticationPrincipal OAuth2User principal) {

        String githubId = String.valueOf(principal.getAttribute("id"));
        log.info("Fetching all tickets via OAuth for githubId {}", githubId);
        Map<String, String> tickets =
                jiraConnectionService.getAllTicketsForUserUsingOAuth(githubId);
        return ResponseEntity.ok(tickets);
    }
}
