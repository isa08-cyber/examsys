package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.example.demo.entity.AnswerDetail;
import com.example.demo.entity.AnswerSheet;
import com.example.demo.entity.Question;
import com.example.demo.repository.AnswerSheetRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.QuestionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ExamService {
    private final ExamRepository examRepo;
    private final QuestionRepository questionRepo;
    private final AnswerSheetRepository sheetRepo;

    public AnswerSheet processResult(String examId, String name, Map<String, String> answers) {
        var questions = questionRepo.findByExamExamIdOrderByQuestionIdAsc(examId);
        
        var sheet = new AnswerSheet();
        sheet.setAnswerId(generateId(examId));
        sheet.setDate(LocalDateTime.now());
        sheet.setExamId(examId);
        sheet.setName(name);

        int total = 0;
        for (var q : questions) {
            String userAnswer = answers.get(String.valueOf(q.getQuestionId()));
            var detail = new AnswerDetail();
            detail.setQuestionId(q.getQuestionId());
            detail.setAnswer(userAnswer);
            detail.setAnswerSheet(sheet);
            sheet.getDetails().add(detail);
            
            if (q.getCorrectAnswer().equals(userAnswer)) total += q.getScore();
        }
        sheet.setTotalScore(total);
        return sheetRepo.save(sheet);
    }

    public Map<String, Object> getStatistics(String examId) {
        var exam = examRepo.findById(examId).orElseThrow();
        var sheets = sheetRepo.findByExamIdOrderByNameAsc(examId);
        var questions = exam.getQuestions();

        double totalSum = sheets.stream().mapToInt(AnswerSheet::getTotalScore).sum();
        double[] qAves = new double[questions.size()];
        
        for (int i = 0; i < questions.size(); i++) {
            int qId = questions.get(i).getQuestionId();
            String correct = questions.get(i).getCorrectAnswer();
            long correctCount = sheets.stream()
                .flatMap(s -> s.getDetails().stream())
                .filter(d -> d.getQuestionId() == qId && correct.equals(d.getAnswer()))
                .count();
            qAves[i] = Math.round((double)correctCount / sheets.size() * 100.0) / 100.0;
        }

        return Map.of(
            "examName", exam.getExamName(),
            "collectAnswerList", questions.stream().map(Question::getCorrectAnswer).toList(),
            "list", sheets,
            "totalAve", sheets.isEmpty() ? 0 : Math.round(totalSum/sheets.size()*10)/10.0,
            "totalAvePerQuestion", qAves
        );
    }

    private String generateId(String examId) {
        return examId + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + new Random().nextInt(10);
    }
}
