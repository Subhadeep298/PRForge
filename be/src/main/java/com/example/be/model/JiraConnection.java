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
    private String name;

    private String username;
    private String domainUrl;
    @Column(length = 4096)
    private String token;
    private String projectName;
    private String projectKey;

    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date updatedAt;
}
