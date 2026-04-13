package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TtsService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public byte[] generateSpeech(String text) {
        String url = "https://api.openai.com/v1/audio/speech";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "tts-1");
        requestBody.put("input", text);
        requestBody.put("voice", "nova"); // 여성 목소리 (alloy, echo, fable, onyx, nova, shimmer 중 선택 가능)

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // 오디오 파일을 byte 배열로 직접 받아옴
            ResponseEntity<byte[]> response = restTemplate.postForEntity(url, entity, byte[].class);
            return response.getBody();
        } catch (Exception e) {
            log.error("TTS 변환 실패: {}", e.getMessage());
            return null;
        }
    }
}