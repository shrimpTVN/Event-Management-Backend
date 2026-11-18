package com.ddd.api.mapper;

import com.ddd.api.dto.pointcategory.req.PointCategoryRequestDto;
import com.ddd.api.dto.pointcategory.res.PointCategoryGroupedTreeResponseDto;
import com.ddd.api.dto.pointcategory.res.PointCategoryResponseDto;
import com.ddd.api.dto.pointcategory.res.PointCategoryTreeNodeResponseDto;
import com.ddd.application.dto.pointcategory.*;
import org.mapstruct.Mapper;

import java.util.List;

import com.ddd.api.dto.pointcategory.req.UpdatePointCategoryRequestDto;

@Mapper(componentModel = "spring")
public interface PointCategoryApiMapper {
    PointCategoryCreateDto toPointCategoryCreateDto(PointCategoryRequestDto pointCategoryRequestDto);

    PointCategoryUpdateDto toPointCategoryUpdateDto(UpdatePointCategoryRequestDto updatePointCategoryRequestDto);

    PointCategoryResponseDto toPointCategoryResponseDto(PointCategoryInfoDto pointCategoryInfoDto);

    List<PointCategoryResponseDto> toPointCategoryResponseDtoList(List<PointCategoryInfoDto> dtos);

    PointCategoryTreeNodeResponseDto toPointCategoryTreeNodeResponseDto(PointCategoryTreeNodeDto dto);

    List<PointCategoryTreeNodeResponseDto> toPointCategoryTreeNodeResponseDtoList(List<PointCategoryTreeNodeDto> dtos);

    PointCategoryGroupedTreeResponseDto toPointCategoryGroupedTreeResponseDto(PointCategoryGroupedTreeDto dto);

    List<PointCategoryGroupedTreeResponseDto> toPointCategoryGroupedTreeResponseDtoList(List<PointCategoryGroupedTreeDto> dtos);

}

