package com.ddd.api.mapper;

import com.ddd.api.dto.pointcategory.req.PointCategoryRequestDto;
import com.ddd.api.dto.pointcategory.res.PointCategoryResponseDto;
import com.ddd.application.dto.pointcategory.PointCategoryCreateDto;
import com.ddd.application.dto.pointcategory.PointCategoryInfoDto;
import org.mapstruct.Mapper;

import java.util.List;

import com.ddd.api.dto.pointcategory.req.UpdatePointCategoryRequestDto;
import com.ddd.application.dto.pointcategory.PointCategoryUpdateDto;

@Mapper(componentModel = "spring")
public interface PointCategoryApiMapper {
    PointCategoryCreateDto toPointCategoryCreateDto(PointCategoryRequestDto pointCategoryRequestDto);

    PointCategoryUpdateDto toPointCategoryUpdateDto(UpdatePointCategoryRequestDto updatePointCategoryRequestDto);

    PointCategoryResponseDto toPointCategoryResponseDto(PointCategoryInfoDto pointCategoryInfoDto);

    List<PointCategoryResponseDto> toPointCategoryResponseDtoList(List<PointCategoryInfoDto> dtos);
}

