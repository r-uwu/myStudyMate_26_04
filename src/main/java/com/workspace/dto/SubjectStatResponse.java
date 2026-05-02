package com.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubjectStatResponse {
    private String subjectName;
    private int studyCount;
}