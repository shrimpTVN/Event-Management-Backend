package com.ddd.application.service.pointcategory;

import com.ddd.application.dto.pointcategory.PointCategoryCreateDto;
import com.ddd.application.dto.pointcategory.PointCategoryInfoDto;

import java.util.List;

import com.ddd.application.dto.pointcategory.PointCategoryUpdateDto;

public interface PointCategoryService {
    PointCategoryInfoDto createPointCategory(PointCategoryCreateDto pointCategoryCreateDto);

    List<PointCategoryInfoDto> getAllPointCategories();

    PointCategoryInfoDto getPointCategoryById(Long id);

    List<PointCategoryInfoDto> getPointCategoryByLevel(Integer level);

    List<PointCategoryInfoDto> getPointCategoryByParentId(Long parentId);

    PointCategoryInfoDto updatePointCategory(Long id, PointCategoryUpdateDto pointCategoryUpdateDto);

    void deletePointCategory(Long id);
}

