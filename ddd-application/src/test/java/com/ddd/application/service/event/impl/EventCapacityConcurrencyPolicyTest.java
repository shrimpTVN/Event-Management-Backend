package com.ddd.application.service.event.impl;

import com.ddd.application.dto.event.EventUpdateDto;
import com.ddd.application.mapper.EventDtoMapper;
import com.ddd.application.service.registration.RegistrationCommandService;
import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.model.Event;
import com.ddd.domain.model.EventType;
import com.ddd.domain.model.FanpageMember;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventCapacityConcurrencyPolicyTest {
    @Mock EventRepository eventRepository;
    @Mock RegistrationRepository registrations;
    @Mock UserRepository users;
    @Mock FanpageMemberRepository members;
    @Mock EventTypeRepository types;
    @Mock RegistrationCommandService registrationCommands;
    @Spy EventDtoMapper mapper = Mappers.getMapper(EventDtoMapper.class);
    @InjectMocks EventCommandServiceImpl service;

    private final Event event = new Event();
    private static final String EMAIL = "member@example.org";

    @BeforeEach
    void authorizedMember() {
        event.setId(1L);
        event.setFanpageId(2L);
        event.setStatus(EventStatusEnum.PUBLISHED.name());
        User user = new User();
        user.setId(3L);
        when(eventRepository.findByIdForUpdate(1L)).thenReturn(event);
        when(users.findByEmail(EMAIL)).thenReturn(user);
        when(members.findById(2L, 3L)).thenReturn(Optional.of(new FanpageMember()));
    }

    private EventUpdateDto capacityUpdate(int capacity) {
        return genderCapacityUpdate(capacity, 0, 0, false);
    }

    private EventUpdateDto genderCapacityUpdate(int capacity, int maleQuantity, int femaleQuantity, boolean genderConstraint) {
        Instant open = Instant.now().plusSeconds(3600);
        when(types.findById(4L)).thenReturn(new EventType());
        when(registrations.countActiveRegistrationsByEventId(1L)).thenReturn(5L);
        return new EventUpdateDto("Event", null, null, open, open.plusSeconds(60), open.plusSeconds(120),
                capacity, maleQuantity, femaleQuantity, genderConstraint, null, 4L, null, null, null, true, null);
    }

    @Test
    void refusesMaleQuantityBelowOccupiedMaleWhenGenderConstraintEnabled() {
        event.setGenderConstraint(true);
        var dto = genderCapacityUpdate(10, 2, 5, true);
        when(registrations.countActiveRegistrationsByEventIdAndGender(1L, "MALE")).thenReturn(3L);
        var failure = assertThrows(BusinessException.class, () -> service.updateEvent(1L, dto, EMAIL));
        assertEquals("Male quantity cannot be lower than current male registrations.", failure.getMessage());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void refusesFemaleQuantityBelowOccupiedFemaleWhenGenderConstraintEnabled() {
        event.setGenderConstraint(true);
        var dto = genderCapacityUpdate(10, 5, 2, true);
        when(registrations.countActiveRegistrationsByEventIdAndGender(1L, "MALE")).thenReturn(2L);
        when(registrations.countActiveRegistrationsByEventIdAndGender(1L, "FEMALE")).thenReturn(3L);
        var failure = assertThrows(BusinessException.class, () -> service.updateEvent(1L, dto, EMAIL));
        assertEquals("Female quantity cannot be lower than current female registrations.", failure.getMessage());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void refusesTogglingGenderConstraintOnWhenExistingGenderExceedsQuota() {
        event.setGenderConstraint(false);
        var dto = genderCapacityUpdate(10, 3, 3, true);
        when(registrations.countActiveRegistrationsByEventIdAndGender(1L, "MALE")).thenReturn(4L);
        var failure = assertThrows(BusinessException.class, () -> service.updateEvent(1L, dto, EMAIL));
        assertEquals("Male quantity cannot be lower than current male registrations.", failure.getMessage());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void permitsGenderQuantitiesAtOrAboveOccupancyAndSkipsChecksWhenDisabled() {
        var disabledDto = genderCapacityUpdate(10, 0, 0, false);
        when(eventRepository.save(event)).thenReturn(event);
        service.updateEvent(1L, disabledDto, EMAIL);
        verify(registrations, never()).countActiveRegistrationsByEventIdAndGender(any(), any());

        var enabledDto = genderCapacityUpdate(10, 3, 2, true);
        when(registrations.countActiveRegistrationsByEventIdAndGender(1L, "MALE")).thenReturn(3L);
        when(registrations.countActiveRegistrationsByEventIdAndGender(1L, "FEMALE")).thenReturn(2L);
        service.updateEvent(1L, enabledDto, EMAIL);
        assertTrue(event.isGenderConstraint());
        verify(eventRepository, times(2)).save(event);
    }

    @Test
    void refusesCapacityBelowOccupancy() {
        var dto = capacityUpdate(4);
        var failure = assertThrows(BusinessException.class, () -> service.updateEvent(1L, dto, EMAIL));
        assertEquals("Event capacity cannot be lower than current registrations.", failure.getMessage());
        verify(eventRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 6})
    void permitsCapacityAtOrAboveOccupancy(int capacity) {
        var dto = capacityUpdate(capacity);
        when(eventRepository.save(event)).thenReturn(event);
        service.updateEvent(1L, dto, EMAIL);
        assertEquals(capacity, event.getCapacity());
        verify(eventRepository).save(event);
    }

    @Test
    void completedStatusUsesAttendanceFinalization() {
        service.changeEventStatus(1L, EventStatusEnum.COMPLETED, EMAIL);
        verify(registrationCommands).fanpageMemberMarkEventAsCompleted(1L);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void finalizedEventCannotBeReopened() {
        event.setStatus(EventStatusEnum.COMPLETED.name());
        assertThrows(BusinessException.class, () -> service.changeEventStatus(1L, EventStatusEnum.PUBLISHED, EMAIL));
        verify(eventRepository, never()).save(any());
    }
}
