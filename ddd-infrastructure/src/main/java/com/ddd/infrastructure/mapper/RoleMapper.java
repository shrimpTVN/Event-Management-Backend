package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.Role;
import com.ddd.infrastructure.entity.RoleJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleJpaEntity toJpaEntity(Role role);
    Role toDomainModel(RoleJpaEntity role);
}
