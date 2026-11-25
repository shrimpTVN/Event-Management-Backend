package com.ddd.application.service.pointcategory;

import com.ddd.application.dto.pointcategory.*;

import java.util.List;

public interface PointCategoryService {
    PointCategoryInfoDto createPointCategory(PointCategoryCreateDto pointCategoryCreateDto);

    List<PointCategoryInfoDto> getAllPointCategories();

    List<PointCategoryInfoDto> getAllPointCategories(Long semesterId);

    List<PointCategoryTreeNodeDto> getPointCategoryTree();

    List<PointCategoryTreeNodeDto> getPointCategoryTree(Long semesterId);

    PointCategoryInfoDto getPointCategoryById(Long id);

    List<PointCategoryInfoDto> getPointCategoryByLevel(Integer level);

    List<PointCategoryInfoDto> getPointCategoryByParentId(Long parentId);

    PointCategoryInfoDto updatePointCategory(Long id, PointCategoryUpdateDto pointCategoryUpdateDto);

    void deletePointCategory(Long id);

    List<PointCategoryGroupedTreeDto> getGroupedPointCategoryTree();

    List<PointCategoryGroupedTreeDto> getGroupedPointCategoryTree(Long semesterId);
}

