package com.example.demo.controller;

import com.example.demo.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/active")
    public void reportActivity(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "true") boolean isEmpty // 파라미터 누락 시 true로 처리
    ) {
        chatService.reportActivity(sessionId, isEmpty);
    }

    @PostMapping("/chat")
    public String handleChat(@RequestParam String sessionId, @RequestBody String message) {
        chatService.bufferMessage(sessionId, message);
        return "ACK";
    }

    @GetMapping("/response")
    public ResponseEntity<String> getResponse(@RequestParam String sessionId) {
        String response = chatService.getStoredResponse(sessionId);
        return (response != null) ? ResponseEntity.ok(response) : ResponseEntity.noContent().build();
    }
}