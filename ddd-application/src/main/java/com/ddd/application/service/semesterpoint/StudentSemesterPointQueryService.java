package com.ddd.application.service.semesterpoint;

import com.ddd.application.dto.semesterpoint.StudentSemesterPointInfoDto;

import java.util.List;

public interface StudentSemesterPointQueryService {
    List<StudentSemesterPointInfoDto> getSemesterPointsForStudent(String email, Long semesterId);
}
