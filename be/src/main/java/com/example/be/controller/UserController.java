package com.example.be.controller;

import com.example.be.dto.JiraConnectionOAuth;
import com.example.be.model.User;
import com.example.be.repository.UserRepository;
import com.example.be.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/user")
    public Map<String, Object> getUser(@AuthenticationPrincipal OAuth2User principal) {
        log.info("Fetching current authenticated user info");
        Map<String, Object> response = new HashMap<>();

        // Determine authentication source
        Object githubIdObj = principal.getAttribute("id");
        Object jiraAccountIdObj = principal.getAttribute("account_id");

        String finalName;
        Object finalAvatarUrl;
        Object finalId;
        String finalEmail = principal.getAttribute("email");
        boolean jiraConnected = false;

        if (jiraAccountIdObj != null) {
            // ✅ Jira OAuth login
            String jiraAccountId = String.valueOf(jiraAccountIdObj);
            log.info("Principal is Jira User: {}", jiraAccountId);

            // Find GitHub user with this Jira OAuth connection (User.jiraConnectionOAuth)
            User linkedUser = userRepository.findAll().stream()
                    .filter(u -> u.getJiraConnectionOAuth() != null
                            && jiraAccountId.equals(u.getJiraConnectionOAuth().getJiraAccountId()))
                    .findFirst()
                    .orElse(null);

            if (linkedUser != null) {
                finalName = linkedUser.getName();
                finalAvatarUrl = linkedUser.getAvatarUrl();
                finalId = linkedUser.getProviderId();
                if (linkedUser.getEmail() != null) {
                    finalEmail = linkedUser.getEmail();
                }
                jiraConnected = true;
                log.info("Found linked GitHub user: {}", linkedUser.getProviderId());
            } else {
                finalName = principal.getAttribute("name");
                finalAvatarUrl = principal.getAttribute("picture");
                finalId = jiraAccountId;
                jiraConnected = true;
                log.warn("Standalone Jira login (no GitHub link)");
            }
        } else if (githubIdObj != null) {
            // ✅ GitHub login
            String githubId = String.valueOf(githubIdObj);
            log.info("Principal is GitHub User: {}", githubId);

            User githubUser = userService.findByProviderId(githubId);

            String name = principal.getAttribute("name");
            if (name == null) name = principal.getAttribute("login");

            finalName = name;
            Object avatar = principal.getAttribute("avatar_url");
            if (avatar == null) avatar = principal.getAttribute("picture");
            finalAvatarUrl = avatar;
            finalId = githubId;

            // ✅ ONLY check User.jiraConnectionOAuth (ignore manual connections)
            jiraConnected = githubUser != null && githubUser.getJiraConnectionOAuth() != null;
        } else {
            log.warn("Unknown principal type");
            finalName = "Unknown";
            finalAvatarUrl = null;
            finalId = null;
        }

        response.put("name", finalName);
        response.put("avatar_url", finalAvatarUrl);
        response.put("email", finalEmail);
        response.put("id", finalId);
        response.put("jira_connected", jiraConnected);  // ✅ Only User.jiraConnectionOAuth

        log.info("User info: {}, jira_connected: {}", finalName, jiraConnected);
        return response;
    }

    @GetMapping("/user/{providerId}")
    public User findUserByProviderId(@PathVariable String providerId) {
        log.info("Fetching user with providerId: {}", providerId);
        User user = userService.findByProviderId(providerId);
        if (user != null) {
            log.info("User found: {}", user.getName());
        } else {
            log.warn("User not found for providerId: {}", providerId);
        }
        return user;
    }

    @DeleteMapping("/jira/disconnect")
    public ResponseEntity<?> disconnectJira(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Object githubIdObj = principal.getAttribute("id");
        Object jiraAccountIdObj = principal.getAttribute("account_id");

        if (jiraAccountIdObj != null) {
            // ✅ Jira login: Remove User.jiraConnectionOAuth
            String jiraAccountId = String.valueOf(jiraAccountIdObj);
            log.info("Disconnecting Jira OAuth for accountId: {}", jiraAccountId);

            User linkedUser = userRepository.findAll().stream()
                    .filter(u -> u.getJiraConnectionOAuth() != null
                            && jiraAccountId.equals(u.getJiraConnectionOAuth().getJiraAccountId()))
                    .findFirst()
                    .orElse(null);

            if (linkedUser != null) {
                linkedUser.setJiraConnectionOAuth(null);
                userRepository.save(linkedUser);
                log.info("Jira OAuth disconnected from user: {}", linkedUser.getProviderId());
                return ResponseEntity.ok().build();
            }
            log.warn("No linked user found for Jira accountId: {}", jiraAccountId);

        } else if (githubIdObj != null) {
            // ✅ GitHub login: Remove User.jiraConnectionOAuth
            String githubId = String.valueOf(githubIdObj);
            log.info("Disconnecting Jira OAuth for GitHub user: {}", githubId);

            User githubUser = userService.findByProviderId(githubId);
            if (githubUser != null && githubUser.getJiraConnectionOAuth() != null) {
                githubUser.setJiraConnectionOAuth(null);
                userRepository.save(githubUser);
                log.info("Jira OAuth disconnected from GitHub user: {}", githubId);
                return ResponseEntity.ok().build();
            }
            log.info("No Jira OAuth connection found for user: {}", githubId);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/jira/connection-status/{providerId}")
    public Map<String, Object> getJiraConnectionStatus(@PathVariable String providerId) {
        log.info("Checking Jira OAuth status for providerId: {}", providerId);

        Map<String, Object> status = new HashMap<>();

        User user = userService.findByProviderId(providerId);
        if (user == null) {
            status.put("error", "User not found");
            return status;
        }

        // ✅ ONLY check User.jiraConnectionOAuth
        JiraConnectionOAuth oauthConn = user.getJiraConnectionOAuth();
        boolean hasJiraConnection = oauthConn != null && oauthConn.getOauthAccessToken() != null;

        status.put("jira_connected", hasJiraConnection);

        if (hasJiraConnection) {
            status.put("jiraDetails", Map.of(
                    "jiraEmail", oauthConn.getJiraEmail(),
                    "jiraName", oauthConn.getJiraName(),
                    "jiraAccountId", oauthConn.getJiraAccountId(),
                    "cloudId", oauthConn.getCloudId()
            ));
        }

        log.info("Jira OAuth status for {}: {}", providerId, hasJiraConnection);
        return status;
    }
}
