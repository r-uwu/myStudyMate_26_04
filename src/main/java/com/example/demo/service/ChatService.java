package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.prompts.feynman-analyzer}")
    private String systemPrompt;

    private final StringRedisTemplate redisTemplate;
    private final SttService sttService;
    private final TtsService ttsService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    public void reportActivity(String sessionId, boolean isEmpty) {
        String activityKey = "activity:status:" + sessionId;
        String lastSeenKey = "activity:lastSeen:" + sessionId;
        long now = System.currentTimeMillis();

        redisTemplate.opsForValue().set(activityKey, String.valueOf(isEmpty), 10, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(lastSeenKey, String.valueOf(now), 10, TimeUnit.SECONDS);

        resetDebounceTimer(sessionId);
    }

    public void bufferMessage(String sessionId, String message) {
        String bufferKey = "buffer:" + sessionId;
        String statusKey = "status:" + sessionId;

        redisTemplate.opsForList().rightPush(bufferKey, message);
        redisTemplate.opsForValue().set(statusKey, "WAITING");

        resetDebounceTimer(sessionId);
    }

    public String processAudioMessage(String sessionId, MultipartFile audioFile) {
        String transcript = sttService.processSpeech(audioFile);

        if (transcript != null && !transcript.contains("에러") && !transcript.contains("실패")) {
            bufferMessage(sessionId, transcript);
            return transcript;
        }
        return null;
    }

    public String getStoredResponse(String sessionId) {
        String statusKey = "status:" + sessionId;
        String responseKey = "response:" + sessionId;

        if ("READY".equals(redisTemplate.opsForValue().get(statusKey))) {
            String jsonResponse = redisTemplate.opsForValue().get(responseKey);
            redisTemplate.delete(List.of(statusKey, responseKey));
            return jsonResponse;
        }
        return null;
    }

    private void resetDebounceTimer(String sessionId) {
        if (scheduledTasks.containsKey(sessionId)) {
            scheduledTasks.get(sessionId).cancel(false);
        }

        ScheduledFuture<?> future = scheduler.schedule(() -> processAndGenerateResponse(sessionId), 3, TimeUnit.SECONDS);
        scheduledTasks.put(sessionId, future);
    }

    private void processAndGenerateResponse(String sessionId) {
        String activityKey = "activity:status:" + sessionId;
        String lastSeenKey = "activity:lastSeen:" + sessionId;

        String isEmptyStatus = redisTemplate.opsForValue().get(activityKey);
        String lastSeenStr = redisTemplate.opsForValue().get(lastSeenKey);
        long now = System.currentTimeMillis();

        if ("false".equals(isEmptyStatus)) {
            log.info("Status check: User is still typing. Session: {}", sessionId);
            resetDebounceTimer(sessionId);
            return;
        }

        if (lastSeenStr != null) {
            long lastSeen = Long.parseLong(lastSeenStr);
            if (now - lastSeen < 2800) {
                log.info("Status check: Recent activity detected. Session: {}", sessionId);
                resetDebounceTimer(sessionId);
                return;
            }
        }

        executeAiResponse(sessionId);
    }

    private void executeAiResponse(String sessionId) {
        String bufferKey = "buffer:" + sessionId;
        List<String> messages = redisTemplate.opsForList().range(bufferKey, 0, -1);

        if (messages == null || messages.isEmpty()) {
            scheduledTasks.remove(sessionId);
            return;
        }

        try {
            String combinedPrompt = String.join("\n", messages);
            redisTemplate.delete(bufferKey);

            String aiResponseText = callOpenAiChat(combinedPrompt);
            byte[] audioBytes = ttsService.generateSpeech(aiResponseText);
            String audioBase64 = "";

            if (audioBytes != null) {
                audioBase64 = Base64.getEncoder().encodeToString(audioBytes);
            }

            Map<String, String> responseData = new HashMap<>();
            responseData.put("text", aiResponseText);
            responseData.put("audio", audioBase64);

            String jsonResult = objectMapper.writeValueAsString(responseData);

            redisTemplate.opsForValue().set("response:" + sessionId, jsonResult, 5, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set("status:" + sessionId, "READY");

            scheduledTasks.remove(sessionId);
            log.info("AI response and TTS generated successfully for session: {}", sessionId);

        } catch (Exception e) {
            log.error("Error during AI response generation: {}", e.getMessage());
            redisTemplate.opsForValue().set("status:" + sessionId, "ERROR");
        }
    }

    private String callOpenAiChat(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o");
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", prompt)
        ));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        if (response.getBody() != null && response.getBody().containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        }

        return "응답을 생성할 수 없습니다.";
    }
}