package com.workspace.mapper;

import com.workspace.dto.SubjectStatResponse;
import com.workspace.dto.WeeklyStatResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LearningStatMapper {
    List<SubjectStatResponse> selectSubjectStats(@Param("sessionId") String sessionId);
    List<WeeklyStatResponse> selectWeeklyStats(@Param("sessionId") String sessionId);
}