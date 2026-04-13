package com.example.demo.service;

import com.example.demo.domain.Explanation;
import com.example.demo.repository.ExplanationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SttService_forGPT4o {

    @Value("${openai.api.key}")
    private String apiKey;

    private final ExplanationRepository explanationRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {

        System.out.println("★ API KEY LOADED: " + apiKey);
    }

    public String processSpeech(MultipartFile audioFile) {
        String url = "https://api.openai.com/v1/chat/completions";

        try {
            // 1. 인코딩 확인
            byte[] bytes = audioFile.getBytes();
            String base64Audio = Base64.getEncoder().encodeToString(bytes);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // 2. 요청 바디 구성 (GPT-4o 전용)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-audio-preview");

            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");

            List<Map<String, Object>> content = new ArrayList<>();
            content.add(Map.of("type", "text", "text", "이 음성을 한국어로 받아쓰기하고 요약해줘."));
            content.add(Map.of(
                    "type", "input_audio",
                    "input_audio", Map.of("data", base64Audio, "format", "mp3")
            ));

            userMessage.put("content", content);
            messages.add(userMessage);
            requestBody.put("messages", messages);

            // 3. 호출 및 상세 에러 로깅
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            String transcript = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");

            explanationRepository.save(Explanation.builder().content(transcript).build());
            return transcript;

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // ★ 핵심: OpenAI가 보낸 실제 에러 메시지를 콘솔에 출력합니다.
            System.err.println("OpenAI API Error: " + e.getResponseBodyAsString());
            return "API 호출 에러: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            e.printStackTrace();
            return "서버 내부 에러: " + e.getMessage();
        }
    }
}