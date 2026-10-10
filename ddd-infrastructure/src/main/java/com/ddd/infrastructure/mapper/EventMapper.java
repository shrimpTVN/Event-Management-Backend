package com.ddd.infrastructure.mapper;

import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.domain.model.Event;
import com.ddd.infrastructure.entity.EventJpaEntity;
import org.mapstruct.*;
import org.springframework.context.annotation.Bean;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring", uses = {EventPointMapper.class})
public interface EventMapper {

    @Mapping(target = "eventTypeId", source = "eventType.id")
    @Mapping(target = "criteriaId", source = "criteria.id")
    @Mapping(target = "fanpageId", source = "fanpage.id")
    @Mapping(target = "semesterId", source = "semester.id")
    @Mapping(target = "active", source = "isActive")
    @Mapping(target = "dateOpen", source = "dateOpen", qualifiedByName = "instantToLocalDate")
    @Mapping(target = "dateClose", source = "dateClose", qualifiedByName = "instantToLocalDate")
    @Mapping(target = "dateHappen", source = "dateHappen", qualifiedByName = "instantToLocalDate")
    @Mapping(target = "status", source = "status", qualifiedByName = "eventStatusEnumToString")
    Event toDomainModel(EventJpaEntity entity);

    @Mapping(target = "eventType", ignore = true)
    @Mapping(target = "criteria", ignore = true)
    @Mapping(target = "fanpage", ignore = true)
    @Mapping(target = "semester", ignore = true)
    @Mapping(target = "eventPoints", ignore = true)
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "dateOpen", source = "dateOpen", qualifiedByName = "localDateToInstant")
    @Mapping(target = "dateClose", source = "dateClose", qualifiedByName = "localDateToInstant")
    @Mapping(target = "dateHappen", source = "dateHappen", qualifiedByName = "localDateToInstant")
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToEventStatusEnum")
    EventJpaEntity toJpaEntity(Event domain);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "eventPoints", ignore = true)
    @Mapping(target = "dateOpen", source = "dateOpen", qualifiedByName = "localDateToInstant")
    @Mapping(target = "dateClose", source = "dateClose", qualifiedByName = "localDateToInstant")
    @Mapping(target = "dateHappen", source = "dateHappen", qualifiedByName = "localDateToInstant")
    void updateJpaEntity(Event domain, @MappingTarget EventJpaEntity entity);

    @Named("instantToLocalDate")
    default LocalDate mapInstantToLocalDate(Instant instant) {
        if (instant == null) return null;
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    @Named("localDateToInstant")
    default Instant mapLocalDateToInstant(LocalDate localDate) {
        if (localDate == null) return null;
        return localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

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
