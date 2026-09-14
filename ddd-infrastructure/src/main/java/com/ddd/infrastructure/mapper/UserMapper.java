package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.User;
import com.ddd.infrastructure.entity.UserJpaEntity;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface UserMapper {

    UserJpaEntity toEntity(User user);

    User toDomain(UserJpaEntity entity);


}