package com.example.demo.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.AnswerSheet;
import com.example.demo.entity.Exam;
import com.example.demo.entity.Question;
import com.example.demo.repository.ExamRepository;
import com.example.demo.service.ExamService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ExamController {
    private final ExamService examService;
    private final ExamRepository examRepo;

    @GetMapping("/start")
    public String start(@RequestParam String examid, Model model) {
        return examRepo.findById(examid).map(exam -> {
            model.addAttribute("examId", exam.getExamId());
            model.addAttribute("examName", exam.getExamName());
            model.addAttribute("questionData", exam.getQuestions().stream().map(Question::getQuestionData).toList());
            return "questionSheet";
        }).orElse("error");
    }

    @PostMapping("/result")
    public String result(@RequestParam String examId, @RequestParam String name, @RequestParam Map<String, String> params, Model model) {
        var userAnswers = params.entrySet().stream()
            .filter(e -> e.getKey().matches("\\d+"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var sheet = examService.processResult(examId, name, userAnswers);
        var exam = examRepo.findById(examId).orElseThrow();
        
        model.addAttribute("name", name);
        model.addAttribute("examName", exam.getExamName());
        model.addAttribute("totalScore", sheet.getTotalScore());
        model.addAttribute("perfectScore", exam.getQuestions().stream().mapToInt(Question::getScore).sum());
        model.addAttribute("answerData", buildResultView(sheet, exam));
        return "result";
    }

    @GetMapping("/jSKXK8hdcsdWMYSajmmS/calc")
    public String calc(@RequestParam String examid, Model model) {
        model.addAllAttributes(examService.getStatistics(examid));
        return "calc";
    }

    // JSPでのAnswer DTOの振る舞いを再現するためのヘルパー
    private List<Map<String, Object>> buildResultView(AnswerSheet sheet, Exam exam) {
        return sheet.getDetails().stream().map(d -> {
            var q = exam.getQuestions().stream().filter(q1 -> q1.getQuestionId() == d.getQuestionId()).findFirst().get();
            return Map.<String, Object>of(
                "questionId", d.getQuestionId(),
                "questionData", q.getQuestionData(),
                "answer", d.getAnswer(),
                "collectAnswer", q.getCorrectAnswer(),
                "score", q.getCorrectAnswer().equals(d.getAnswer()) ? q.getScore() : 0,
                "allocate", q.getScore()
            );
        }).toList();
    }
}