package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.auth.res.SemesterResponseDto;
import com.ddd.api.mapper.SemesterApiMapper;
import com.ddd.application.dto.auth.SemesterDto;
import com.ddd.application.service.SemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/semesters")
@RequiredArgsConstructor
public class SemesterController {
    private final SemesterService semesterService;
    private final SemesterApiMapper semesterApiMapper;
    @GetMapping({"","/"})
    public BaseResponse<List<SemesterResponseDto>> getAllSemester() {
        List<SemesterDto> semesterDtos = semesterService.findAll();
        return BaseResponse.of(semesterDtos.stream().map(semesterApiMapper::toResponseDto).toList());
    }

}
