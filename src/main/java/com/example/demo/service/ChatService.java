package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value; // 올바른 Import로 수정
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate = new RestTemplate();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    public void reportActivity(String sessionId, boolean isEmpty) {
        String activityKey = "activity:status:" + sessionId;
        String lastSeenKey = "activity:lastSeen:" + sessionId;
        long now = System.currentTimeMillis();

        // 1. 입력창 상태 저장
        redisTemplate.opsForValue().set(activityKey, String.valueOf(isEmpty), 10, TimeUnit.SECONDS);
        // 2. 마지막 활동 시간(Timestamp) 기록
        redisTemplate.opsForValue().set(lastSeenKey, String.valueOf(now), 10, TimeUnit.SECONDS);

        resetDebounceTimer(sessionId);
    }

    public void bufferMessage(String sessionId, String message) {
        redisTemplate.opsForList().rightPush("buffer:" + sessionId, message);
        redisTemplate.opsForValue().set("status:" + sessionId, "WAITING");
        resetDebounceTimer(sessionId);
    }

    public String getStoredResponse(String sessionId) {
        String statusKey = "status:" + sessionId;
        String responseKey = "response:" + sessionId;

        if ("READY".equals(redisTemplate.opsForValue().get(statusKey))) {
            String aiResponse = redisTemplate.opsForValue().get(responseKey);
            redisTemplate.delete(List.of(statusKey, responseKey));
            return aiResponse;
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

        // 검증 로직 A: 입력창에 글이 남아있다면 절대 응답하지 않음
        if ("false".equals(isEmptyStatus)) {
            log.info("사용자 입력창에 텍스트 잔존 - 응답 연기: {}", sessionId);
            resetDebounceTimer(sessionId);
            return;
        }

        // 검증 로직 B: 마지막 활동(Heartbeat) 이후 아직 3초가 지나지 않았다면 연기
        if (lastSeenStr != null) {
            long lastSeen = Long.parseLong(lastSeenStr);
            if (now - lastSeen < 2800) { // 3초에 근접한 시간차 계산
                log.info("최근 활동 감지 - 응답 연기: {}", sessionId);
                resetDebounceTimer(sessionId);
                return;
            }
        }

        // 모든 가드를 통과했을 때만 OpenAI 호출
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
            // 응답 생성 전 버퍼 선삭제 (중복 방지)
            redisTemplate.delete(bufferKey);

            String aiResponse = callOpenAiChat(combinedPrompt);

            redisTemplate.opsForValue().set("response:" + sessionId, aiResponse, 5, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set("status:" + sessionId, "READY");

            scheduledTasks.remove(sessionId);
            log.info("AI 응답 생성 완료: {}", sessionId);
        } catch (Exception e) {
            log.error("OpenAI 통신 중 에러: {}", e.getMessage());
            // 실패 시 사용자에게 알리기 위해 상태 업데이트
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
                // 주입받은 systemPrompt 사용
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
        return "응답 생성 실패";
    }
}