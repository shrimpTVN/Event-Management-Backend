package com.ddd.application.dto.semesterpoint;

import com.ddd.domain.model.StudentSemesterPoint;
import java.util.List;
import java.util.Map;

public record StudentSemesterPointCalculation(
        List<StudentSemesterPoint> points,
        Map<Long, NodeMetadata> nodeMetadata) {
    public record NodeMetadata(boolean isAtMaximum, int completionPercentage,
                               Long selectedChildId, boolean contributesToParent) {}
}
