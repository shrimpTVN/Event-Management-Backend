package com.ddd.application.mapper;

import com.ddd.application.dto.recommendation.ViewedEventsDto;
import com.ddd.domain.model.recommendation.EventViewBatch;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventViewDtoMapper {
    EventViewBatch toDomain(ViewedEventsDto dto);
}
