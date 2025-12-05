package com.example.be.repository;

import com.example.be.model.CompareResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompareRepository extends JpaRepository<CompareResult, Long> {
    List<CompareResult> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<CompareResult> findByOwnerAndRepoOrderByCreatedAtDesc(String owner, String repo);
}
