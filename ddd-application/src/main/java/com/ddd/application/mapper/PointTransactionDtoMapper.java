package com.ddd.application.mapper;

import com.ddd.application.dto.semesterpoint.PointTransactionInfoDto;
import com.ddd.infrastructure.entity.PointTransactionJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PointTransactionDtoMapper {
    @Mapping(target = "pointCategoryId", source = "pointCategory.id")
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventName", source = "event.name")
    PointTransactionInfoDto toInfoDto(PointTransactionJpaEntity pointTransactionJpaEntity);
}
