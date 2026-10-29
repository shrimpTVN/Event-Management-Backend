package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.User;
import com.ddd.infrastructure.entity.UserJpaEntity;
import org.mapstruct.*;


@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    @Mapping(target = "roles", ignore = true)
    UserJpaEntity toEntity(User user);

    User toDomain(UserJpaEntity entity);
}
