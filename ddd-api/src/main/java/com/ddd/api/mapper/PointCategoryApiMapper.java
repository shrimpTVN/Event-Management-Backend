package com.ddd.api.mapper;

import com.ddd.api.dto.pointcategory.req.PointCategoryRequestDto;
import com.ddd.api.dto.pointcategory.res.PointCategoryResponseDto;
import com.ddd.application.dto.pointcategory.PointCategoryCreateDto;
import com.ddd.application.dto.pointcategory.PointCategoryInfoDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PointCategoryApiMapper {
    PointCategoryCreateDto toPointCategoryCreateDto(PointCategoryRequestDto pointCategoryRequestDto);

    PointCategoryResponseDto toPointCategoryResponseDto(PointCategoryInfoDto pointCategoryInfoDto);
}
