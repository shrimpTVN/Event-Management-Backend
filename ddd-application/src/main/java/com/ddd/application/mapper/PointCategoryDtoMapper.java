package com.ddd.application.mapper;

import com.ddd.application.dto.pointcategory.PointCategoryCreateDto;
import com.ddd.application.dto.pointcategory.PointCategoryInfoDto;
import com.ddd.domain.model.PointCategory;
import com.ddd.application.dto.pointcategory.PointCategoryUpdateDto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PointCategoryDtoMapper {
    PointCategory toEntity(PointCategoryCreateDto pointCategoryDto);

    @Mapping(source = "active", target = "isActive")
    PointCategoryInfoDto toInfoDto(PointCategory pointCategory);

    List<PointCategoryInfoDto> toInfoDtoList(List<PointCategory> pointCategories);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "level", ignore = true)
    @Mapping(source = "isActive", target = "active")
    void updateEntityFromDto(PointCategoryUpdateDto dto, @MappingTarget PointCategory entity);
}

