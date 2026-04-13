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
            body.add("prompt", "이것은 학생이 설명하는 스터디 대화입니다.");
            body.add("language", "ko");

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("text")) {
                Map<String, Object> responseBody = response.getBody();
                String transcript = (String) responseBody.get("text");

                // --- 💡 무음 확률(no_speech_prob) 체크 로직 시작 ---
                if (responseBody.containsKey("segments")) {
                    List<Map<String, Object>> segments = (List<Map<String, Object>>) responseBody.get("segments");
                    if (!segments.isEmpty()) {
                        // 첫 번째 세그먼트의 무음 확률 추출
                        Object noSpeechProbObj = segments.get(0).get("no_speech_prob");
                        if (noSpeechProbObj != null) {
                            double noSpeechProb = Double.parseDouble(noSpeechProbObj.toString());

                            // 무음 확률이 0.6(60%)을 넘으면 환각으로 간주하고 중단
                            if (noSpeechProb > 0.6) {
                                log.warn("⚠️ Whisper 환각 감지 (확률: {}), 텍스트: {}", noSpeechProb, transcript);
                                return null;
                            }
                        }
                    }
                }
                // --- 💡 무음 확률 체크 로직 끝 ---

                // 환각이 아님이 검증된 경우에만 DB 저장 진행
//                explanationRepository.save(Explanation.builder()
//                        .content(transcript)
//                        .build());

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
}