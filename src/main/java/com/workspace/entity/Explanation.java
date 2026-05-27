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

    @Column(nullable = false, name = "user_email")
    private String userEmail;

    private String topic;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "category_id")
    private Long categoryId;

    public void updateCategory(Long categoryId) {
        this.categoryId = categoryId;
    }
}