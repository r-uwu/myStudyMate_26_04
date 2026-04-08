package com.example.demo.service;

import com.example.demo.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzeService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.prompts.feynman-analyzer}")
    private String systemPrompt;

    private final RestTemplate restTemplate = new RestTemplate();

    public String analyzeWithHistory(List<ChatMessage> history) {
        String url = "https://api.openai.com/v1/chat/completions";
        long startTime = System.currentTimeMillis();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");

        // Request Body 메시지 구성
        List<Map<String, String>> messages = new ArrayList<>();

        // 1. 시스템 프롬프트를 가장 먼저 주입 (API 호출마다 필수)
        messages.add(Map.of("role", "system", "content", systemPrompt));

        // 2. 컨트롤러에서 전달받은 이전 대화 내역과 현재 사용자의 입력을 순차적으로 추가
        for (ChatMessage msg : history) {
            messages.add(Map.of("role", msg.role(), "content", msg.content()));
        }

        requestBody.put("messages", messages);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            long endTime = System.currentTimeMillis();
            logUsage(response, endTime - startTime);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

            return (String) message.get("content");
        } catch (Exception e) {
            log.error("Analysis Error: {}", e.getMessage());
            return "분석 중 오류가 발생했습니다.";
        }
    }

    private void logUsage(Map<String, Object> response, long duration) {
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");

        // Null 안전성을 위해 Number 타입으로 캐스팅 후 int 변환
        int promptTokens = ((Number) usage.get("prompt_tokens")).intValue();
        int completionTokens = ((Number) usage.get("completion_tokens")).intValue();
        int totalTokens = ((Number) usage.get("total_tokens")).intValue();

        log.info("=== GPT-4o-mini Analysis Report ===");
        log.info("Process Time: {}ms", duration);
        log.info("Token Usage - Prompt: {}, Completion: {}, Total: {}", promptTokens, completionTokens, totalTokens);
        log.info("===================================");
    }

    // ... 기존 코드 (analyzeWithHistory 등) ...

    public String summarizeConversation(List<ChatMessage> history) {
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, String>> messages = new ArrayList<>();

        // 요약 전용 시스템 프롬프트 (친근한 동급생 페르소나 유지)
        String summaryPrompt = "You are a friendly study mate. The conversation has ended. " +
                "Summarize the user's explanation from the history. " +
                "Format your response nicely with bullet points: " +
                "1. [우리가 오늘 정리한 내용] - Core concept summary\n" +
                "2. [완벽하게 이해한 부분] - Strong points\n" +
                "3. [다음에 더 파볼 부분] - Remaining knowledge gaps or missing links.\n" +
                "Speak in Korean casually and friendly (e.g., '~했어', '~인 것 같아').";

        messages.add(Map.of("role", "system", "content", summaryPrompt));

        for (ChatMessage msg : history) {
            messages.add(Map.of("role", msg.role(), "content", msg.content()));
        }

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", messages
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            return (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");
        } catch (Exception e) {
            log.error("Summary Error: {}", e.getMessage());
            return "요약 중 에러가 발생했어 ㅠㅠ";
        }
    }
}