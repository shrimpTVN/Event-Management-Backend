package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.Event;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventMapperTest {
    @Test
    void deactivationSurvivesUpdatingAndReloadingAnEvent() {
        EventMapper mapper = Mappers.getMapper(EventMapper.class);
        Event event = new Event();
        event.setActive(true);
        var entity = mapper.toJpaEntity(event);
        assertTrue(entity.getIsActive());

        event.setActive(false);
        mapper.updateJpaEntity(event, entity);

        assertFalse(entity.getIsActive());
        assertFalse(mapper.toDomainModel(entity).isActive());
    }

    @Test
    void genderConstraintSurvivesMappingToJpaAndBack() {
        EventMapper mapper = Mappers.getMapper(EventMapper.class);
        Event event = new Event();
        event.setGenderConstraint(true);
        var entity = mapper.toJpaEntity(event);
        assertTrue(entity.isGenderConstraint());
        assertTrue(mapper.toDomainModel(entity).isGenderConstraint());

        event.setGenderConstraint(false);
        mapper.updateJpaEntity(event, entity);
        assertFalse(entity.isGenderConstraint());
        assertFalse(mapper.toDomainModel(entity).isGenderConstraint());
    }

    @Test
    void restorationClearsViolationReasonToNullWhenUpdatingEntity() {
        EventMapper mapper = Mappers.getMapper(EventMapper.class);
        Event event = new Event();
        event.setActive(false);
        event.setViolationReason("Spam content");
        var entity = mapper.toJpaEntity(event);
        assertEquals("Spam content", entity.getViolationReason());

        event.setActive(true);
        event.setViolationReason(null);
        mapper.updateJpaEntity(event, entity);

        assertTrue(entity.getIsActive());
        assertNull(entity.getViolationReason());
        assertNull(mapper.toDomainModel(entity).getViolationReason());
    }
}
