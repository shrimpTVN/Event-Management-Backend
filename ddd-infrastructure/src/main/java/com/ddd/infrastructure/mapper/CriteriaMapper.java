package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.Criteria;
import com.ddd.infrastructure.entity.CriteriaJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CriteriaMapper {
    CriteriaJpaEntity toEntity(Criteria criteria);
    Criteria toDomain(CriteriaJpaEntity criteriaJpaEntity);
}
