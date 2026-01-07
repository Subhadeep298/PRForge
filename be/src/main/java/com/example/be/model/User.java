package com.example.be.model;

import com.example.be.dto.JiraConnectionOAuth;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String avatarUrl;

    @Column(unique = true)
    private String providerId; // GitHub ID or Jira Account ID

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jiraName", column = @Column(name = "jira_name")),
            @AttributeOverride(name = "jiraAccountId", column = @Column(name = "jira_account_id")),
            @AttributeOverride(name = "jiraEmail", column = @Column(name = "jira_email")),
            @AttributeOverride(name = "cloudId", column = @Column(name = "jira_cloud_id")),
            @AttributeOverride(name = "baseUrl", column = @Column(name = "jira_base_url")),
            @AttributeOverride(name = "oauthAccessToken", column = @Column(name = "jira_oauth_access_token", length = 4096)),
            @AttributeOverride(name = "oauthRefreshToken", column = @Column(name = "jira_oauth_refresh_token", length = 4096)),
            @AttributeOverride(name = "oauthAccessTokenExpiresAt", column = @Column(name = "jira_oauth_expires_at"))
    })
    private JiraConnectionOAuth jiraConnectionOAuth;
}
