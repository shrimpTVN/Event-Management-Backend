package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.RefreshToken;
import com.ddd.infrastructure.entity.RefreshTokenJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefreshTokenMapper {
    RefreshToken toDomain(RefreshTokenJpaEntity entity);
    RefreshTokenJpaEntity toEntity(RefreshToken domain);
}
