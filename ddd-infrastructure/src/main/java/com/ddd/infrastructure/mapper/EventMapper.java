package com.ddd.infrastructure.mapper;

import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.domain.model.Event;
import com.ddd.infrastructure.entity.EventJpaEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {EventPointMapper.class})
public interface EventMapper {

    @Mapping(target = "eventTypeId", source = "eventType.id")
    @Mapping(target = "criteriaId", source = "criteria.id")
    @Mapping(target = "fanpageId", source = "fanpage.id")
    @Mapping(target = "semesterId", source = "semester.id")
    @Mapping(target = "active", source = "isActive")
    @Mapping(target = "status", source = "status", qualifiedByName = "eventStatusEnumToString")
    Event toDomainModel(EventJpaEntity entity);

    @Mapping(target = "eventType", ignore = true)
    @Mapping(target = "criteria", ignore = true)
    @Mapping(target = "fanpage", ignore = true)
    @Mapping(target = "semester", ignore = true)
    @Mapping(target = "eventPoints", ignore = true)
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToEventStatusEnum")
    EventJpaEntity toJpaEntity(Event domain);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "eventPoints", ignore = true)
    void updateJpaEntity(Event domain, @MappingTarget EventJpaEntity entity);

    @Named("eventStatusEnumToString")
    default String mapEventStatusEnumToString(EventStatusEnum statusEnum) {
        if (statusEnum == null) return null;
        return statusEnum.name();
    }

    @Named("stringToEventStatusEnum")
    default EventStatusEnum mapStringToEventStatusEnum(String status) {
        if (status == null) return EventStatusEnum.DRAFT;
        try {
            return EventStatusEnum.valueOf(status);
        } catch (IllegalArgumentException e) {
            return EventStatusEnum.DRAFT;
        }
    }
}
