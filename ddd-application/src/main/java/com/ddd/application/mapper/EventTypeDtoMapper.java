package com.ddd.application.mapper;

import com.ddd.application.dto.eventtype.EventTypeDto;
import com.ddd.domain.model.EventType;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface EventTypeDtoMapper {

    @Mapping(source = "active", target = "isActive")
    EventTypeDto toDto(EventType eventType);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "isActive", target = "active")
    EventType toDomain(EventTypeDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "dto.isActive", target = "active")
    void updateDomainFromDto(EventTypeDto dto, @MappingTarget EventType eventType);
}
