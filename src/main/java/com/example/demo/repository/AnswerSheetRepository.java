package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.AnswerSheet;

public interface AnswerSheetRepository extends JpaRepository<AnswerSheet, String> {
    List<AnswerSheet> findByExamIdOrderByNameAsc(String examId);
}
