package com.example.be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "compare_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompareResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String owner;
    private String repo;
    private String baseBranch;
    private String headBranch;

    @Column(columnDefinition = "TEXT")
    private String patches;

    @Column(columnDefinition = "TEXT")
    private String deletedCode;

    @Column(columnDefinition = "TEXT")
    private String addedCode;

    private Integer filesChanged;
    private Integer additions;
    private Integer deletions;

    private LocalDateTime createdAt;

    private Long userId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
