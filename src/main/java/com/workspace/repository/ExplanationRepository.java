package com.workspace.repository;

import com.workspace.entity.Explanation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

import java.util.List;

@Repository
public interface ExplanationRepository extends JpaRepository<Explanation, Long> {
    //특정 세션의 로그를 최신순으로 가져오는 쿼리 메서드
    //List<Explanation> findAllBySessionIdOrderByCreatedAtDesc(String sessionId);

    // 학습 요약표 전체 리스트 조회용
    List<Explanation> findByUserEmail(String userEmail);

    // 통계 대시보드 페이징 및 기간 검색 필터링용
    Page<Explanation> findByUserEmailAndCreatedAtBetween(
            String userEmail,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );
}

