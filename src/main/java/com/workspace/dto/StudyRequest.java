package com.workspace.dto;

public record StudyRequest(
        String topic,      // 학습 주제
        String category,   // 카테고리
        String goal        // 학습 목표
) {}