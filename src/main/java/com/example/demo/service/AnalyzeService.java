package com.example.demo.service;

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

    private final RestTemplate restTemplate = new RestTemplate();

    public String analyzeExplanation(String transcript) {
        String url = "https://api.openai.com/v1/chat/completions";

        long startTime = System.currentTimeMillis(); // 시간 측정 시작

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini"); // 가성비 모델 지정

//        String systemPrompt = "You are a professional educator specializing in the Feynman Technique. " +
//                "Analyze the user's explanation and provide friendly feedback in Korean. " +
//                "Focus on: 1. Technical jargon difficult for elementary students to understand, " +
//                "2. Logical leaps or gaps in the explanation, " +
//                "3. Core concepts that need further elaboration. " +
//                "Always respond in Korean.";

        String systemPrompt = "Act as empathetic peer & tech tutor. Output Korean only. Empathize, validate STT input, correct blind spots via hints. Append JSON block at end: {\"summary\":\"\",\"knowledge_gaps\":[],\"accuracy_score\":0,\"sentiment\":\"\",\"key_keywords\":[]}. Strictly separate chat and JSON. No emojis. Ensure tech depth.";

        List<Map<String, String>> messages = new ArrayList<>();
        // 시스템 프롬프트: 분석 페르소나 설정

        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", transcript));

        requestBody.put("messages", messages);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            long endTime = System.currentTimeMillis(); // 시간 측정 종료

            // 비용 및 성능 로그 기록
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
        int promptTokens = (int) usage.get("prompt_tokens");
        int completionTokens = (int) usage.get("completion_tokens");
        int totalTokens = (int) usage.get("total_tokens");

        log.info("=== GPT-4o-mini 분석 리포트 ===");
        log.info("처리 시간: {}ms", duration);
        log.info("사용 토큰: Prompt({}), Completion({}), Total({})", promptTokens, completionTokens, totalTokens);
        // gpt-4o-mini 가격 기준(입력 $0.15/1M, 출력 $0.6/1M)으로 대략적인 비용 계산 로그 추가 가능
        log.info("==============================");
    }
}