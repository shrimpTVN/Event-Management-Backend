package com.ddd.application.service.event.impl;

import com.ddd.application.dto.event.EventCreateDto;
import com.ddd.application.dto.event.EventUpdateDto;
import com.ddd.application.mapper.EventDtoMapper;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Event;
import com.ddd.domain.model.EventType;
import com.ddd.domain.model.Fanpage;
import com.ddd.domain.model.FanpageMember;
import com.ddd.domain.model.School;
import com.ddd.domain.model.User;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventCommandServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FanpageMemberRepository fanpageMemberRepository;

    @Mock
    private FanpageRepository fanpageRepository;

    @Mock
    private EventTypeRepository eventTypeRepository;

    @Mock
    private CriteriaRepository criteriaRepository;

    @Mock
    private EventDtoMapper eventDtoMapper;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private PointCategoryRepository pointCategoryRepository;

    @Mock
    private SecurityContextPort securityContextPort;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private FollowingRepository followingRepository;

    @InjectMocks
    private EventCommandServiceImpl eventCommandService;

    private Fanpage testFanpage;
    private FanpageMember testMember;
    private EventType testEventType;

    @BeforeEach
    void setUp() {
        testFanpage = new Fanpage();
        testFanpage.setId(10L);
        testFanpage.setName("IT Club");

        testMember = new FanpageMember();
        testMember.setActive(true);

        testEventType = new EventType();
        testEventType.setId(5L);
        testEventType.setName("Academic Seminar");
    }

    private EventCreateDto buildCreateDto(Long schoolId) {
        Instant now = Instant.now();
        return new EventCreateDto(
                "Test Event",
                "Description",
                "Hall A",
                now.plus(1, ChronoUnit.DAYS),
                now.plus(2, ChronoUnit.DAYS),
                now.plus(3, ChronoUnit.DAYS),
                100,
                50,
                50,
                "https://banner.url",
                5L,
                null,
                10L,
                null,
                schoolId,
                true,
                Collections.emptyList()
        );
    }

    private EventUpdateDto buildUpdateDto(Long schoolId) {
        Instant now = Instant.now();
        return new EventUpdateDto(
                "Updated Event",
                "Updated Description",
                "Hall B",
                now.plus(1, ChronoUnit.DAYS),
                now.plus(2, ChronoUnit.DAYS),
                now.plus(3, ChronoUnit.DAYS),
                120,
                60,
                60,
                "https://banner2.url",
                5L,
                null,
                null,
                schoolId,
                true,
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Should successfully create event when school is valid and active")
    void createEvent_success_whenSchoolIsValidAndActive() {
        Long schoolId = 1L;
        EventCreateDto dto = buildCreateDto(schoolId);
        Long userId = 99L;

        School activeSchool = new School();
        activeSchool.setId(schoolId);
        activeSchool.setName("School of Technology");
        activeSchool.setActive(true);

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(fanpageRepository.findById(10L)).thenReturn(Optional.of(testFanpage));
        when(fanpageMemberRepository.findById(10L, userId)).thenReturn(Optional.of(testMember));
        when(eventTypeRepository.findById(5L)).thenReturn(testEventType);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool));

        Event mappedEvent = new Event();
        mappedEvent.setId(100L);
        mappedEvent.setDateOpen(dto.dateOpen());
        mappedEvent.setDateClose(dto.dateClose());
        mappedEvent.setDateHappen(dto.dateHappen());
        mappedEvent.setCapacity(100);
        mappedEvent.setMaleQuantity(50);
        mappedEvent.setFemaleQuantity(50);

        when(eventDtoMapper.toEvent(dto)).thenReturn(mappedEvent);
        when(eventRepository.save(mappedEvent)).thenReturn(mappedEvent);
        when(followingRepository.findByFanpageIdAndFolStatus(any(), any())).thenReturn(Collections.emptyList());

        Long eventId = eventCommandService.createEvent(dto);

        assertEquals(100L, eventId);
        verify(schoolRepository).findById(schoolId);
        verify(eventRepository).save(mappedEvent);
    }

    @Test
    @DisplayName("Should successfully create event when schoolId is null (all schools)")
    void createEvent_success_whenSchoolIdIsNull() {
        EventCreateDto dto = buildCreateDto(null);
        Long userId = 99L;

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(fanpageRepository.findById(10L)).thenReturn(Optional.of(testFanpage));
        when(fanpageMemberRepository.findById(10L, userId)).thenReturn(Optional.of(testMember));
        when(eventTypeRepository.findById(5L)).thenReturn(testEventType);

        Event mappedEvent = new Event();
        mappedEvent.setId(101L);
        mappedEvent.setDateOpen(dto.dateOpen());
        mappedEvent.setDateClose(dto.dateClose());
        mappedEvent.setDateHappen(dto.dateHappen());
        mappedEvent.setCapacity(100);
        mappedEvent.setMaleQuantity(50);
        mappedEvent.setFemaleQuantity(50);

        when(eventDtoMapper.toEvent(dto)).thenReturn(mappedEvent);
        when(eventRepository.save(mappedEvent)).thenReturn(mappedEvent);
        when(followingRepository.findByFanpageIdAndFolStatus(any(), any())).thenReturn(Collections.emptyList());

        Long eventId = eventCommandService.createEvent(dto);

        assertEquals(101L, eventId);
        verify(schoolRepository, never()).findById(any());
        verify(eventRepository).save(mappedEvent);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when schoolId does not exist")
    void createEvent_throwsResourceNotFound_whenSchoolNotFound() {
        Long nonExistentSchoolId = 999L;
        EventCreateDto dto = buildCreateDto(nonExistentSchoolId);
        Long userId = 99L;

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(fanpageRepository.findById(10L)).thenReturn(Optional.of(testFanpage));
        when(fanpageMemberRepository.findById(10L, userId)).thenReturn(Optional.of(testMember));
        when(eventTypeRepository.findById(5L)).thenReturn(testEventType);
        when(schoolRepository.findById(nonExistentSchoolId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                eventCommandService.createEvent(dto));
        assertTrue(ex.getMessage().contains("School not found with id: 999"));
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessException when school is inactive")
    void createEvent_throwsBusinessException_whenSchoolIsInactive() {
        Long inactiveSchoolId = 2L;
        EventCreateDto dto = buildCreateDto(inactiveSchoolId);
        Long userId = 99L;

        School inactiveSchool = new School();
        inactiveSchool.setId(inactiveSchoolId);
        inactiveSchool.setName("Old School");
        inactiveSchool.setActive(false);

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(fanpageRepository.findById(10L)).thenReturn(Optional.of(testFanpage));
        when(fanpageMemberRepository.findById(10L, userId)).thenReturn(Optional.of(testMember));
        when(eventTypeRepository.findById(5L)).thenReturn(testEventType);
        when(schoolRepository.findById(inactiveSchoolId)).thenReturn(Optional.of(inactiveSchool));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                eventCommandService.createEvent(dto));
        assertTrue(ex.getMessage().contains("School has been deactivated"));
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessException on update when school is inactive")
    void updateEvent_throwsBusinessException_whenSchoolIsInactive() {
        Long inactiveSchoolId = 2L;
        EventUpdateDto dto = buildUpdateDto(inactiveSchoolId);
        String email = "test@example.com";

        Event existingEvent = new Event();
        existingEvent.setId(50L);
        existingEvent.setFanpageId(10L);

        User user = new User();
        user.setId(99L);
        user.setEmail(email);

        School inactiveSchool = new School();
        inactiveSchool.setId(inactiveSchoolId);
        inactiveSchool.setName("Old School");
        inactiveSchool.setActive(false);

        when(eventRepository.findByIdForUpdate(50L)).thenReturn(existingEvent);
        when(userRepository.findByEmail(email)).thenReturn(user);
        when(fanpageMemberRepository.findById(10L, 99L)).thenReturn(Optional.of(testMember));
        when(eventTypeRepository.findById(5L)).thenReturn(testEventType);
        when(schoolRepository.findById(inactiveSchoolId)).thenReturn(Optional.of(inactiveSchool));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                eventCommandService.updateEvent(50L, dto, email));
        assertTrue(ex.getMessage().contains("School has been deactivated"));
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should hide event with violationReason when admin hides it")
    void updateEventVisibilityByAdmin_shouldHideAndSetReason_whenAdmin() {
        Event event = new Event();
        event.setId(55L);
        event.setActive(true);
        when(securityContextPort.hasRole("ADMIN")).thenReturn(true);
        when(eventRepository.findByIdForUpdate(55L)).thenReturn(event);

        eventCommandService.updateEventVisibilityByAdmin(55L, true, "Spam content");

        assertFalse(event.isActive());
        assertEquals("Spam content", event.getViolationReason());
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("Should restore event and clear violationReason when admin unhides it")
    void updateEventVisibilityByAdmin_shouldRestoreAndClearReason_whenAdmin() {
        Event event = new Event();
        event.setId(55L);
        event.setActive(false);
        event.setViolationReason("Spam content");
        when(securityContextPort.hasRole("ADMIN")).thenReturn(true);
        when(eventRepository.findByIdForUpdate(55L)).thenReturn(event);

        eventCommandService.updateEventVisibilityByAdmin(55L, false, null);

        assertTrue(event.isActive());
        assertNull(event.getViolationReason());
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-admin calls updateEventVisibilityByAdmin")
    void updateEventVisibilityByAdmin_shouldThrowUnauthorized_whenNotAdmin() {
        when(securityContextPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(com.ddd.domain.exception.UnauthorizedException.class,
                () -> eventCommandService.updateEventVisibilityByAdmin(55L, true, "Spam"));
        verify(eventRepository, never()).save(any());
    }
}
