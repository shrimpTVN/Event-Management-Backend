package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.Notification;
import com.ddd.infrastructure.entity.NotificationJpaEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "fanpageId", source = "fanpage.id")
    @Mapping(target = "active", source = "isActive")
    Notification toDomainModel(NotificationJpaEntity entity);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "fanpage", ignore = true)
    @Mapping(target = "isActive", source = "active")
    NotificationJpaEntity toJpaEntity(Notification domain);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "fanpage", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntityFromDomain(Notification domain, @MappingTarget NotificationJpaEntity entity);
}
