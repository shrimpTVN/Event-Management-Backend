package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.Fanpage;
import com.ddd.infrastructure.entity.FanpageJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FanpageMapper {
    @Mapping(source = "isActive", target = "active")
    Fanpage toDomain(FanpageJpaEntity fanpageJpaEntity);

    @Mapping(source = "active", target = "isActive")
    FanpageJpaEntity toEntity(Fanpage fanpage);
}
