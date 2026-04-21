package com.workspace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "study_record") // 💡 조회 쿼리와 일치하도록 테이블명 변경
public class Explanation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId;

    private String topic; // 💡 별도 컬럼으로 분리

    @Column(columnDefinition = "TEXT")
    private String summary; // 💡 content 대신 summary로 변경

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}