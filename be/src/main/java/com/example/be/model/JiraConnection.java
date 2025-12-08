package com.example.be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "jira_connection")
public class JiraConnection {

    @Id
    private UUID id;

    // GitHub user id (providerId)
    private String userId;

    @Column(unique = true)
    private String name;          // manual connection name

    // Manual auth fields (keep as‑is)
    private String username;
    private String domainUrl;
    private String token;
    private String projectName;
    private String projectKey;

    // New: OAuth‑based Jira fields
    private String jiraAccountId;     // from /me
    private String jiraEmail;
    private String cloudId;           // Jira site id from accessible-resources
    private String baseUrl;           // e.g. https://your-site.atlassian.net

    private String oauthAccessToken;
    private String oauthRefreshToken;
    @Temporal(TemporalType.TIMESTAMP)
    private Date oauthAccessTokenExpiresAt;

    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date updatedAt;
}


