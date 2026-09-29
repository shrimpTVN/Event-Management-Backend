package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.pointcategory.req.PointCategoryRequestDto;
import com.ddd.api.dto.pointcategory.res.PointCategoryResponseDto;
import com.ddd.api.mapper.PointCategoryApiMapper;
import com.ddd.application.dto.pointcategory.PointCategoryInfoDto;
import com.ddd.application.service.pointcategory.PointCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/point-categories")
@RequiredArgsConstructor
public class PointCategoryController {

    private final PointCategoryService pointCategoryService;
    private final PointCategoryApiMapper pointCategoryApiMapper;

    @PostMapping("/admin")
    public BaseResponse<PointCategoryResponseDto> createPointCategory(@RequestBody @Valid PointCategoryRequestDto request) {
        PointCategoryInfoDto response = pointCategoryService.createPointCategory(pointCategoryApiMapper.toPointCategoryCreateDto(request));
        return BaseResponse.of(pointCategoryApiMapper.toPointCategoryResponseDto(response));
    }
}
