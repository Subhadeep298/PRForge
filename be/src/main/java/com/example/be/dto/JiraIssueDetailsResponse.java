package com.example.be.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraIssueDetailsResponse {

    private boolean success;

    private String message;

    private JiraIssueDetails jiraIssueDetails;
}
