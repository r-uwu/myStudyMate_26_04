package com.workspace.service;

import com.workspace.entity.Explanation;
import com.workspace.repository.ExplanationRepository;
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

import java.util.List;
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
        String url = "https://api.openai.com/v1/audio/transcriptions";
        long startTime = System.currentTimeMillis();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(apiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", audioFile.getResource());
            body.add("model", "whisper-1");
            body.add("response_format", "verbose_json");
            body.add("temperature", 0.0);
            body.add("prompt", "학습 내용을 설명하는 한국어 음성입니다.");
            body.add("language", "ko");

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("text")) {
                String transcript = (String) responseBody.get("text");

                if (responseBody.containsKey("segments")) {
                    List<Map<String, Object>> segments = (List<Map<String, Object>>) responseBody.get("segments");
                    if (!segments.isEmpty()) {
                        Map<String, Object> firstSegment = segments.get(0);

                        double noSpeechProb = Double.parseDouble(firstSegment.get("no_speech_prob").toString());
                        double avgLogprob = Double.parseDouble(firstSegment.get("avg_logprob").toString());

                        log.info("STT 분석 결과 - Text: {}, NoSpeechProb: {}, AvgLogprob: {}", transcript, noSpeechProb, avgLogprob);

                        // 텍스트가 명확히 존재하고 신뢰도(Logprob)가 높다면, 무음 확률(NoSpeechProb) 임계치를 0.90까지 방어적으로 상향
                        if (noSpeechProb > 0.90 && avgLogprob < -1.0) {
                            log.warn("⚠️ Whisper 환각 감지 및 차단 (NoSpeech: {}, Logprob: {})", noSpeechProb, avgLogprob);
                            return null;
                        }

                        // 2. 특정 뉴스/방송 관련 키워드 패턴 매칭
                        if (transcript.matches(".*(뉴스 스토리|시청해주셔서|채널|구독|MBC|KBS|SBS|---|신영증권|자막).*")) {
                            log.warn("🚫 전형적인 방송 환각 패턴 감지: {}", transcript);
                            return null;
                        }

                        // 3. 동일 문구 무한 반복(Looping) 감지
                        if (isRepeating(transcript)) {
                            log.warn("🚫 텍스트 무한 반복 루핑 감지: {}", transcript);
                            return null;
                        }
                    }
                }

                long endTime = System.currentTimeMillis();
                log.info("Whisper STT 처리 시간: {}ms", endTime - startTime);
                return transcript;
            }
            return null;

        } catch (Exception e) {
            log.error("Whisper STT 에러: {}", e.getMessage());
            return null;
        }
    }

    private boolean isRepeating(String text) {
        if (text == null || text.length() < 15) return false;
        String half = text.substring(0, text.length() / 2).trim();
        return text.replace(half, "").length() < (text.length() / 4);
    }
}