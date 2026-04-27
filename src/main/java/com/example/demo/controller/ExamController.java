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

/**
 * オンライン試験システムの主要な画面遷移を制御するコントローラーです。
 * <p>
 * 試験問題の表示、回答の受け取り、採点結果の表示、および管理者向けの集計表示を担当します。
 * </p>
 */
@Controller
@RequiredArgsConstructor
public class ExamController {
    private final ExamService examService;
    private final ExamRepository examRepo;

    /**
     * 指定された試験IDに基づき、試験問題画面を表示します。
     * <p>
     * Azure SQL Databaseから試験に紐づく設問リストをロードし、
     * Thymeleafテンプレートにデータを渡します。BLOBデータの読み込みを含むため、
     * フロントエンド側でローディング表示を行うことが推奨されます。
     * </p>
     *
     * @param examid 試験を一意に識別するID（URLパラメータ）
     * @param model  画面に渡すデータを格納するオブジェクト
     * @return 試験問題画面（questionSheet.html）のパス。試験が見つからない場合はエラー画面へ遷移。
     */
    @GetMapping("/start")
    public String start(@RequestParam String examid, Model model) {

        // 指定された試験IDでDBを検索し、試験情報と設問リストをモデルに格納して出題画面へ遷移させる
        // 試験が存在しない場合は、一貫性を保つためエラー画面のパスを返す
        return examRepo.findById(examid).map(exam -> {
            model.addAttribute("examId", exam.getExamId());
            model.addAttribute("examName", exam.getExamName());
            model.addAttribute("questionData", exam.getQuestions().stream().map(Question::getQuestionData).toList());
            return "questionSheet";
        }).orElse("error");
    }

    /**
     * 送信された試験回答を処理し、採点結果画面を表示します。
     * <p>
     * フォームから送信された動的なパラメータ（設問IDがキー、回答内容が値）を抽出し、
     * {@link ExamService} を介して採点およびDBへの永続化を行います。
     * 画面表示用に、受験者名、得点、満点、および詳細な正誤情報をモデルに格納します。
     * </p>
     *
     * @param examId 対象の試験ID
     * @param name   受験者の氏名
     * @param params 設問IDと回答を含むリクエストパラメータのマップ
     * @param model  画面表示用データの格納オブジェクト
     * @return 採点結果画面（result.html）
     */
    @PostMapping("/result")
    public String result(@RequestParam String examId, @RequestParam String name, @RequestParam Map<String, String> params, Model model) {
        
        // リクエストパラメータから設問ID（数値）をキーに持つエントリーのみを抽出し、回答マップを作成
        var userAnswers = params.entrySet().stream()
            .filter(e -> e.getKey().matches("\\d+"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // 回答データを処理し、採点結果の生成とデータベースへの永続化を実行
        var sheet = examService.processResult(examId, name, userAnswers);
        // 試験マスタ情報を再取得し、存在しない場合は例外をスロー（基本的には存在する想定）
        var exam = examRepo.findById(examId).orElseThrow();
        
        // 表示用データのセット：受験者名、試験名、獲得点数、満点をモデルに追加
        model.addAttribute("name", name);
        model.addAttribute("examName", exam.getExamName());
        model.addAttribute("totalScore", sheet.getTotalScore());
        model.addAttribute("perfectScore", exam.getQuestions().stream().mapToInt(Question::getScore).sum());
        
        // 保存された回答詳細と試験マスタを照合し、正誤結果を含むビュー用データを構築
        model.addAttribute("answerData", buildResultView(sheet, exam));
        return "result";
    }

    /**
     * 試験の統計情報を表示する。
     * <p>
     * 管理者専用のパスを通じて、指定された試験の全受験者の平均点や得点分布などの
     * 統計情報を取得し、表示します。
     * </p>
     * @param examid 統計対象の試験ID
     * @param model  統計データを格納するモデル
     * @return 統計情報表示画面（calc.html）
     */
    @GetMapping("/jSKXK8hdcsdWMYSajmmS/calc")
    public String calc(@RequestParam String examid, Model model) {

        // Service層で計算された統計情報（平均点、得点分布、受験者リスト等）のマップを全てモデルに追加
        model.addAllAttributes(examService.getStatistics(examid));
        return "calc";
    }

    /**
     * 結果画面表示用のデータを生成するヘルパーメソッド。
     * <p>
     * 保存された回答シート（{@link AnswerSheet}）と試験マスタ（{@link Exam}）を突合し、
     * 設問文、ユーザー回答、正解、配点、獲得点数をセットにしたビューモデルを作成します。
     * </p>
     * @param sheet 永続化された採点済み結果シート
     * @param exam  試験構成情報
     * @return 設問ごとの正誤結果を含む詳細データリスト
     */
    private List<Map<String, Object>> buildResultView(AnswerSheet sheet, Exam exam) {
        
        // 回答詳細(Details)をループし、各回答に対応する設問情報をマスタ(Exam)から結合して新しいMapを生成する
        return sheet.getDetails().stream().map(d -> {
            // 現在の回答詳細の設問IDと一致する設問を、試験マスタの設問リストから検索して取得
            var q = exam.getQuestions().stream().filter(q1 -> q1.getQuestionId() == d.getQuestionId()).findFirst().get();
            
            // 画面表示に必要な要素（設問、回答、正解、配点、得点）をMap形式で構築
            return Map.<String, Object>of(
                "questionId", d.getQuestionId(),
                "questionData", q.getQuestionData(),
                "answer", d.getAnswer(),
                "collectAnswer", q.getCorrectAnswer(),
                // 三項演算子を用いて、回答が正解と一致する場合のみ配点を与え、それ以外は0点とする
                "score", q.getCorrectAnswer().equals(d.getAnswer()) ? q.getScore() : 0,
                "allocate", q.getScore()
            );
        }).toList(); // 全ての回答詳細を変換後、リストにまとめて返却
    }
}