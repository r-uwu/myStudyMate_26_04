package com.example.demo.controller;

import com.example.demo.DTO.StudyRequest;
import com.example.demo.service.AnalyzeService;
import com.example.demo.service.SttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
@Slf4j
public class LearningController {

    private final SttService sttService;
    private final AnalyzeService analyzeService;

    @PostMapping("/start")
    //@GetMapping("/start")
    public ResponseEntity<String> startSession(@RequestBody StudyRequest request) {

        return ResponseEntity.ok(request.topic() + " 학습 세션을 시작합니다.");
    }

    @PostMapping("/explain")
    public ResponseEntity<String> uploadSpeech(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("파일이 비어있습니다.");
        }

        //stt
        String transcript = sttService.processSpeech(file);
        log.info("STT Result: {}", transcript);

        //analysis
        String feedback = analyzeService.analyzeExplanation(transcript);
        log.info("Analysis Feedback: {}", feedback);

        Map<String, String> response = new HashMap<>();
        response.put("transcript", transcript);
        response.put("feedback", feedback);

        return ResponseEntity.ok("STT 변환 및 저장 완료: " + response);
    }
}