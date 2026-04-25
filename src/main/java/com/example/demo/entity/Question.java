
package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity 
@Table(name = "t_question") 
@Data
public class Question {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 上記SQLで追加した新しい主キー

    @Column(name = "question_id") // DBの列名と正確に合わせる
    private int questionId;

    @Column(name = "question_data")
    private String questionData;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "score")
    private int score;

    @ManyToOne 
    @JoinColumn(name = "exam_id", referencedColumnName = "exam_id") 
    private Exam exam;
}
