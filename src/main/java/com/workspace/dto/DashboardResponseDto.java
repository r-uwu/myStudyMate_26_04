package com.workspace.dto;

import com.workspace.entity.Explanation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDto {
    /* 주제별 통계 데이터 리스트 */
    private List<SubjectStatProjection> subjects;

    /* 일별/주별 통계 데이터 리스트 */
    private List<DailyStatProjection> dailyStats;

    /* 최근 학습 기록 리스트 */
    private List<Explanation> recentHistory;
}