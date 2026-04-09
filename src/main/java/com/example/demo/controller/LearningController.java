package com.example.demo.controller;

import com.example.demo.ChatMessage;
import com.example.demo.service.AnalyzeService;
import com.example.demo.repository.StudyRecordRepository;
import com.example.demo.domain.StudyRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
public class LearningController {

    private final AnalyzeService analyzeService;
    private final StudyRecordRepository studyRecordRepository;
    private final Map<String, List<ChatMessage>> chatSessions = new ConcurrentHashMap<>();

//    @PostMapping("/chat")
//    public ResponseEntity<String> chat(@RequestParam String sessionId, @RequestBody String userInput) {
//        List<ChatMessage> history = chatSessions.computeIfAbsent(sessionId, k -> new ArrayList<>());
//
//        history.add(new ChatMessage("user", userInput));
//        String aiResponse = analyzeService.analyzeWithHistory(history);
//        history.add(new ChatMessage("assistant", aiResponse));
//
//        return ResponseEntity.ok(aiResponse);
//    }

    @PostMapping("/summary")
    public ResponseEntity<String> getSummary(@RequestParam String sessionId, @RequestParam String topic) {
        List<ChatMessage> history = chatSessions.get(sessionId);

        if (history == null || history.isEmpty()) {
            return ResponseEntity.ok("대화 기록이 없습니다.");
        }

        String summary = analyzeService.summarizeConversation(history);

        studyRecordRepository.save(StudyRecord.builder()
                .sessionId(sessionId)
                .topic(topic)
                .summary(summary)
                .build());

        chatSessions.remove(sessionId);

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/history")
    public ResponseEntity<List<StudyRecord>> getHistory(@RequestParam String sessionId) {
        // DB에서 해당 세션 아이디의 학습 기록을 최신순으로 모두 가져옵니다.
        List<StudyRecord> history = studyRecordRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);

        // 프론트엔드로 데이터를 전달합니다. (JSON 형태로 자동 변환됨)
        return ResponseEntity.ok(history);
    }
}