package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.StoredEvent;
import com.ddd.infrastructure.entity.StoredEventJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StoredEventMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(source = "isActive", target = "active")
    StoredEvent toDomain(StoredEventJpaEntity entity);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(source = "active", target = "isActive")
    StoredEventJpaEntity toJpaEntity(StoredEvent domain);
}
