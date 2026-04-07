package com.example.demo.service;

import com.example.demo.domain.Explanation;
import com.example.demo.repository.ExplanationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SttService {

    private final ExplanationRepository explanationRepository;

    public String processSpeech(MultipartFile audioFile) {
        // 1. 외부 STT API 호출 (지금은 가짜 데이터를 반환하도록 설정)
        // String transcript = externalSttApi.transcribe(audioFile);
        String transcript = "이것은 가비지 컬렉션의 동작 원리에 대한 설명입니다."; // Mock 데이터

        // 2. 변환된 텍스트를 DB에 저장
        Explanation explanation = Explanation.builder()
                .content(transcript)
                .build();

        explanationRepository.save(explanation);

        return transcript;
    }
}