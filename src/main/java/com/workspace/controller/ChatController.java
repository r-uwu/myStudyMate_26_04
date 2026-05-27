package com.workspace.controller;

import com.workspace.entity.Explanation;
import com.workspace.security.CustomUserDetails;
import com.workspace.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<String> handleChat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody String message) {

        chatService.processTextMessage(userDetails.getUsername(), message);
        return ResponseEntity.ok("ACK");
    }

    @PostMapping(value = "/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> handleAudio(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("audio") MultipartFile audioFile) {

        String transcript = chatService.processAudioMessage(userDetails.getUsername(), audioFile);

        if (transcript != null) {
            return ResponseEntity.ok(transcript);
        }
        return ResponseEntity.ok("");
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // 프론트엔드에서 쿼리 파라미터(?token=...)로 보낸 JWT를 필터가 가로채어 userDetails를 주입합니다.
        return chatService.subscribe(userDetails.getUsername());
    }

    @PostMapping("/summary")
    public ResponseEntity<String> summarize(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String topic) {
        
        String summary = chatService.generateSummary(userDetails.getUsername(), topic);
        return ResponseEntity.ok(summary);
    }

    @GetMapping(value = "/history", produces = "application/json;charset=UTF-8")
    public ResponseEntity<List<Explanation>> getHistory(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatService.getHistory(userDetails.getUsername()));
    }
}