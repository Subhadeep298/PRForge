package com.example.be.service;

import com.example.be.dto.JiraIssueDetails;
import com.example.be.dto.JiraIssueDetailsResponse;
import com.example.be.dto.PRSuggestion;
import com.example.be.model.CompareResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);

    @Value("${groq.api-key:}")
    private String groqApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public LLMService() {
        this.webClient = WebClient.create("https://api.groq.com");
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate PR suggestion using ONLY GitHub code changes
     */
    public PRSuggestion generatePRSuggestion(CompareResult compareResult) {
        return generatePRSuggestion(compareResult, null);
    }

    /**
     * Generate PR suggestion using GitHub code changes + Jira ticket context
     */
    public PRSuggestion generatePRSuggestion(CompareResult compareResult, JiraIssueDetailsResponse jiraDetails) {
        try {
            logger.info("=== Starting Enhanced PR Suggestion Generation ===");
            logger.info("Repository: {}/{}", compareResult.getOwner(), compareResult.getRepo());
            logger.info("Branches: {} -> {}", compareResult.getBaseBranch(), compareResult.getHeadBranch());

            if (jiraDetails != null && jiraDetails.isSuccess() && jiraDetails.getJiraIssueDetails() != null) {
                logger.info("✅ Including Jira ticket context in prompt");
            } else {
                logger.info("ℹ️ No Jira context available - using code changes only");
            }

            // Build enhanced prompt with Jira context
            String prompt = buildEnhancedPrompt(compareResult, jiraDetails);

            // Create request body for Groq API
            String requestBody = String.format(
                    "{\"model\":\"llama-3.3-70b-versatile\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"temperature\":0.7,\"max_tokens\":1500}",
                    escapeJson(prompt));

            logger.info("Calling Groq API...");

            // Call Groq API
            String response = webClient.post()
                    .uri("/openai/v1/chat/completions")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + groqApiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response);
            String generatedText = jsonResponse
                    .path("choices").get(0)
                    .path("message")
                    .path("content").asText();

            PRSuggestion result = parseResponse(generatedText);
            logger.info("=== Enhanced PR Suggestion Generated Successfully ===");

            return result;

        } catch (Exception e) {
            logger.error("=== ERROR Generating PR Suggestion ===");
            logger.error("Error: {}", e.getMessage());

            return buildFallbackSuggestion(compareResult, jiraDetails, e);
        }
    }

    private String buildEnhancedPrompt(CompareResult compareResult, JiraIssueDetailsResponse jiraDetails) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert software engineer writing a pull request. Generate a professional PR title and description based on the following context.\\n\\n");

        // === JIRA TICKET CONTEXT (if available) ===
        if (jiraDetails != null && jiraDetails.isSuccess() && jiraDetails.getJiraIssueDetails() != null) {
            JiraIssueDetails ticket = jiraDetails.getJiraIssueDetails();

            prompt.append("## 🎫 Jira Ticket Context\\n");
            prompt.append("**Title:** ").append(ticket.getTitle()).append("\\n");

            if (ticket.getDescription() != null && !ticket.getDescription().isEmpty()) {
                String desc = ticket.getDescription();
                if (desc.length() > 800) {
                    desc = desc.substring(0, 800) + "...(truncated)";
                }
                prompt.append("**Description:** ").append(desc).append("\\n");
            }

            if (ticket.getAcceptanceCriteria() != null && !ticket.getAcceptanceCriteria().isEmpty()) {
                prompt.append("**Acceptance Criteria:**\\n");
                for (String criteria : ticket.getAcceptanceCriteria()) {
                    prompt.append("- ").append(criteria).append("\\n");
                }
            }
            prompt.append("\\n");
        }

        // === GITHUB CODE CHANGES ===
        prompt.append("## 📝 Repository Context\\n");
        prompt.append("Repository: ").append(compareResult.getOwner()).append("/").append(compareResult.getRepo()).append("\\n");
        prompt.append("Comparing: ").append(compareResult.getBaseBranch()).append(" → ").append(compareResult.getHeadBranch()).append("\\n");
        prompt.append("Files Changed: ").append(compareResult.getFilesChanged()).append("\\n");
        prompt.append("Stats: +").append(compareResult.getAdditions()).append(" -").append(compareResult.getDeletions()).append("\\n\\n");

        // Add code changes (truncated if too long)
        if (compareResult.getAddedCode() != null && !compareResult.getAddedCode().isEmpty()) {
            String addedCode = compareResult.getAddedCode();
            if (addedCode.length() > 2000) {
                addedCode = addedCode.substring(0, 2000) + "\\n...(truncated)";
            }
            prompt.append("## ✅ Added Code\\n``````\\n\\n");
        }

        if (compareResult.getDeletedCode() != null && !compareResult.getDeletedCode().isEmpty()) {
            String deletedCode = compareResult.getDeletedCode();
            if (deletedCode.length() > 2000) {
                deletedCode = deletedCode.substring(0, 2000) + "\\n...(truncated)";
            }
            prompt.append("## ❌ Deleted Code\\n``````\\n\\n");
        }

        // === INSTRUCTIONS ===
        prompt.append("## 📋 Instructions\\n");
        prompt.append("Generate a professional PR that connects the Jira ticket requirements with the actual code changes.\\n\\n");

        prompt.append("**TITLE (50-72 chars):**\\n");
        prompt.append("- Use imperative mood (Add/Fix/Update/Refactor/Implement)\\n");
        if (jiraDetails != null && jiraDetails.isSuccess()) {
            prompt.append("- Reference the Jira ticket's goal\\n");
        }
        prompt.append("- Be specific and concise\\n");
        prompt.append("- Example: 'Implement OAuth2 authentication for user login'\\n\\n");

        prompt.append("**DESCRIPTION:**\\n");
        prompt.append("1. Start with a one-sentence summary explaining WHY this change was made\\n");
        if (jiraDetails != null && jiraDetails.isSuccess()) {
            prompt.append("2. Mention how it fulfills the Jira ticket requirements\\n");
        }
        prompt.append("3. Add a 'Changes:' section with 3-6 bullet points\\n");
        prompt.append("   - Each bullet: one concise line describing WHAT was done\\n");
        prompt.append("   - Use past tense\\n");
        prompt.append("   - No bold/italic formatting\\n");
        prompt.append("   - Focus on implementation details from the code\\n");
        if (jiraDetails != null && jiraDetails.isSuccess() && jiraDetails.getJiraIssueDetails().getAcceptanceCriteria() != null) {
            prompt.append("4. Optional: Add 'Acceptance Criteria Met:' section if relevant\\n");
        }
        prompt.append("\\n");

        prompt.append("**Example Format:**\\n");
        prompt.append("TITLE: Implement user authentication with OAuth2\\n");
        prompt.append("DESCRIPTION:\\n");
        prompt.append("Added OAuth2 authentication to enable secure user login as required by JIRA-123.\\n\\n");
        prompt.append("Changes:\\n");
        prompt.append("- Created OAuth2SecurityConfig with GitHub provider\\n");
        prompt.append("- Added UserRepository for storing authenticated users\\n");
        prompt.append("- Implemented session management with JWT tokens\\n");
        prompt.append("- Added logout endpoint with token invalidation\\n");
        if (jiraDetails != null && jiraDetails.isSuccess()) {
            prompt.append("\\nAcceptance Criteria Met:\\n");
            prompt.append("- Users can log in via GitHub OAuth\\n");
            prompt.append("- Session persists across page reloads\\n");
        }
        prompt.append("\\n");

        prompt.append("## 🎯 Output\\n");
        prompt.append("Respond ONLY with:\\n");
        prompt.append("TITLE: <your title>\\n");
        prompt.append("DESCRIPTION:\\n");
        prompt.append("<your description>");

        return prompt.toString();
    }

    private PRSuggestion parseResponse(String response) {
        String title = "Update code";
        String description = "Code changes";

        try {
            String[] lines = response.split("\\n");
            StringBuilder descBuilder = new StringBuilder();
            boolean inDescription = false;

            for (String line : lines) {
                String trimmed = line.trim();

                if (trimmed.startsWith("TITLE:")) {
                    title = trimmed.substring(6).trim();
                    // Remove markdown formatting
                    title = title.replaceAll("[*_`#]", "").trim();

                } else if (trimmed.startsWith("DESCRIPTION:")) {
                    String inline = trimmed.substring(12).trim();
                    if (!inline.isEmpty()) {
                        descBuilder.append(inline).append("\\n");
                    }
                    inDescription = true;

                } else if (inDescription && !trimmed.isEmpty()) {
                    descBuilder.append(line).append("\\n");
                }
            }

            if (descBuilder.length() > 0) {
                description = descBuilder.toString().trim();
            }

        } catch (Exception e) {
            logger.error("Error parsing LLM response", e);
            description = response; // Fallback to raw response
        }

        return PRSuggestion.builder()
                .title(title)
                .description(description)
                .build();
    }

    private PRSuggestion buildFallbackSuggestion(CompareResult compareResult, JiraIssueDetailsResponse jiraDetails, Exception e) {
        StringBuilder fallbackDesc = new StringBuilder();

        // Add Jira context if available
        if (jiraDetails != null && jiraDetails.isSuccess() && jiraDetails.getJiraIssueDetails() != null) {
            JiraIssueDetails ticket = jiraDetails.getJiraIssueDetails();
            fallbackDesc.append("Related to: ").append(ticket.getTitle()).append("\\n\\n");
        }

        fallbackDesc.append("Changes between ")
                .append(compareResult.getBaseBranch())
                .append(" and ")
                .append(compareResult.getHeadBranch())
                .append("\\n\\n");

        fallbackDesc.append(compareResult.getFilesChanged()).append(" files changed, ")
                .append("+").append(compareResult.getAdditions()).append(" additions, ")
                .append("-").append(compareResult.getDeletions()).append(" deletions");

        // Add error context
        if (e.getMessage() != null) {
            if (e.getMessage().contains("429")) {
                fallbackDesc.insert(0, "⚠️ Groq API rate limit reached. Please wait and try again.\\n\\n");
            } else if (e.getMessage().contains("401")) {
                fallbackDesc.insert(0, "⚠️ Invalid Groq API key. Check GROQ_API_KEY environment variable.\\n\\n");
            } else {
                fallbackDesc.insert(0, "⚠️ Unable to generate AI suggestion. Using basic description.\\n\\n");
            }
        }

        String fallbackTitle = jiraDetails != null && jiraDetails.getJiraIssueDetails() != null
                ? "Update: " + jiraDetails.getJiraIssueDetails().getTitle()
                : "Update " + compareResult.getRepo();

        return PRSuggestion.builder()
                .title(fallbackTitle)
                .description(fallbackDesc.toString())
                .build();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
