package com.example.be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraConnectionRequestDto {

    private String userId;

    private String name;

    private String username;

    private String domainUrl;

    private String token;

    private String projectName;

    private String projectKey;
}
