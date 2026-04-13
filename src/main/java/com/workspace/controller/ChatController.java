package com.workspace.controller;

import com.workspace.entity.Explanation;
import com.workspace.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat")
    public ResponseEntity<String> handleChat(@RequestParam("sessionId") String sessionId, @RequestBody String message) {
        chatService.processTextMessage(sessionId, message);
        return ResponseEntity.ok("ACK");
    }

    @PostMapping(value = "/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> handleAudio(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("audio") MultipartFile audioFile) {

        String transcript = chatService.processAudioMessage(sessionId, audioFile);

        // 💡 수정: transcript가 null이더라도 400 에러 대신 빈 문자열 반환
        if (transcript != null) {
            return ResponseEntity.ok(transcript);
        }
        return ResponseEntity.ok("");
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam("sessionId") String sessionId) {
        return chatService.subscribe(sessionId);
    }

    @PostMapping("/summary")
    public ResponseEntity<String> summarize(@RequestParam String sessionId, @RequestParam String topic) {
        String summary = chatService.generateSummary(sessionId, topic);
        return ResponseEntity.ok(summary);
    }

    @GetMapping(value = "/history", produces = "application/json;charset=UTF-8")
    public ResponseEntity<List<Explanation>> getHistory(@RequestParam("sessionId") String sessionId) {
        return ResponseEntity.ok(chatService.getHistory(sessionId));
    }
}