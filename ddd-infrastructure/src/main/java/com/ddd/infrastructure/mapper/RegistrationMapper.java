package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.Registration;
import com.ddd.infrastructure.entity.RegistrationJpaEntity;

import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RegistrationMapper {
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "id", ignore = true)
    RegistrationJpaEntity toEntity(Registration registration);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "eventId", source = "event.id")
    Registration toDomain(RegistrationJpaEntity registrationJpaEntity);


    @Mapping(target = "user", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDomain(Registration registration, @MappingTarget RegistrationJpaEntity entity);
}
