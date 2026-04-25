package com.example.demo.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Data;

@Entity 
@Table(name = "t_exam") 
@Data
public class Exam {
    @Id 
    @Column(name = "exam_id") // DBの列名と一致させる
    private String examId;

    @Column(name = "exam_name")
    private String examName;

    // fetch = FetchType.EAGER を追加して、Exam取得時にQuestionも即座に読み込む
    @OneToMany(mappedBy = "exam", fetch = FetchType.EAGER)
    @OrderBy("questionId ASC")
    private List<Question> questions;
}


