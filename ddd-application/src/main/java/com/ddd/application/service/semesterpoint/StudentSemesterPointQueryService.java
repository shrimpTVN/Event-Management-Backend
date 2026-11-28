package com.ddd.application.service.semesterpoint;

import com.ddd.application.dto.semesterpoint.StudentSemesterPointGroupedTreeDto;
import com.ddd.application.dto.semesterpoint.StudentSemesterPointInfoDto;

import java.util.List;

public interface StudentSemesterPointQueryService {
    com.ddd.application.dto.semesterpoint.StudentSemesterPointSummaryDto getSummaryForCurrentStudent(Long semesterId);

    List<StudentSemesterPointInfoDto> getSemesterPointsForStudent(String email, Long semesterId);

    List<StudentSemesterPointGroupedTreeDto> getSemesterPointTreeForCurrentStudent(Long semesterId);

    List<StudentSemesterPointGroupedTreeDto> getSemesterPointTreeForStudent(Long userId, Long semesterId);
}

