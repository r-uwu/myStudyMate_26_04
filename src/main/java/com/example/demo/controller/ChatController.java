package com.example.demo.controller;

import com.example.demo.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/active")
    public void reportActivity(@RequestParam String sessionId, @RequestParam boolean isEmpty) {
        chatService.reportActivity(sessionId, isEmpty);
    }

    @PostMapping("/chat")
    public String handleChat(@RequestParam String sessionId, @RequestBody String message) {
        chatService.bufferMessage(sessionId, message);
        return "ACK";
    }

    // 💡 음성 처리를 위한 엔드포인트
    @PostMapping(value = "/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> handleAudio(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("audio") MultipartFile audioFile) {

        String transcript = chatService.processAudioMessage(sessionId, audioFile);
        return (transcript != null) ? ResponseEntity.ok(transcript) : ResponseEntity.badRequest().body("STT 실패");
    }

    @GetMapping(value = "/response", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getResponse(@RequestParam String sessionId) {
        String jsonResponse = chatService.getStoredResponse(sessionId);

        if (jsonResponse != null) {
            return ResponseEntity.ok(jsonResponse);
        }
        return ResponseEntity.noContent().build();
    }
}