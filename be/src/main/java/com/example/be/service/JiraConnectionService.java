package com.example.be.service;

import com.example.be.dto.JiraConnectionRequestDto;
import com.example.be.dto.JiraIssueDetails;
import com.example.be.dto.JiraIssueDetailsResponse;
import com.example.be.dto.ValidateResponse;
import org.springframework.stereotype.Service;



import java.util.Map;
import java.util.UUID;

@Service
public interface JiraConnectionService {

    ValidateResponse saveJiraConnection(JiraConnectionRequestDto jiraConnectionRequestDto);

    Map<String, String> getAllTicketsByConnectionId(UUID connectionId);

    JiraIssueDetailsResponse getTicketById(UUID connectionId, String ticketId);

    Map<String, UUID> getAllConnectionsByUserid(String ticketId);

    // New methods for OAuth‑based flow
    Map<String, String> getAllTicketsForCurrentUserOAuth(String userId);

    JiraIssueDetailsResponse getTicketByIdUsingOAuth(String userId, String ticketKey);

//    Map<String, String> getAllTicketsForUserUsingOAuth(String userId);
}
