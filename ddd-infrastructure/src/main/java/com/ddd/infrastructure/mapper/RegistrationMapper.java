package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.Registration;
import com.ddd.infrastructure.entity.RegistrationJpaEntity;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RegistrationMapper {
    @org.mapstruct.Mapping(target = "user", ignore = true)
    @org.mapstruct.Mapping(target = "event", ignore = true)
    @org.mapstruct.Mapping(target = "id", ignore = true)
    RegistrationJpaEntity toEntity(Registration registration);

    @org.mapstruct.Mapping(target = "userId", source = "user.id")
    @org.mapstruct.Mapping(target = "eventId", source = "event.id")
    Registration toDomain(RegistrationJpaEntity registrationJpaEntity);

    @org.mapstruct.Mapping(target = "user", ignore = true)
    @org.mapstruct.Mapping(target = "event", ignore = true)
    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntityFromDomain(Registration registration, @org.mapstruct.MappingTarget RegistrationJpaEntity entity);
}
