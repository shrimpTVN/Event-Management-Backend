package com.ddd.api.mapper;

import com.ddd.api.dto.criteria.req.CriteriaRequestDto;
import com.ddd.api.dto.criteria.res.CriteriaResponseDto;
import com.ddd.application.dto.criteria.CriteriaDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CriteriaApiMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    CriteriaDto toCriteriaDto(CriteriaRequestDto requestDto);
    
    CriteriaResponseDto toCriteriaResponseDto(CriteriaDto criteriaDto);
}
