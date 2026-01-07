package com.example.be.security;

import com.example.be.dto.JiraConnectionOAuth;
import com.example.be.model.User;
import com.example.be.repository.JiraConnectionRepo;
import com.example.be.repository.UserRepository;
import tools.jackson.databind.JsonNode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RestTemplate restTemplate;

    public OAuth2LoginSuccessHandler(UserRepository userRepository,
            JiraConnectionRepo jiraConnectionRepo,
            OAuth2AuthorizedClientService authorizedClientService,
            RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.authorizedClientService = authorizedClientService;
        this.restTemplate = restTemplate;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws ServletException, IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId(); // "github" or "jira"

        OAuth2User oauth2User = oauthToken.getPrincipal();
        Map<String, Object> attributes = oauth2User.getAttributes();

        if ("github".equals(registrationId)) {
            handleGithubLogin(attributes, request);
        } else if ("jira".equals(registrationId)) {
            handleJiraLogin(oauthToken, oauth2User, request);
        }

        this.setDefaultTargetUrl("http://localhost:5173");
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private void handleGithubLogin(Map<String, Object> attributes, HttpServletRequest request) {
        String providerId = String.valueOf(attributes.get("id"));
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String avatarUrl = (String) attributes.get("avatar_url");
        if (name == null) {
            name = (String) attributes.get("login");
        }

        User user = userRepository.findByProviderId(providerId)
                .orElseGet(() -> User.builder()
                        .providerId(providerId)
                        .email(email)
                        .build());

        user.setName(name);
        user.setAvatarUrl(avatarUrl);
        if (email != null) {
            user.setEmail(email);
        }
        userRepository.save(user);

        // store github id in session so we can link Jira login to this user
        request.getSession().setAttribute("github_id", providerId);
    }

    private void handleJiraLogin(OAuth2AuthenticationToken oauthToken,
                                 OAuth2User oauth2User,
                                 HttpServletRequest request) {

        // 1. Get Jira Profile Info
        String jiraAccountId = (String) oauth2User.getAttribute("account_id");
        String jiraEmail = (String) oauth2User.getAttribute("email");
        String jiraName = (String) oauth2User.getAttribute("name");
        String jiraAvatar = (String) oauth2User.getAttribute("picture");

        // 2. Identify or Create User (Standalone Support)
        String githubId = (String) request.getSession().getAttribute("github_id");
        User user;

        if (githubId != null) {
            // Case A: Link to existing logged-in session (e.g. GitHub user adding Jira)
            user = userRepository.findByProviderId(githubId)
                    .orElseThrow(() -> new IllegalStateException("User from session not found"));
        } else {
            // Case B: Standalone Jira Login
            user = userRepository.findByProviderId(jiraAccountId).orElse(null);

            if (user == null && jiraEmail != null) {
                // Try to find by email
                user = userRepository.findByEmail(jiraEmail).orElse(null);
            }

            if (user == null) {
                user = User.builder()
                        .providerId(jiraAccountId)
                        .email(jiraEmail)
                        .name(jiraName != null ? jiraName : "Jira User")
                        .avatarUrl(jiraAvatar)
                        .build();
                // Don't save yet - will save after adding OAuth details
            }
        }

        // 3. Fetch Accessible Resources (Cloud ID)
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauth2User.getName());

        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        Instant expiresAt = authorizedClient.getAccessToken().getExpiresAt();
        String refreshToken = authorizedClient.getRefreshToken() != null
                ? authorizedClient.getRefreshToken().getTokenValue()
                : null;

        String resourcesUrl = "https://api.atlassian.com/oauth/token/accessible-resources";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/json");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> resourcesResponse = restTemplate.exchange(
                resourcesUrl, HttpMethod.GET, entity, JsonNode.class);

        JsonNode resources = resourcesResponse.getBody();
        if (resources == null || !resources.isArray() || resources.isEmpty()) {
            throw new IllegalStateException("No accessible Jira resources found for user");
        }

        JsonNode site = resources.get(0); // Default to first available site
        String cloudId = site.get("id").asText();
        String baseUrl = site.get("url").asText();

        // 4. Create/Update JiraConnectionOAuth embedded object
        JiraConnectionOAuth jiraOAuth = JiraConnectionOAuth.builder()
                .jiraName(jiraName)
                .jiraAccountId(jiraAccountId)
                .jiraEmail(jiraEmail)
                .cloudId(cloudId)
                .baseUrl(baseUrl)
                .oauthAccessToken(accessToken)
                .oauthRefreshToken(refreshToken)
                .oauthAccessTokenExpiresAt(expiresAt)
                .build();

        // Set the embedded OAuth details to the user
        user.setJiraConnectionOAuth(jiraOAuth);

        // 5. Save the user (saves embedded OAuth details automatically)
        userRepository.save(user);

        // Store user ID in session for future reference
        request.getSession().setAttribute("user_id", user.getId());
        request.getSession().setAttribute("jira_connected", true);
    }

}
