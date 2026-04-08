package com.example.demo.controller;

import com.example.demo.dto.ChatMessage;
import com.example.demo.service.AnalyzeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
public class LearningController {

    private final AnalyzeService analyzeService;

    // 세션별 대화 이력을 저장하기 위한 메모리 맵 (ConcurrentHashMap 사용으로 스레드 안전성 확보)
    private final Map<String, List<ChatMessage>> chatSessions = new ConcurrentHashMap<>();

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam String sessionId, @RequestBody String userInput) {
        // 1. 해당 세션의 이전 대화 이력을 가져오거나 새로 생성
        List<ChatMessage> history = chatSessions.computeIfAbsent(sessionId, k -> new ArrayList<>());

        // 2. 현재 사용자의 입력을 대화 이력에 추가
        history.add(new ChatMessage("user", userInput));

        // 3. 누적된 대화 이력을 바탕으로 AI 분석 요청
        String aiResponse = analyzeService.analyzeWithHistory(history);

        // 4. AI의 답변을 대화 이력에 추가하여 다음 턴의 문맥 유지
        history.add(new ChatMessage("assistant", aiResponse));

        return ResponseEntity.ok(aiResponse);
    }

    @PostMapping("/summary")
    public ResponseEntity<String> getSummary(@RequestParam String sessionId) {
        List<ChatMessage> history = chatSessions.get(sessionId);

        if (history == null || history.isEmpty()) {
            return ResponseEntity.ok("아직 나눈 대화가 없는걸? 🤔");
        }

        String summary = analyzeService.summarizeConversation(history);

        // 요약이 끝났으므로 세션에서 대화 기록 삭제 (메모리 관리)
        chatSessions.remove(sessionId);

        return ResponseEntity.ok(summary);
    }
}