package com.example.demo.controller;

import com.example.demo.DTO.StudyRequest;
import com.example.demo.service.SttService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
public class LearningController {

    private final SttService sttService;
    //SttService sttService;

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

        String resultText = sttService.processSpeech(file);
        return ResponseEntity.ok("STT 변환 및 저장 완료: " + resultText);
    }
}