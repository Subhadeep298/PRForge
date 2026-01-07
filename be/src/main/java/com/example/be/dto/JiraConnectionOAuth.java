package com.example.be.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;


@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraConnectionOAuth {

    @NotBlank(message = "Connection name is required")
    private String jiraName;

    @NotBlank(message = "Jira account ID is required")
    private String jiraAccountId;

    private String jiraEmail;

    @NotBlank(message = "Cloud ID is required")
    private String cloudId;

    @NotBlank(message = "Base URL is required")
    private String baseUrl;

    @NotNull(message = "Access token is required")
    @Column(length = 4096)
    private String oauthAccessToken;
    @Column(length = 4096)
    private String oauthRefreshToken;

    private Instant oauthAccessTokenExpiresAt;
}

