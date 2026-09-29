package com.ddd.application.mapper;

import com.ddd.application.dto.pointcategory.PointCategoryCreateDto;
import com.ddd.application.dto.pointcategory.PointCategoryInfoDto;
import com.ddd.domain.model.PointCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PointCategoryDtoMapper {
    PointCategory toEntity(PointCategoryCreateDto pointCategoryDto);

    @Mapping(source = "active", target = "isActive")
    PointCategoryInfoDto toInfoDto(PointCategory pointCategory);
}
