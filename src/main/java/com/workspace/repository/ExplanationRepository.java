package com.workspace.repository;

import com.workspace.entity.Explanation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExplanationRepository extends JpaRepository<Explanation, Long> {
//특정 세션의 로그를 최신순으로 가져오는 쿼리 메서드
List<Explanation> findAllBySessionIdOrderByCreatedAtDesc(String sessionId);
}