package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.semesterpoint.res.StudentSemesterPointGroupedTreeResponseDto;
import com.ddd.api.dto.semesterpoint.res.StudentSemesterPointResponseDto;
import com.ddd.api.mapper.StudentSemesterPointApiMapper;
import com.ddd.application.dto.semesterpoint.StudentSemesterPointGroupedTreeDto;
import com.ddd.application.dto.semesterpoint.StudentSemesterPointInfoDto;
import com.ddd.application.service.semesterpoint.StudentSemesterPointCommandService;
import com.ddd.application.service.semesterpoint.StudentSemesterPointQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/student-semester-points")
@RequiredArgsConstructor
public class StudentSemesterPointController {
    private final StudentSemesterPointQueryService studentSemesterPointQueryService;
    private final StudentSemesterPointApiMapper studentSemesterPointApiMapper;

    @GetMapping("/grouped-tree/{semesterId}")
    public BaseResponse<List<StudentSemesterPointGroupedTreeResponseDto>> getSemesterPointTree(
            @PathVariable Long semesterId) {
        List<StudentSemesterPointGroupedTreeDto> tree = studentSemesterPointQueryService
                .getSemesterPointTreeForCurrentStudent(semesterId);
        return BaseResponse.of(studentSemesterPointApiMapper.toGroupedTreeResponseDtoList(tree));
    }

    @GetMapping("/me/semesters/{semesterId}")
    public BaseResponse<List<StudentSemesterPointResponseDto>> getSemesterPointForStudent(Authentication authentication ,
                                                                                          @PathVariable Long semesterId) {
        String email = authentication.getName();
        List<StudentSemesterPointInfoDto> semesterPoints = studentSemesterPointQueryService
                .getSemesterPointsForStudent(email, semesterId);
        return BaseResponse.of(semesterPoints.stream().map(studentSemesterPointApiMapper::toResponseDto)
                .collect(Collectors.toList()));
    }
}
