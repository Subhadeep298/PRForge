package com.example.be.service;

import com.example.be.dto.PRSuggestion;
import com.example.be.model.CompareResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public PRSuggestion generatePRSuggestion(CompareResult compareResult) {
        try {
            logger.info("=== Starting PR Suggestion Generation (Groq AI) ===");
            logger.info("Repository: {}/{}", compareResult.getOwner(), compareResult.getRepo());
            logger.info("Branches: {} -> {}", compareResult.getBaseBranch(), compareResult.getHeadBranch());

            // Build the prompt
            String prompt = buildPrompt(compareResult);

            // Create request body for Groq API (OpenAI-compatible format)
            String requestBody = String.format(
                    "{\"model\":\"llama-3.3-70b-versatile\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"temperature\":0.7,\"max_tokens\":1000}",
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

            // Parse response (OpenAI format)
            JsonNode jsonResponse = objectMapper.readTree(response);
            String generatedText = jsonResponse
                    .path("choices").get(0)
                    .path("message")
                    .path("content").asText();

            PRSuggestion result = parseResponse(generatedText);
            logger.info("=== PR Suggestion Generated Successfully ===");

            return result;

        } catch (Exception e) {
            logger.error("=== ERROR Generating PR Suggestion ===");
            logger.error("Error: {}", e.getMessage());

            String fallbackDescription = "Changes between " + compareResult.getBaseBranch() +
                    " and " + compareResult.getHeadBranch() +
                    "\\n\\n" + compareResult.getFilesChanged() + " files changed, " +
                    "+" + compareResult.getAdditions() + " additions, " +
                    "-" + compareResult.getDeletions() + " deletions";

            // Check if it's a rate limit error
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                fallbackDescription = "⚠️ Groq API rate limit reached. Please wait a moment and try again.\\n\\n"
                        + fallbackDescription;
            } else if (e.getMessage() != null && e.getMessage().contains("401")) {
                fallbackDescription = "⚠️ Invalid Groq API key. Please check your GROQ_API_KEY environment variable.\\n\\n"
                        + fallbackDescription;
            } else {
                fallbackDescription = "⚠️ Unable to generate AI suggestion. Using basic description.\\n\\n"
                        + fallbackDescription;
            }

            // Fallback to basic suggestion if LLM fails
            return PRSuggestion.builder()
                    .title("Update " + compareResult.getRepo())
                    .description(fallbackDescription)
                    .build();
        }
    }

    private String buildPrompt(CompareResult compareResult) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(
                "You are an expert software engineer writing a pull request. Generate a professional PR title and description.\\n\\n");

        prompt.append("## Repository Context\\n");
        prompt.append("Repository: ").append(compareResult.getOwner()).append("/").append(compareResult.getRepo())
                .append("\\n");
        prompt.append("Comparing: ").append(compareResult.getBaseBranch()).append(" → ")
                .append(compareResult.getHeadBranch()).append("\\n");
        prompt.append("Files Changed: ").append(compareResult.getFilesChanged()).append("\\n\\n");

        // Add code changes
        if (compareResult.getAddedCode() != null && !compareResult.getAddedCode().isEmpty()) {
            String addedCode = compareResult.getAddedCode();
            if (addedCode.length() > 3000) {
                addedCode = addedCode.substring(0, 3000) + "\\n...(truncated)";
            }
            prompt.append("## Added Code\\n```\\n").append(addedCode).append("\\n```\\n\\n");
        }

        if (compareResult.getDeletedCode() != null && !compareResult.getDeletedCode().isEmpty()) {
            String deletedCode = compareResult.getDeletedCode();
            if (deletedCode.length() > 3000) {
                deletedCode = deletedCode.substring(0, 3000) + "\\n...(truncated)";
            }
            prompt.append("## Deleted Code\\n```\\n").append(deletedCode).append("\\n```\\n\\n");
        }

        // Instructions
        prompt.append("## Instructions\\n");
        prompt.append("Generate a professional PR following these guidelines:\\n\\n");

        prompt.append("**TITLE:**\\n");
        prompt.append("- Use imperative mood (Add/Fix/Update/Refactor/Implement)\\n");
        prompt.append("- Be specific and concise (50-72 characters)\\n");
        prompt.append("- Focus on the main change\\n\\n");

        prompt.append("**DESCRIPTION:**\\n");
        prompt.append("- Start with a one-sentence summary\\n");
        prompt.append("- Add a 'Changes:' section with bullet points\\n");
        prompt.append("- Each bullet should be one concise line\\n");
        prompt.append("- Use past tense\\n");
        prompt.append("- 3-6 bullets maximum\\n");
        prompt.append("- No bold/italic in bullets\\n");
        prompt.append("- Focus on WHAT changed, not HOW\\n\\n");

        prompt.append("**Example Format:**\\n");
        prompt.append("TITLE: Add user authentication\\n");
        prompt.append("DESCRIPTION:\\n");
        prompt.append("Implemented OAuth2 authentication for secure user access.\\n\\n");
        prompt.append("Changes:\\n");
        prompt.append("- Added OAuth2 configuration\\n");
        prompt.append("- Created user session management\\n");
        prompt.append("- Integrated GitHub provider\\n\\n");

        prompt.append("## Output\\n");
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
                if (line.startsWith("TITLE:")) {
                    title = line.substring(6).trim();
                    title = title.replaceAll("[*_`]", "");
                } else if (line.startsWith("DESCRIPTION:")) {
                    description = line.substring(12).trim();
                    inDescription = true;
                } else if (inDescription) {
                    descBuilder.append(line).append("\\n");
                }
            }

            if (descBuilder.length() > 0) {
                description = descBuilder.toString().trim();
            }

        } catch (Exception e) {
            logger.error("Error parsing response", e);
            description = response;
        }

        return PRSuggestion.builder()
                .title(title)
                .description(description)
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
