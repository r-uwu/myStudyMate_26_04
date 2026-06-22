package com.workspace.service;

import com.workspace.entity.Explanation;
import com.workspace.repository.ExplanationRepository;
import com.workspace.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ExplanationRepository explanationRepository;

    public DashboardResponseDto getDashboardData(String userEmail) {

        // 1. 주제별 통계 비동기 조회
        CompletableFuture<List<SubjectStatProjection>> subjectsFuture =
                CompletableFuture.supplyAsync(() ->
                        explanationRepository.findSubjectStatsByUserEmail(userEmail));

        // 2. 일간/주간 추이 비동기 조회 (Native Query)
        CompletableFuture<List<DailyStatProjection>> dailyFuture =
                CompletableFuture.supplyAsync(() ->
                        explanationRepository.findDailyStatsByUserEmail(userEmail));

        // 3. 최근 타임라인 비동기 조회
        CompletableFuture<List<Explanation>> recentFuture =
                CompletableFuture.supplyAsync(() ->
                        explanationRepository.findTop5ByUserEmailOrderByCreatedAtDesc(userEmail));

        // 모든 조회가 완료될 때까지 대기
        CompletableFuture.allOf(subjectsFuture, dailyFuture, recentFuture).join();

        // 조회된 데이터를 취합하여 하나의 Response DTO로 반환
        return DashboardResponseDto.builder()
                .subjects(subjectsFuture.join())
                .dailyStats(dailyFuture.join())
                .recentHistory(recentFuture.join())
                .build();
    }
}