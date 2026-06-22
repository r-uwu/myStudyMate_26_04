package com.workspace.repository;

import com.workspace.entity.Explanation;
import com.workspace.dto.SubjectStatProjection;
import com.workspace.dto.DailyStatProjection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExplanationRepository extends JpaRepository<Explanation, Long> {

    // 학습 요약표 전체 리스트 조회용
    List<Explanation> findByUserEmail(String userEmail);

    // 요약표 페이징 및 기간 검색 필터링용
    Page<Explanation> findByUserEmailAndCreatedAtBetween(
            String userEmail,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    // 대시보드 신규: 최근 학습 타임라인 (최신순 5개)
    List<Explanation> findTop5ByUserEmailOrderByCreatedAtDesc(String userEmail);

    // 대시보드 신규: 주제별 학습 비중 통계 (JPQL)
    @Query("SELECT e.topic AS subjectName, COUNT(e) AS studyCount " +
            "FROM Explanation e WHERE e.userEmail = :userEmail GROUP BY e.topic")
    List<SubjectStatProjection> findSubjectStatsByUserEmail(@Param("userEmail") String userEmail);

    // 대시보드 신규: 주간 일간 학습 추이 통계 (Native Query)
    @Query(value = "SELECT DATE(created_at) AS dayLabel, COUNT(*) AS summaryCount, (COUNT(*) * 30) AS studyMinutes " +
            "FROM explanation " +
            "WHERE user_email = :userEmail AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY DATE(created_at) ASC", nativeQuery = true)
    List<DailyStatProjection> findDailyStatsByUserEmail(@Param("userEmail") String userEmail);
}