package com.workspace.service.tts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GoogleTtsProvider implements TtsProvider {

    @Override
    public String getProviderName() {
        return "google";
    }

    @Override
    public byte[] generateSpeech(String text) {
        // Google Cloud Text-to-Speech API 연동 로직 구현
        log.info("Google TTS를 사용하여 음성 변환 수행");
        return new byte[0];
    }
}