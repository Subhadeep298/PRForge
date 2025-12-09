package com.example.be.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GitHubService {

    @Autowired
    private WebClient webClient;

    @SuppressWarnings("unchecked")
    public Map<String, Object> compareCommits(String owner, String repo, String baseBranch, String headBranch,
            String token) {
        String basehead = baseBranch + "..." + headBranch;

        try {
            Map<String, Object> response = webClient.get()
                    .uri("/repos/{owner}/{repo}/compare/{basehead}", owner, repo, basehead)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            Map<String, Object> result = new HashMap<>();

            if (response != null) {
                // Extract statistics
                List<Map<String, Object>> files = (List<Map<String, Object>>) response.get("files");
                result.put("filesChanged", files != null ? files.size() : 0);
                result.put("additions", response.get("total_additions") != null ? response.get("total_additions") : 0);
                result.put("deletions", response.get("total_deletions") != null ? response.get("total_deletions") : 0);

                // Process files - extract only filename, status, and patch
                if (files != null) {
                    List<Map<String, Object>> simplifiedFiles = new java.util.ArrayList<>();
                    StringBuilder deletedCode = new StringBuilder();
                    StringBuilder addedCode = new StringBuilder();

                    for (Map<String, Object> file : files) {
                        // Create simplified file object with only needed fields
                        Map<String, Object> simplifiedFile = new HashMap<>();
                        simplifiedFile.put("filename", file.get("filename"));
                        simplifiedFile.put("status", file.get("status"));
                        simplifiedFile.put("patch", file.get("patch"));
                        simplifiedFiles.add(simplifiedFile);

                        // Parse patch to extract deleted and added lines
                        String patch = (String) file.get("patch");
                        if (patch != null) {
                            String filename = (String) file.get("filename");
                            String[] lines = patch.split("\\n");

                            for (String line : lines) {
                                if (line.startsWith("-") && !line.startsWith("---")) {
                                    // Deleted line
                                    deletedCode.append(filename).append(": ").append(line.substring(1)).append("\\n");
                                } else if (line.startsWith("+") && !line.startsWith("+++")) {
                                    // Added line
                                    addedCode.append(filename).append(": ").append(line.substring(1)).append("\\n");
                                }
                            }
                        }
                    }

                    // Store simplified patches as JSON
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        result.put("patches", mapper.writeValueAsString(simplifiedFiles));
                        result.put("deletedCode", deletedCode.toString());
                        result.put("addedCode", addedCode.toString());
                    } catch (Exception e) {
                        result.put("patches", "[]");
                        result.put("deletedCode", "");
                        result.put("addedCode", "");
                    }
                } else {
                    result.put("patches", "[]");
                    result.put("deletedCode", "");
                    result.put("addedCode", "");
                }

                result.put("success", true);
            } else {
                result.put("success", false);
                result.put("error", "No response from GitHub API");
            }

            return result;
        } catch (WebClientResponseException e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getStatusCode() + " " + e.getStatusText() + " from " + e.getRequest().getURI());
            return errorResult;
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }
}
