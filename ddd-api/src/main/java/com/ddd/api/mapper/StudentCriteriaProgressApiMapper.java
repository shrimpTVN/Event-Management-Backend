package com.ddd.api.mapper;

import com.ddd.api.dto.criteria.res.CriteriaProgressItemResponseDto;
import com.ddd.api.dto.criteria.res.LeafProgressDetailResponseDto;
import com.ddd.api.dto.criteria.res.StudentCriteriaProgressResponseDto;
import com.ddd.application.dto.criteria.CriteriaProgressItemDto;
import com.ddd.application.dto.criteria.LeafProgressDetailDto;
import com.ddd.application.dto.criteria.StudentCriteriaProgressDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StudentCriteriaProgressApiMapper {
    StudentCriteriaProgressResponseDto toResponseDto(StudentCriteriaProgressDto dto);

    CriteriaProgressItemResponseDto toResponseDto(CriteriaProgressItemDto dto);

    LeafProgressDetailResponseDto toResponseDto(LeafProgressDetailDto dto);
}
