package com.example.be.controller;

import com.example.be.model.User;
import com.example.be.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final com.example.be.repository.JiraConnectionRepo jiraConnectionRepo;

    @GetMapping("/user")
    public Map<String, Object> getUser(@AuthenticationPrincipal OAuth2User principal) {
        log.info("Fetching current authenticated user info");
        Map<String, Object> response = new java.util.HashMap<>();

        // Determine authentication source
        Object githubIdObj = principal.getAttribute("id");
        Object jiraAccountIdObj = principal.getAttribute("account_id");

        String finalName;
        Object finalAvatarUrl;
        Object finalId;
        String finalEmail = principal.getAttribute("email");
        boolean jiraConnected = false;

        if (jiraAccountIdObj != null) {
            // Logged in with Jira Principal
            String jiraAccountId = String.valueOf(jiraAccountIdObj);
            log.info("Principal is Jira User: {}", jiraAccountId);

            // Check if this Jira account is linked to a GitHub user
            java.util.Optional<com.example.be.model.JiraConnection> connectionOpt = jiraConnectionRepo
                    .findByJiraAccountId(jiraAccountId);

            if (connectionOpt.isPresent()) {
                // LINK FOUND: Mask as GitHub User
                com.example.be.model.JiraConnection connection = connectionOpt.get();
                String githubUserId = connection.getUserId();
                log.info("Found linked GitHub user ID: {}", githubUserId);

                User githubUser = userService.findByProviderId(githubUserId);
                if (githubUser != null) {
                    finalName = githubUser.getName();
                    finalAvatarUrl = githubUser.getAvatarUrl();
                    finalId = githubUser.getProviderId(); // Use GitHub ID
                    // Optionally use GitHub email if needed, but session is Jira's
                    if (githubUser.getEmail() != null)
                        finalEmail = githubUser.getEmail();
                    jiraConnected = true;
                    log.info("Masking identity as GitHub User: {}", finalName);
                } else {
                    // Fallback to Jira profile if local user not found
                    finalName = principal.getAttribute("name");
                    finalAvatarUrl = principal.getAttribute("picture");
                    finalId = jiraAccountId;
                    jiraConnected = true;
                }
            } else {
                // STANDALONE JIRA LOGIN (Not linked)
                finalName = principal.getAttribute("name");
                finalAvatarUrl = principal.getAttribute("picture");
                finalId = jiraAccountId;
                jiraConnected = true;
            }
        } else {
            // Logged in with GitHub Principal
            log.info("Principal is GitHub User");
            String name = principal.getAttribute("name");
            if (name == null)
                name = principal.getAttribute("login");

            finalName = name;
            Object avatar = principal.getAttribute("avatar_url");
            if (avatar == null)
                avatar = principal.getAttribute("picture");
            finalAvatarUrl = avatar;
            finalId = githubIdObj;

            // Check for Jira connection
            if (finalId != null) {
                jiraConnected = !jiraConnectionRepo.findAllByUserId(String.valueOf(finalId)).isEmpty();
            }
        }

        response.put("name", finalName);
        response.put("avatar_url", finalAvatarUrl);
        response.put("email", finalEmail);
        response.put("id", finalId);
        response.put("jira_connected", jiraConnected);

        log.info("User info retrieved: {}, jira_connected: {}", finalName, jiraConnected);
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

    @org.springframework.web.bind.annotation.DeleteMapping("/jira/disconnect")
    public org.springframework.http.ResponseEntity<?> disconnectJira(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return org.springframework.http.ResponseEntity.status(401).build();
        }

        Object githubIdObj = principal.getAttribute("id");
        Object jiraAccountIdObj = principal.getAttribute("account_id");

        if (jiraAccountIdObj != null) {
            // Logged in via Jira: Find connection by Jira Account ID
            String jiraAccountId = String.valueOf(jiraAccountIdObj);
            log.info("Disconnecting Jira for Jira Principal: {}", jiraAccountId);

            // Use derived delete method
            jiraConnectionRepo.deleteByJiraAccountId(jiraAccountId);

        } else if (githubIdObj != null) {
            // Logged in via GitHub: Find connections by GitHub User ID
            String userId = String.valueOf(githubIdObj);
            log.info("Disconnecting Jira for GitHub User: {}", userId);

            java.util.List<com.example.be.model.JiraConnection> connections = jiraConnectionRepo
                    .findAllByUserId(userId);
            jiraConnectionRepo.deleteAll(connections);
        } else {
            log.warn("Unknown principal type during disconnect");
            return org.springframework.http.ResponseEntity.badRequest().build();
        }

        return org.springframework.http.ResponseEntity.ok().build();
    }
}
