package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    // 💡 1. 사용자별 SSE 연결을 저장하는 맵
    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // 💡 2. 프론트엔드가 SSE 연결을 요청할 때 호출하는 메서드
    public SseEmitter subscribe(String sessionId) {
        SseEmitter emitter = new SseEmitter(120000L); // 2분 타임아웃
        emitters.put(sessionId, emitter);

        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> emitters.remove(sessionId));
        emitter.onError((e) -> emitters.remove(sessionId));

        return emitter;
    }

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
        redisTemplate.opsForList().rightPush(bufferKey, message);
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
            resetDebounceTimer(sessionId);
            return;
        }

        if (lastSeenStr != null) {
            long lastSeen = Long.parseLong(lastSeenStr);
            if (now - lastSeen < 2800) {
                resetDebounceTimer(sessionId);
                return;
            }
        }

        // 💡 3. 실행 함수 호출
        executeAiResponseStreaming(sessionId);
    }

    // 💡 4. 완전히 새로 작성된 스트리밍 + 병렬 TTS 로직
    private void executeAiResponseStreaming(String sessionId) {
        String bufferKey = "buffer:" + sessionId;
        List<String> messages = redisTemplate.opsForList().range(bufferKey, 0, -1);

        if (messages == null || messages.isEmpty()) {
            scheduledTasks.remove(sessionId);
            return;
        }

        String combinedPrompt = String.join("\n", messages);
        redisTemplate.delete(bufferKey);
        scheduledTasks.remove(sessionId);

        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) {
            log.warn("SSE 연결이 존재하지 않습니다. Session: {}", sessionId);
            return;
        }

        // 비동기로 OpenAI API 호출 및 스트림 처리
        CompletableFuture.runAsync(() -> {
            try {
                String url = "https://api.openai.com/v1/chat/completions";
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", "gpt-4o-mini");
                requestBody.put("messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", combinedPrompt)
                ));
                requestBody.put("stream", true); // 스트리밍 활성화

                byte[] requestBytes = objectMapper.writeValueAsBytes(requestBody);

                // RestTemplate로 스트림 데이터를 한 줄씩 읽어오는 콜백 설정
                restTemplate.execute(url, HttpMethod.POST, request -> {
                    request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    request.getHeaders().setBearerAuth(apiKey);
                    request.getBody().write(requestBytes);
                }, response -> {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8));
                    String line;
                    StringBuilder sentenceBuffer = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                            String data = line.substring(6);
                            JsonNode node = objectMapper.readTree(data);
                            JsonNode contentNode = node.path("choices").get(0).path("delta").path("content");

                            if (!contentNode.isMissingNode()) {
                                String token = contentNode.asText();
                                sentenceBuffer.append(token);

                                // 마침표, 느낌표, 물음표, 줄바꿈이 나오면 한 문장 완성으로 간주!
                                if (token.contains(".") || token.contains("!") || token.contains("?") || token.contains("\n")) {
                                    String sentence = sentenceBuffer.toString().trim();
                                    sentenceBuffer.setLength(0); // 버퍼 비우기

                                    if (!sentence.isEmpty()) {
                                        // 💡 [병렬] 완성된 한 문장만 TTS 변환
                                        byte[] audioBytes = ttsService.generateSpeech(sentence);
                                        String base64Audio = audioBytes != null ? Base64.getEncoder().encodeToString(audioBytes) : "";

                                        // 프론트엔드로 조각(Chunk) 전송
                                        Map<String, String> chunkData = new HashMap<>();
                                        chunkData.put("text", sentence + " ");
                                        chunkData.put("audio", base64Audio);

                                        emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(chunkData)));
                                    }
                                }
                            }
                        }
                    }
                    // 대화가 끝났음을 알림
                    emitter.send(SseEmitter.event().name("done").data(""));
                    return null;
                });

            } catch (Exception e) {
                log.error("Streaming error: ", e);
                try { emitter.completeWithError(e); } catch (Exception ignored) {}
            }
        });
    }
}