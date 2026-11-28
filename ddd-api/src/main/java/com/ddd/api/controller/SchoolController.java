package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.school.req.*;
import com.ddd.api.dto.school.res.*;
import com.ddd.api.mapper.SchoolApiMapper;
import com.ddd.application.dto.school.SchoolDto;
import com.ddd.application.dto.school.SchoolDetailDto;
import com.ddd.application.service.school.SchoolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;
    private final SchoolApiMapper schoolApiMapper;

    @GetMapping
    public BaseResponse<List<SchoolResponseDto>> getSchools(
            @RequestParam(required = false) Boolean isActive) {


        List<SchoolResponseDto> response = schoolService.findAll().stream()
                .map(schoolApiMapper::toSchoolResponseDto)
                .collect(Collectors.toList());

        return BaseResponse.of(response);
    }

    @GetMapping("/with-majors")
    public BaseResponse<List<SchoolDetailResponseDto>> getSchoolsWithMajors() {
        List<SchoolDetailResponseDto> response = schoolService.findAllWithMajors().stream()
                .map(schoolApiMapper::toSchoolDetailResponseDto)
                .toList();
        return BaseResponse.of(response);
    }

    @GetMapping("/{id}")
    public BaseResponse<SchoolDetailResponseDto> getSchoolById(@PathVariable Long id) {
        SchoolDetailDto schoolDetail = schoolService.findById(id);
        return BaseResponse.of(schoolApiMapper.toSchoolDetailResponseDto(schoolDetail));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<SchoolResponseDto> createSchool(@Valid @RequestBody CreateSchoolRequestDto req) {
        SchoolDto dto = schoolApiMapper.toSchoolDto(req);
        SchoolDto created = schoolService.createSchool(dto);
        return BaseResponse.of(schoolApiMapper.toSchoolResponseDto(created));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<SchoolResponseDto> updateSchool(@PathVariable Long id, @Valid @RequestBody UpdateSchoolRequestDto req) {
        SchoolDto dto = schoolApiMapper.toSchoolDto(req);
        SchoolDto updated = schoolService.updateSchool(id, dto);
        return BaseResponse.of(schoolApiMapper.toSchoolResponseDto(updated));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteSchool(@PathVariable Long id) {
        schoolService.deleteSchool(id);
    }
}
