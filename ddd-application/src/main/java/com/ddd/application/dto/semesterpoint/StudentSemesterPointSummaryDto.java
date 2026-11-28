package com.ddd.application.dto.semesterpoint;

public record StudentSemesterPointSummaryDto(Long semesterId, int totalPoint, int maximum, String classificationLabel,
        String nextClassificationLabel, int pointsNeeded, int activitiesCount) {}
