package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.Fanpage;
import com.ddd.infrastructure.entity.FanpageJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FanpageMapper {
    Fanpage toDomain(FanpageJpaEntity fanpageJpaEntity);

    FanpageJpaEntity toEntity(Fanpage fanpage);
}
