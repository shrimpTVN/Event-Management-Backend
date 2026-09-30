package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.EventPoint;
import com.ddd.infrastructure.entity.EventPointJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventPointMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "pointCategory", ignore = true)
    @Mapping(target = "isActive", source = "active")
    EventPointJpaEntity toJpaEntity(EventPoint domain);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "pointCategoryId", source = "pointCategory.id")
    @Mapping(target = "active", source = "isActive")
    EventPoint toDomainModel(EventPointJpaEntity entity);
}
