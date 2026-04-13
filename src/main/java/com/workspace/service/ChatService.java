package com.workspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workspace.entity.Explanation;
import com.workspace.repository.ExplanationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.prompts.feynman-analyzer}")
    private String systemPrompt;

    private final SttService sttService;
    private final TtsService ttsService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final ExplanationRepository explanationRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String sessionId) {
        SseEmitter emitter = new SseEmitter(300000L);
        emitters.put(sessionId, emitter);

        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> emitters.remove(sessionId));
        emitter.onError((e) -> emitters.remove(sessionId));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (Exception ignored) {
        }

        return emitter;
    }

    public String processAudioMessage(String sessionId, MultipartFile audioFile) {
        if (audioFile.getSize() < 1000) {
            log.info("Ignored empty audio chunk. Size: {} bytes", audioFile.getSize());
            return null;
        }

        String transcript = sttService.processSpeech(audioFile);

        if (transcript != null && !transcript.trim().isEmpty()) {
            executeAiResponseStreaming(sessionId, transcript);
            return transcript;
        }
        return null;
    }

    public void processTextMessage(String sessionId, String message) {
        executeAiResponseStreaming(sessionId, message);
    }

    private void executeAiResponseStreaming(String sessionId, String prompt) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) {
            log.warn("SSE connection not found for session: {}", sessionId);
            return;
        }

        CompletableFuture.runAsync(() -> {
            StringBuilder fullAiResponseBuffer = new StringBuilder();
            try {
                String url = "https://api.openai.com/v1/chat/completions";
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", "gpt-4o-mini");
                requestBody.put("messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", prompt)
                ));
                requestBody.put("stream", true);

                byte[] requestBytes = objectMapper.writeValueAsBytes(requestBody);

                restTemplate.execute(url, HttpMethod.POST, request -> {
                    request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    request.getHeaders().setBearerAuth(apiKey);
                    request.getBody().write(requestBytes);
                }, response -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        StringBuilder sentenceBuffer = new StringBuilder();

                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                                JsonNode node = objectMapper.readTree(line.substring(6));
                                JsonNode contentNode = node.path("choices").get(0).path("delta").path("content");

                                if (!contentNode.isMissingNode()) {
                                    String token = contentNode.asText();
                                    sentenceBuffer.append(token);
                                    fullAiResponseBuffer.append(token);

                                    if (token.matches(".*[.!?\\n].*")) {
                                        String sentence = sentenceBuffer.toString().trim();
                                        sentenceBuffer.setLength(0);

                                        if (!sentence.isEmpty()) {
                                            byte[] audioBytes = ttsService.generateSpeech(sentence);
                                            try {
                                                sendChunkToClient(emitter, sentence, audioBytes);
                                            } catch (Exception e) {
                                                log.info("사용자의 입력 개입으로 스트리밍을 중단합니다.");
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        if (fullAiResponseBuffer.length() > 0) {
                            saveConversationHistory(sessionId, prompt, fullAiResponseBuffer.toString());
                        }
                        try {
                            emitter.send(SseEmitter.event().name("done").data(""));
                        } catch (Exception ignored) {}
                    }
                    return null;
                });

            } catch (Exception e) {
                log.error("Streaming error: ", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {}
            }
        });
    }

    private void sendChunkToClient(SseEmitter emitter, String text, byte[] audio) throws Exception {
        String base64Audio = audio != null ? Base64.getEncoder().encodeToString(audio) : "";
        Map<String, String> chunkData = new HashMap<>();
        chunkData.put("text", text + " ");
        chunkData.put("audio", base64Audio);
        emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(chunkData)));
    }

    private void saveConversationHistory(String sessionId, String userMsg, String aiMsg) {
        String historyKey = "history:" + sessionId;
        String entry = String.format("User: %s\nAI: %s", userMsg, aiMsg);

        redisTemplate.opsForList().rightPush(historyKey, entry);
        redisTemplate.expire(historyKey, 1, TimeUnit.HOURS);
    }

    public String generateSummary(String sessionId, String topic) {
        String historyKey = "history:" + sessionId;
        List<String> history = redisTemplate.opsForList().range(historyKey, 0, -1);

        if (history == null || history.isEmpty()) {
            log.warn("Redis history is empty for session: {}", sessionId);
            return "대화 기록이 없어 요약할 수 없습니다.";
        }

        String allChat = String.join("\n", history);
        String summaryPrompt = String.format(
                "다음은 '%s' 주제에 대한 학생의 학습 대화 내용입니다. 학습자가 이해한 부분과 보완이 필요한 부분을 구분하여 3줄로 요약하세요.",
                topic
        );

        String summary = getSyncChatResponse(summaryPrompt + "\n\n" + allChat);

        saveToDb(sessionId, topic, summary);
        redisTemplate.delete(historyKey);

        return summary;
    }

    public List<Explanation> getHistory(String sessionId) {
        return explanationRepository.findAllBySessionIdOrderByCreatedAtDesc(sessionId);
    }

    private void saveToDb(String sessionId, String topic, String summary) {
        Explanation explanation = Explanation.builder()
                .sessionId(sessionId)
                .topic(topic)
                .summary(summary)
                .build();
        explanationRepository.save(explanation);
        log.info("세션 {}의 요약 데이터가 DB(study_record)에 저장되었습니다.", sessionId);
    }

    private String getSyncChatResponse(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);
            if (response.getBody() != null) {
                return response.getBody().path("choices").get(0).path("message").path("content").asText();
            }
        } catch (Exception e) {
            log.error("OpenAI 요약 호출 실패", e);
        }
        return "요약 생성 실패";
    }
}