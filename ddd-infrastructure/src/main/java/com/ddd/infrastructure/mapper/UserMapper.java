package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.User;
import com.ddd.infrastructure.entity.UserJpaEntity;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", ignore = true)
    UserJpaEntity toEntity(User user);

    @Mapping(target = "roleId", source = "role.id")
    User toDomain(UserJpaEntity entity);


}