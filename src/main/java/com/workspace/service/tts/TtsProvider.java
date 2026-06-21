package com.workspace.service.tts;

public interface TtsProvider {
    // 해당 프로바이더의 고유 식별자 반환
    String getProviderName();

    // 텍스트를 음성 바이트 배열로 변환
    byte[] generateSpeech(String text);
}