package com.workspace.event;

public record StudyCompletedEvent(
        String userEmail,
        String topic,
        Long studyTimeMinutes
) {
}