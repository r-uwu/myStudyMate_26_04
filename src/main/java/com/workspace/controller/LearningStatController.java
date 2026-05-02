package com.workspace.controller;
import com.workspace.dto.SubjectStatResponse;
import com.workspace.dto.WeeklyStatResponse;
import com.workspace.service.LearningStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/learning/stats")
@RequiredArgsConstructor
public class LearningStatController {

    private final LearningStatService statService;

    @GetMapping("/subjects")
    public ResponseEntity<List<SubjectStatResponse>> getSubjectStats(@RequestParam String sessionId) {
        return ResponseEntity.ok(statService.getSubjectStats(sessionId));
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<WeeklyStatResponse>> getWeeklyStats(@RequestParam String sessionId) {
        return ResponseEntity.ok(statService.getWeeklyStats(sessionId));
    }
}