package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.pointcategory.req.PointCategoryRequestDto;
import com.ddd.api.dto.pointcategory.res.PointCategoryResponseDto;
import com.ddd.api.mapper.PointCategoryApiMapper;
import com.ddd.application.dto.pointcategory.PointCategoryInfoDto;
import com.ddd.application.service.pointcategory.PointCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/point-categories")
@RequiredArgsConstructor
public class PointCategoryController {

    private final PointCategoryService pointCategoryService;
    private final PointCategoryApiMapper pointCategoryApiMapper;

    @GetMapping("")
    public BaseResponse<List<PointCategoryResponseDto>> getAllPointCategory() {

        List<PointCategoryInfoDto> categories = pointCategoryService.getAllPointCategories();
        return BaseResponse.of(pointCategoryApiMapper.toPointCategoryResponseDtoList(categories));
    }

    @GetMapping("/{id}")
    public BaseResponse<PointCategoryResponseDto> getPointCategoryById(@PathVariable Long id) {
        PointCategoryInfoDto response = pointCategoryService.getPointCategoryById(id);
        return BaseResponse.of(pointCategoryApiMapper.toPointCategoryResponseDto(response));
    }

    @GetMapping("/level")
    public BaseResponse<List<PointCategoryResponseDto>> getPointCategoryByLevel(@RequestParam(defaultValue = "1") Integer level) {

        return null;
    }

    @GetMapping("/parent")
    public BaseResponse<List<PointCategoryResponseDto>> getPointCategoryByParentId(@RequestParam(defaultValue = "0") Long parentId) {
        return null;
    }

    @PostMapping("/admin")
    public BaseResponse<PointCategoryResponseDto> createPointCategory(@RequestBody @Valid PointCategoryRequestDto request) {
        PointCategoryInfoDto response = pointCategoryService.createPointCategory(pointCategoryApiMapper.toPointCategoryCreateDto(request));
        return BaseResponse.of(pointCategoryApiMapper.toPointCategoryResponseDto(response));
    }

}
