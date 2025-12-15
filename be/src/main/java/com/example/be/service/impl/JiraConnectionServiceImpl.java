package com.example.be.service.impl;

import com.example.be.dto.JiraConnectionRequestDto;
import com.example.be.dto.JiraIssueDetails;
import com.example.be.dto.JiraIssueDetailsResponse;
import com.example.be.dto.ValidateResponse;
import com.example.be.exception.UserDoesNotExists;
import com.example.be.model.JiraConnection;
import com.example.be.model.User;
import com.example.be.repository.JiraConnectionRepo;
import com.example.be.service.JiraConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class JiraConnectionServiceImpl implements JiraConnectionService {

    private final JiraConnectionRepo jiraConnectionRepo;
    private final RestTemplate restTemplate;
    private final UserServiceImpl userService;

    @Override
    public ValidateResponse saveJiraConnection(JiraConnectionRequestDto jiraConnectionRequestDto) {
        log.info("Attempting to save Jira connection: {} for userId: {}",
                jiraConnectionRequestDto.getName(), jiraConnectionRequestDto.getUserId());

        boolean userExists = userService.validateUserByProviderId(jiraConnectionRequestDto.getUserId());
        if (!userExists) {
            log.warn("User with id {} does not exist", jiraConnectionRequestDto.getUserId());
            return new ValidateResponse(true, "User does not exist.");
        }

        JiraConnection connectionDoesExist = jiraConnectionRepo.findByUserIdAndName(
                jiraConnectionRequestDto.getUserId(), jiraConnectionRequestDto.getName());
        if (connectionDoesExist != null) {
            log.warn("Connection {} already exists for userId: {}",
                    jiraConnectionRequestDto.getName(), jiraConnectionRequestDto.getUserId());
            return new ValidateResponse(true, "Connection with this name already exists for the user.");
        }

        boolean isConnectionTested = testJiraConnection(jiraConnectionRequestDto);
        if (!isConnectionTested) {
            log.error("Connection test failed for {}", jiraConnectionRequestDto.getName());
            return new ValidateResponse(true, "Connection failed, Try again with correct credentials.");
        }

        JiraConnection jiraConnection = JiraConnection.builder()
                .id(UUID.randomUUID())
                .userId(jiraConnectionRequestDto.getUserId())
                .name(jiraConnectionRequestDto.getName())
                .username(jiraConnectionRequestDto.getUsername())
                .domainUrl(jiraConnectionRequestDto.getDomainUrl())
                .token(jiraConnectionRequestDto.getToken())
                .projectName(jiraConnectionRequestDto.getProjectName())
                .projectKey(jiraConnectionRequestDto.getProjectKey())
                // OAuth fields null for manual connections
                .jiraAccountId(null)
                .jiraEmail(null)
                .cloudId(null)
                .baseUrl(null)
                .oauthAccessToken(null)
                .oauthRefreshToken(null)
                .oauthAccessTokenExpiresAt(null)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        try {
            jiraConnectionRepo.save(jiraConnection);
            log.info("Connection {} saved successfully for userId: {}",
                    jiraConnectionRequestDto.getName(), jiraConnectionRequestDto.getUserId());
            return new ValidateResponse(false, "Connection tested and saved successfully.");
        } catch (Exception e) {
            log.error("Error saving connection {}: {}", jiraConnectionRequestDto.getName(), e.getMessage(), e);
            return new ValidateResponse(true, "Connection failed, Please try again.");
        }
    }

    public boolean testJiraConnection(JiraConnectionRequestDto jiraConnectionRequestDto) {
        log.info("Testing Jira connection for: {}", jiraConnectionRequestDto.getName());
        boolean keyAndNameValid = doesConnectionExistandNameMatch(
                jiraConnectionRequestDto.getDomainUrl(),
                jiraConnectionRequestDto.getProjectKey(),
                jiraConnectionRequestDto.getProjectName(),
                jiraConnectionRequestDto.getToken(),
                jiraConnectionRequestDto.getUsername());
        log.info("Connection test result for {}: {}", jiraConnectionRequestDto.getName(), keyAndNameValid);
        return keyAndNameValid;
    }

    public boolean doesConnectionExistandNameMatch(String domainurl, String projectKey,
            String expectedProjectName, String apiToken, String username) {
        String url = domainurl + "/rest/api/3/project/" + projectKey;
        try {
            HttpHeaders headers = createAuthHeader(username, apiToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String actualProjectName = response.getBody().get("name").toString();
                boolean match = actualProjectName != null && actualProjectName.equalsIgnoreCase(expectedProjectName);
                log.info("Project name validation: expected='{}', actual='{}', match={}",
                        expectedProjectName, actualProjectName, match);
                return match;
            }
            return false;
        } catch (Exception e) {
            log.error("Error validating project name: {}", e.getMessage(), e);
            return false;
        }
    }

    public HttpHeaders createAuthHeader(String username, String apiToken) {
        String auth = username + ":" + apiToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + encodedAuth);
        headers.add("Accept", "application/json");
        return headers;
    }

    // NEW: headers for OAuth connections
    private HttpHeaders createOauthAuthHeader(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/json");
        return headers;
    }

    @Override
    public Map<String, String> getAllTicketsByConnectionId(UUID connectionId) {
        log.info("Fetching all tickets for connectionId: {}", connectionId);
        JiraConnection connection = jiraConnectionRepo.findById(connectionId).orElseThrow();
        String jiraUrl = connection.getDomainUrl();

        String auth = connection.getUsername() + ":" + connection.getToken();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        String url = jiraUrl + "/rest/api/3/search/jql?jql={jql}&startAt={startAt}&maxResults={maxResults}&fields=*all";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("Accept", "application/json");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map<String, Object> uriVariables = new HashMap<>();
        if (connection.getProjectKey() != null && !connection.getProjectKey().isEmpty()) {
            uriVariables.put("jql", "project=" + connection.getProjectKey() + " ORDER BY created DESC");
        } else {
            uriVariables.put("jql", "ORDER BY created DESC");
        }
        uriVariables.put("startAt", 0);
        uriVariables.put("maxResults", 100);
        uriVariables.put("fields", "summary,description,status,assignee,reporter,priority,created,updated,comment");
        uriVariables.put("expand", "names,schema");

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                JsonNode.class,
                uriVariables);

        log.info("Tickets fetched successfully for connectionId: {}", connectionId);
        return extractTicketKeys(response.getBody());
    }

    public Map<String, String> extractTicketKeys(JsonNode response) {
        Map<String, String> idToKeyMap = new HashMap<>();
        JsonNode issues = response.get("issues");

        if (issues != null && issues.isArray()) {
            for (JsonNode issue : issues) {
                JsonNode keyNode = issue.get("key");
                JsonNode idNode = issue.get("id");

                if (keyNode != null && idNode != null) {
                    idToKeyMap.put(idNode.asText(), keyNode.asText());
                }
            }
        }

        log.info("Extracted {} ticket keys", idToKeyMap.size());
        return idToKeyMap;
    }

    @Override
    public JiraIssueDetailsResponse getTicketById(UUID connectionId, String ticketId) {
        log.info("Fetching ticket details for ticketId: {} on connectionId: {}", ticketId, connectionId);
        try {
            JiraConnection connection = jiraConnectionRepo.findById(connectionId)
                    .orElseThrow(() -> new RuntimeException("Connection not found"));

            String jiraUrl = connection.getDomainUrl();
            String auth = connection.getUsername() + ":" + connection.getToken();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            String url = jiraUrl + "/rest/api/3/issue/{ticketId}";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.set("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            Map<String, String> uriVariables = new HashMap<>();
            uriVariables.put("ticketId", ticketId);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class,
                    uriVariables);

            JiraIssueDetails issueDetails = extractIssueDetails(response.getBody());

            log.info("Ticket {} fetched successfully", ticketId);
            return JiraIssueDetailsResponse.builder()
                    .success(true)
                    .message("Ticket fetched successfully")
                    .jiraIssueDetails(issueDetails)
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch ticket {}: {}", ticketId, e.getMessage(), e);
            return JiraIssueDetailsResponse.builder()
                    .success(false)
                    .message("Failed to fetch ticket: " + e.getMessage())
                    .jiraIssueDetails(null)
                    .build();
        }
    }


    public JiraIssueDetailsResponse getTicketByIdUsingOAuth(UUID connectionId, String ticketKey) {
        log.info("Fetching ticket {} for connection {} using OAuth Jira connection", ticketKey, connectionId);

        // 1. Load connection and validate OAuth fields
        JiraConnection connection = jiraConnectionRepo.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("No OAuth Jira connection found for id: " + connectionId));

        if (connection.getOauthAccessToken() == null || connection.getCloudId() == null) {
            throw new RuntimeException("Missing OAuth token or cloudId for connection: " + connectionId);
        }

        String cloudId = connection.getCloudId();

        // 2. Build URL using api.atlassian.com + cloudId
        String url = "https://api.atlassian.com/ex/jira/"
                + cloudId
                + "/rest/api/3/issue/{ticketId}";

        // 3. OAuth Bearer header
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(connection.getOauthAccessToken());
        headers.set("Accept", "application/json");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 4. Path variable for issue key or id
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("ticketId", ticketKey);

        // 5. Call Jira
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                JsonNode.class,
                uriVariables
        );

        JiraIssueDetails issueDetails = extractIssueDetails(response.getBody());

        log.info("Ticket {} fetched successfully using OAuth", ticketKey);
        return JiraIssueDetailsResponse.builder()
                .success(true)
                .message("Ticket fetched successfully")
                .jiraIssueDetails(issueDetails)
                .build();
    }


    // NEW: OAuth version of "get all tickets"
    @Override
    public Map<String, String> getAllTicketsForCurrentUserOAuth(UUID connectionId) {
        log.info("Fetching all tickets for connectionId: {}", connectionId);

        // 1. Fetch connection & validate OAuth fields
        JiraConnection connection = jiraConnectionRepo.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found: " + connectionId));

        if (connection.getOauthAccessToken() == null || connection.getBaseUrl() == null) {
            throw new RuntimeException("Missing OAuth token or baseUrl");
        }

        // 2. Build JQL using your OAuth fields (same logic as your Basic Auth)
        String jql;
        if (connection.getProjectKey() != null && !connection.getProjectKey().isEmpty()) {
            jql = "project = " + connection.getProjectKey() + " ORDER BY created DESC";
        } else {
            // Safe bounded fallback – limit by current user or date
            jql = "reporter = currentUser() ORDER BY created DESC";
            // or e.g. "created >= -30d ORDER BY created DESC"
        }


        // 3. Build URL using baseUrl (your OAuth field)
        String url = "https://api.atlassian.com/ex/jira/"
                + connection.getCloudId()
                + "/rest/api/3/search/jql?jql={jql}&maxResults={maxResults}&fields={fields}";



        // 4. OAuth Bearer token (instead of Basic Auth)
        String auth = connection.getOauthAccessToken();  // Your OAuth field
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(auth);  // Bearer {oauthAccessToken}
        headers.set("Accept", "application/json");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 5. Same URI variables as your code
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("jql", jql);
        uriVariables.put("startAt", 0);
        uriVariables.put("maxResults", 100);
        uriVariables.put("fields", "summary,description,status,assignee,reporter,priority,created,updated,comment");

        // 6. Same REST call
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, JsonNode.class, uriVariables);

        log.info("Tickets fetched successfully for connectionId: {}", connectionId);
        return extractTicketKeys(response.getBody());
    }

    public String parseADF(JsonNode node) {
        if (node == null)
            return "";

        StringBuilder result = new StringBuilder();

        if (node.has("text")) {
            result.append(node.get("text").asText());
        }

        if (node.has("content") && node.get("content").isArray()) {
            for (JsonNode child : node.get("content")) {
                result.append(parseADF(child));

                if (child.has("type")) {
                    String type = child.get("type").asText();
                    if (type.equals("paragraph") || type.equals("hardBreak") || type.equals("listItem")) {
                        result.append("\n");
                    }
                }
            }
        }

        return result.toString().trim();
    }

    public JiraIssueDetails extractIssueDetails(JsonNode response) {
        JsonNode fields = response.get("fields");
        if (fields == null)
            return null;

        String title = fields.get("summary").asText();
        String descriptionText = parseADF(fields.get("description"));

        List<String> acceptanceCriteria = new ArrayList<>();
        String[] lines = descriptionText.split("\n");
        boolean isAC = false;
        for (String line : lines) {
            if (line.trim().equalsIgnoreCase("Acceptance Criteria:")) {
                isAC = true;
                continue;
            }
            if (isAC && !line.trim().isEmpty()) {
                acceptanceCriteria.add(line.trim());
            }
        }

        log.info("Extracted issue details for: {}", title);
        return new JiraIssueDetails(title, descriptionText, acceptanceCriteria);
    }

    @Override
    public Map<String, UUID> getAllConnectionsByUserid(String userId) {
        log.info("Fetching all Jira connections for userId: {}", userId);

        boolean userExists = userService.validateUserByProviderId(userId);
        if (!userExists) {
            throw new UserDoesNotExists("User with : " + userId + " does not exists.");
        }

        List<JiraConnection> connections = jiraConnectionRepo.findAllByUserId(userId);
        Map<String, UUID> connectionMap = new HashMap<>();

        for (JiraConnection connection : connections) {
            connectionMap.put(connection.getName(), connection.getId());
        }

        log.info("Found {} connections for userId: {}", connectionMap.size(), userId);
        return connectionMap;
    }
}
