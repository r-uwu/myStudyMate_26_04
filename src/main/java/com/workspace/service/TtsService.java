package com.workspace.service;

import com.workspace.service.tts.TtsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TtsService {

    private final Map<String, TtsProvider> providerMap;

    // 스프링이 TtsProvider를 구현한 모든 빈을 List로 주입합니다.
    public TtsService(List<TtsProvider> providers) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(TtsProvider::getProviderName, provider -> provider));
    }

    // 기본 프로바이더를 사용하는 오버로딩 메서드
    public byte[] generateSpeech(String text) {
        return generateSpeech(text, "openai");
    }

    // 프로바이더를 명시적으로 선택하는 메서드
    public byte[] generateSpeech(String text, String providerName) {
        TtsProvider provider = providerMap.get(providerName);

        if (provider == null) {
            log.warn("지원하지 않는 TTS 프로바이더입니다: {}. 기본값(openai)으로 대체합니다.", providerName);
            provider = providerMap.get("openai");
        }

        return provider.generateSpeech(text);
    }
}