package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.criteria.req.ManualCriteriaProgressRequestDto;
import com.ddd.api.dto.criteria.res.CriteriaProgressItemResponseDto;
import com.ddd.api.dto.criteria.res.StudentCriteriaProgressResponseDto;
import com.ddd.api.mapper.StudentCriteriaProgressApiMapper;
import com.ddd.application.dto.criteria.CriteriaProgressItemDto;
import com.ddd.application.dto.criteria.StudentCriteriaProgressDto;
import com.ddd.application.service.criteria.StudentCriteriaProgressCommandService;
import com.ddd.application.service.criteria.StudentCriteriaProgressQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/criteria-progress")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentCriteriaProgressController {
    private final StudentCriteriaProgressQueryService progressQueryService;
    private final StudentCriteriaProgressCommandService progressCommandService;
    private final StudentCriteriaProgressApiMapper progressApiMapper;

    @GetMapping("")
    public BaseResponse<StudentCriteriaProgressResponseDto> getProgress(
            Authentication authentication,
            @RequestParam(required = false) Long academicYearId) {
        StudentCriteriaProgressDto progress = progressQueryService.getProgress(authentication.getName(), academicYearId);
        return BaseResponse.of(progressApiMapper.toResponseDto(progress));
    }

    @GetMapping("/criteria/{criteriaId}")
    public BaseResponse<CriteriaProgressItemResponseDto> getCriteriaProgress(
            Authentication authentication,
            @PathVariable Long criteriaId,
            @RequestParam(required = false) Long academicYearId) {
        CriteriaProgressItemDto progress = progressQueryService.getCriteriaProgress(
                authentication.getName(),
                criteriaId,
                academicYearId
        );
        return BaseResponse.of(progressApiMapper.toResponseDto(progress));
    }

    @PostMapping("/me")
    public BaseResponse<Void> setProgress(
            Authentication authentication,
            @Valid @RequestBody ManualCriteriaProgressRequestDto request) {
        progressCommandService.setManualProgress(
                authentication.getName(),
                request.criteriaId(),
                request.academicYearId(),
                request.note()
        );
        return BaseResponse.ok();
    }
}
