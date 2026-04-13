package com.example.demo.controller;

import com.example.demo.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

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
//
//    @GetMapping(value = "/response", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<String> getResponse(@RequestParam String sessionId) {
//        String jsonResponse = chatService.getStoredResponse(sessionId);
//
//        if (jsonResponse != null) {
//            return ResponseEntity.ok(jsonResponse);
//        }
//        return ResponseEntity.noContent().build();
//    }

    // ChatController.java에 추가
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam String sessionId) {
        return chatService.subscribe(sessionId);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        // 타임아웃을 충분히 줍니다 (예: 5분)
        SseEmitter emitter = new SseEmitter(300000L);

        // 초기 연결 시 더미 데이터를 하나 보내주는 것이 안정적입니다.
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }
}