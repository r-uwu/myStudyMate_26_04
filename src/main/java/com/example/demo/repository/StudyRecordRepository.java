package com.example.demo.repository;

import com.example.demo.domain.StudyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {

    /**
     * 특정 사용자(세션)의 모든 학습 기록을 최신순(내림차순)으로 조회합니다.
     * 대시보드의 '학습 히스토리' 타임라인을 구성할 때 사용됩니다.
     */
    List<StudyRecord> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * 특정 사용자(세션)가 지금까지 학습한 총 개념(주제)의 개수를 반환합니다.
     * 대시보드의 '총 누적 학습 개념 수' 카드에 사용됩니다.
     */
    long countBySessionId(String sessionId);
}