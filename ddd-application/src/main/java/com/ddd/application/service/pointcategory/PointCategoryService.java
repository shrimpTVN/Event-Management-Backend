package com.ddd.application.service.pointcategory;

import com.ddd.application.dto.pointcategory.PointCategoryCreateDto;
import com.ddd.application.dto.pointcategory.PointCategoryInfoDto;

public interface PointCategoryService {
    PointCategoryInfoDto createPointCategory(PointCategoryCreateDto pointCategoryCreateDto);
}
