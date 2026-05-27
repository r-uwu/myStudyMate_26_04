package com.workspace.service;

import com.workspace.dto.SubjectStatResponse;
import com.workspace.dto.WeeklyStatResponse;
import com.workspace.mapper.LearningStatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearningStatService {

    private final LearningStatMapper statMapper;

    public List<SubjectStatResponse> getSubjectStats(String userEmail) {
        return statMapper.selectSubjectStats(userEmail);
    }

    public List<WeeklyStatResponse> getWeeklyStats(String userEmail) {
        return statMapper.selectWeeklyStats(userEmail);
    }
}