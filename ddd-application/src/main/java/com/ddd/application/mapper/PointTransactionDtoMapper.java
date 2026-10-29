package com.ddd.application.mapper;

import com.ddd.application.dto.semesterpoint.PointTransactionInfoDto;
import com.ddd.application.dto.semesterpoint.PointTransactionRequestInfoDto;
import com.ddd.domain.model.PointTransaction;
import com.ddd.infrastructure.entity.PointTransactionJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PointTransactionDtoMapper {
    @Mapping(target = "pointCategoryId", source = "pointCategory.id")
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventName", source = "event.name")
    @Mapping(target = "semesterId", source = "semester.id")
    PointTransactionInfoDto toInfoDto(PointTransactionJpaEntity pointTransactionJpaEntity);

    PointTransaction toDomain(PointTransactionInfoDto pointTransactionInfoDto);
    PointTransaction toDomain(PointTransactionRequestInfoDto pointTransactionRequestDto);
}
