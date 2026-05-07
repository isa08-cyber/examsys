package com.example.demo.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Id;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Data;

@Entity 
@Table(name = "t_answer") 
@Data
public class AnswerSheet {
    @Id 
    @Column(name = "answer_id")
    private String answerId;

    @Column(name = "date")
    private LocalDateTime date;

    @Column(name = "exam_id")
    private String examId;

    @Column(name = "name")
    private String name;

    @Column(name = "total_score")
    private int totalScore;

    @OneToMany(mappedBy = "answerSheet", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
@OrderBy("questionId ASC")
    private List<AnswerDetail> details = new ArrayList<>();
}