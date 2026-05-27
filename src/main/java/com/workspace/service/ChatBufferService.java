package com.workspace.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ChatBufferService {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final ConcurrentHashMap<String, ScheduledFuture<?>> debounceTasks = new ConcurrentHashMap<>();

    private static final long DEBOUNCE_DELAY_MS = 3000;
    private static final String REDIS_BUFFER_KEY_PREFIX = "chat:buffer:";

    public ChatBufferService(StringRedisTemplate redisTemplate, SimpMessagingTemplate messagingTemplate) {
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
    }

    public void bufferMessage(String userEmail, String message) {
        String redisKey = REDIS_BUFFER_KEY_PREFIX + userEmail; // sessionId -> userEmail

        // 1. Redis List에 메시지 누적 (RPUSH)
        redisTemplate.opsForList().rightPush(redisKey, message);

        // 2. 기존 예약된 타이머가 있다면 취소 (Debouncing)
        ScheduledFuture<?> existingTask = debounceTasks.get(userEmail);
        if (existingTask != null) {
            existingTask.cancel(false);
        }

        // 3. 새로운 타이머 예약 (3초 후 실행)
        ScheduledFuture<?> newTask = scheduler.schedule(() -> processBufferedMessages(userEmail, redisKey),
                DEBOUNCE_DELAY_MS,
                TimeUnit.MILLISECONDS);
        debounceTasks.put(userEmail, newTask);
    }

    private void processBufferedMessages(String userEmail, String redisKey) {
        // 1. 버퍼링된 모든 메시지 조회
        List<String> messages = redisTemplate.opsForList().range(redisKey, 0, -1);

        if (messages != null && !messages.isEmpty()) {
            // 2. Redis에서 버퍼 비우기
            redisTemplate.delete(redisKey);
            debounceTasks.remove(userEmail);

            // 3. 메시지 병합 및 Gemini API 호출 로직
            String combinedPrompt = String.join("\n", messages);
            String aiResponse = callGeminiApi(combinedPrompt);

            // 4. 클라이언트로 최종 AI 응답 전송 (userEmail을 식별자로 사용)
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/reply", aiResponse);
        }
    }

    private String callGeminiApi(String prompt) {
        // Implementation for Gemini API call
        return "AI Response based on: " + prompt;
    }
}