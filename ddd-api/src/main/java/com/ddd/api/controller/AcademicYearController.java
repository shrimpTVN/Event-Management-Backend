package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.academicyear.res.AcademicYearResponseDto;
import com.ddd.api.mapper.AcademicYearApiMapper;
import com.ddd.application.service.academicyear.AcademicYearService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/academic-years")
@RequiredArgsConstructor
public class AcademicYearController {
    private final AcademicYearService academicYearService;
    private final AcademicYearApiMapper academicYearApiMapper;

    @GetMapping({"", "/"})
    public BaseResponse<List<AcademicYearResponseDto>> getAllActive() {
        return BaseResponse.of(academicYearApiMapper.toResponseDtoList(academicYearService.findAllActive()));
    }

    @GetMapping("/current")
    public BaseResponse<AcademicYearResponseDto> getCurrent() {
        return BaseResponse.of(academicYearApiMapper.toResponseDto(academicYearService.getCurrent()));
    }
}
