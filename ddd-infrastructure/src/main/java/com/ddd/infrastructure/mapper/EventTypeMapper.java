package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.EventType;
import com.ddd.infrastructure.entity.EventTypeJpaEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface EventTypeMapper {

    @Mapping(source = "isActive", target = "active")
    EventType toDomainModel(EventTypeJpaEntity entity);

    @Mapping(source = "active", target = "isActive")
    EventTypeJpaEntity toJpaEntity(EventType domain);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(source = "active", target = "isActive")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDomain(EventType domain, @MappingTarget EventTypeJpaEntity entity);
}
