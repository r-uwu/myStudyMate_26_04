package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyRecord {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId; // 사용자 또는 세션 식별자

    @Column(length = 500)
    private String topic; // 예: 다형성

    @Column(columnDefinition = "TEXT")
    private String summary; // AI가 요약해준 내용

    private LocalDateTime createdAt;

    @Builder
    public StudyRecord(String sessionId, String topic, String summary) {
        this.sessionId = sessionId;
        this.topic = topic;
        this.summary = summary;
        this.createdAt = LocalDateTime.now();
    }
}