package com.ddd.api.mapper;

import com.ddd.api.dto.criteria.req.CriteriaRequestDto;
import com.ddd.api.dto.criteria.res.CriteriaTransactionResponseDto;
import com.ddd.api.dto.criteria.res.CriteriaTreeNodeResponseDto;
import com.ddd.api.dto.criteria.res.CriteriaResponseDto;
import com.ddd.application.dto.criteria.CriteriaDto;
import com.ddd.application.dto.criteria.CriteriaTransactionInfoDto;
import com.ddd.application.dto.criteria.CriteriaTreeNodeDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CriteriaApiMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    CriteriaDto toCriteriaDto(CriteriaRequestDto requestDto);
    
    CriteriaResponseDto toCriteriaResponseDto(CriteriaDto criteriaDto);

    CriteriaTreeNodeResponseDto toCriteriaTreeNodeResponseDto(CriteriaTreeNodeDto criteriaTreeNodeDto);

    CriteriaTransactionResponseDto toCriteriaTransactionResponseDto(CriteriaTransactionInfoDto criteriaTransactionInfoDto);
}
