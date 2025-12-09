package com.example.be.repository;

import com.example.be.model.JiraConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface JiraConnectionRepo extends JpaRepository<JiraConnection, UUID> {

    JiraConnection findByName(String name);

    JiraConnection findByUserIdAndName(String userId, String name);

    List<JiraConnection> findAllByUserId(String userId);

    // Optional: at most one OAuth connection per (user, cloudId)
    Optional<JiraConnection> findByUserIdAndCloudId(String userId, String cloudId);

    Optional<JiraConnection> findByJiraAccountId(String jiraAccountId);

    @org.springframework.transaction.annotation.Transactional
    void deleteByJiraAccountId(String jiraAccountId);
}
