package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.pointcategory.req.PointCategoryRequestDto;
import com.ddd.api.dto.pointcategory.req.UpdatePointCategoryRequestDto;
import com.ddd.api.dto.pointcategory.res.PointCategoryGroupedTreeResponseDto;
import com.ddd.api.dto.pointcategory.res.PointCategoryResponseDto;
import com.ddd.api.dto.pointcategory.res.PointCategoryTreeNodeResponseDto;
import com.ddd.api.mapper.PointCategoryApiMapper;
import com.ddd.application.dto.pointcategory.PointCategoryGroupedTreeDto;
import com.ddd.application.dto.pointcategory.PointCategoryInfoDto;
import com.ddd.application.dto.pointcategory.PointCategoryTreeNodeDto;
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

    @GetMapping("/tree")
    public BaseResponse<List<PointCategoryTreeNodeResponseDto>> getPointCategoryTree() {
        List<PointCategoryTreeNodeDto> tree = pointCategoryService.getPointCategoryTree();
        return BaseResponse.of(pointCategoryApiMapper.toPointCategoryTreeNodeResponseDtoList(tree));
    }

    @GetMapping("/grouped-tree")
    public BaseResponse<List<PointCategoryGroupedTreeResponseDto>> getGroupedPointCategoryTree() {
        List<PointCategoryGroupedTreeDto> categories = pointCategoryService.getGroupedPointCategoryTree();
        return BaseResponse.of(pointCategoryApiMapper.toPointCategoryGroupedTreeResponseDtoList(categories));
    }

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
        List<PointCategoryInfoDto> categories = pointCategoryService.getPointCategoryByLevel(level);
        return BaseResponse.of(pointCategoryApiMapper.toPointCategoryResponseDtoList(categories));
    }

    @GetMapping("/parentId")
    public BaseResponse<List<PointCategoryResponseDto>> getPointCategoryByParentId(@RequestParam(defaultValue = "0") Long parentId) {
        List<PointCategoryInfoDto> categories = pointCategoryService.getPointCategoryByParentId(parentId);
        return BaseResponse.of(pointCategoryApiMapper.toPointCategoryResponseDtoList(categories));
    }

    @PostMapping("/admin")
    public BaseResponse<PointCategoryResponseDto> createPointCategory(@RequestBody @Valid PointCategoryRequestDto request) {
        PointCategoryInfoDto response = pointCategoryService.createPointCategory(pointCategoryApiMapper.toPointCategoryCreateDto(request));
        return BaseResponse.of(pointCategoryApiMapper.toPointCategoryResponseDto(response));
    }

    @PatchMapping("/admin/{id}")
    public BaseResponse<PointCategoryResponseDto> updatePointCategory(@PathVariable Long id, @RequestBody @Valid UpdatePointCategoryRequestDto request) {
        PointCategoryInfoDto response = pointCategoryService.updatePointCategory(id, pointCategoryApiMapper.toPointCategoryUpdateDto(request));
        return BaseResponse.of(pointCategoryApiMapper.toPointCategoryResponseDto(response));
    }

    @DeleteMapping("/admin/{id}")
    public BaseResponse<Void> deletePointCategory(@PathVariable Long id) {
        pointCategoryService.deletePointCategory(id);
        return BaseResponse.ok();
    }

}
