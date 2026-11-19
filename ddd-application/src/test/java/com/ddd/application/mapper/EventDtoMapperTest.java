package com.ddd.application.mapper;

import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.infrastructure.entity.CriteriaJpaEntity;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.entity.EventTypeJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class EventDtoMapperTest {
    private final EventDtoMapper mapper = Mappers.getMapper(EventDtoMapper.class);

    @Test
    @DisplayName("Should map eventTypeId, eventTypeName, criteriaId, and criteriaName from EventJpaEntity")
    void toInfoDto_shouldMapEventTypeIdAndCriteriaId() {
        EventTypeJpaEntity type = new EventTypeJpaEntity();
        type.setId(20L);
        type.setName("Workshop");

        CriteriaJpaEntity criteria = new CriteriaJpaEntity();
        criteria.setId(55L);
        criteria.setName("Volunteer");

        EventJpaEntity event = new EventJpaEntity();
        event.setId(10L);
        event.setName("Tech Conference");
        event.setEventType(type);
        event.setCriteria(criteria);

        EventInfoDto dto = mapper.toInfoDto(event);

        assertNotNull(dto);
        assertEquals(20L, dto.eventTypeId());
        assertEquals("Workshop", dto.eventTypeName());
        assertEquals(55L, dto.criteriaId());
        assertEquals("Volunteer", dto.criteriaName());
    }

    @Test
    @DisplayName("Should map gracefully when eventType and criteria are null")
    void toInfoDto_whenEventTypeAndCriteriaAreNull_shouldMapGracefully() {
        EventJpaEntity event = new EventJpaEntity();
        event.setId(10L);
        event.setName("Tech Conference");

        EventInfoDto dto = mapper.toInfoDto(event);

        assertNotNull(dto);
        assertNull(dto.eventTypeId());
        assertNull(dto.eventTypeName());
        assertNull(dto.criteriaId());
        assertNull(dto.criteriaName());
    }
}
