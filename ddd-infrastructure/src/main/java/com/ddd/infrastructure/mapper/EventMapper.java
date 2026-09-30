package com.ddd.infrastructure.mapper;

import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.domain.model.Event;
import com.ddd.infrastructure.entity.EventJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "eventTypeId", source = "eventType.id")
    @Mapping(target = "criteriaId", source = "criteria.id")
    @Mapping(target = "fanpageId", source = "fanpage.id")
    @Mapping(target = "active", source = "isActive")
    @Mapping(target = "dateOpen", source = "dateOpen", qualifiedByName = "instantToLocalDate")
    @Mapping(target = "dateClose", source = "dateClose", qualifiedByName = "instantToLocalDate")
    @Mapping(target = "dateHappen", source = "dateHappen", qualifiedByName = "instantToLocalDate")
    @Mapping(target = "status", source = "status", qualifiedByName = "eventStatusEnumToString")
    Event toDomainModel(EventJpaEntity entity);

    @Mapping(target = "eventType", ignore = true)
    @Mapping(target = "criteria", ignore = true)
    @Mapping(target = "fanpage", ignore = true)
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "dateOpen", source = "dateOpen", qualifiedByName = "localDateToInstant")
    @Mapping(target = "dateClose", source = "dateClose", qualifiedByName = "localDateToInstant")
    @Mapping(target = "dateHappen", source = "dateHappen", qualifiedByName = "localDateToInstant")
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToEventStatusEnum")
    EventJpaEntity toJpaEntity(Event domain);

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
