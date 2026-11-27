package com.ddd.api.dto.semesterpoint.res;

public record StudentSemesterPointSummaryResponseDto(Long semesterId, int totalPoint, int maximum, String classificationLabel,
        String nextClassificationLabel, int pointsNeeded, int activitiesCount) {}
