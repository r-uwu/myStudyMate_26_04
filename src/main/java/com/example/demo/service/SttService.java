package com.example.demo.service;

import com.example.demo.domain.Explanation;
import com.example.demo.repository.ExplanationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SttService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final ExplanationRepository explanationRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public String processSpeech(MultipartFile audioFile) {
        // Whisper-1 전용 엔드포인트
        String url = "https://api.openai.com/v1/audio/transcriptions";

        //비용측정용 타이머
        long startTime = System.currentTimeMillis();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(apiKey);

            // multipart/form-data 구성을 위한 MultiValueMap 사용
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", audioFile.getResource());
            body.add("model", "whisper-1");
            body.add("language", "ko"); // 한국어 정확도 향상을 위한 명시적 지정

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            // API 호출
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("text")) {
                String transcript = (String) response.getBody().get("text");

                // 1단계: 변환된 텍스트 DB 저장
                explanationRepository.save(Explanation.builder()
                        .content(transcript)
                        .build());

                long endTime = System.currentTimeMillis();
                log.info("Whisper STT 처리 시간: {}ms", endTime - startTime);
                log.info("파일 크기: {} bytes", audioFile.getSize());

                return transcript;
            }
            return "변환 실패: 응답 텍스트 없음";

        } catch (Exception e) {
            e.printStackTrace();
            return "Whisper STT 에러: " + e.getMessage();
        }
    }
}