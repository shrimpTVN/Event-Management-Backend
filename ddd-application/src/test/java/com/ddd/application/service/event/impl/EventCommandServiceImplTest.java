package com.ddd.application.service.event.impl;

import com.ddd.application.dto.event.EventCreateDto;
import com.ddd.application.dto.event.EventUpdateDto;
import com.ddd.application.mapper.EventDtoMapper;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.*;
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
    private FanpageRepository fanpageRepository;

    @Mock
    private FollowingRepository followingRepository;

    @Mock
    private FanpageMemberRepository fanpageMemberRepository;

    @Mock
    private UserRepository userRepository;

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

    @InjectMocks
    private EventCommandServiceImpl eventCommandService;

    private User testUser;
    private Fanpage testFanpage;
    private FanpageMember testMember;
    private EventType testEventType;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("leader@university.edu.vn");

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
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Should create event successfully when school is valid and active")
    void createEvent_success_whenSchoolIsValidAndActive() {
        Long schoolId = 20L;
        EventCreateDto dto = buildCreateDto(schoolId);

        School school = new School();
        school.setId(schoolId);
        school.setActive(true);

        Event event = new Event();
        event.setId(100L);
        event.setStatus("DRAFT");
        event.setDateOpen(dto.dateOpen());
        event.setDateClose(dto.dateClose());
        event.setDateHappen(dto.dateHappen());
        event.setCapacity(dto.capacity());
        event.setMaleQuantity(dto.maleQuantity());
        event.setFemaleQuantity(dto.femaleQuantity());

        when(securityContextPort.getCurrentUserId()).thenReturn(testUser.getId());
        when(fanpageRepository.findById(dto.fanpageId())).thenReturn(Optional.of(testFanpage));
        when(fanpageMemberRepository.findById(dto.fanpageId(), testUser.getId())).thenReturn(Optional.of(testMember));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(testEventType);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
        when(eventDtoMapper.toEvent(dto)).thenReturn(event);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(followingRepository.findByFanpageIdAndFolStatus(any(), any())).thenReturn(Collections.emptyList());

        Long eventId = eventCommandService.createEvent(dto);

        assertEquals(100L, eventId);
        verify(schoolRepository).findById(schoolId);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when schoolId does not exist on create")
    void createEvent_throwsResourceNotFoundException_whenSchoolNotFound() {
        Long schoolId = 999L;
        EventCreateDto dto = buildCreateDto(schoolId);

        when(securityContextPort.getCurrentUserId()).thenReturn(testUser.getId());
        when(fanpageRepository.findById(dto.fanpageId())).thenReturn(Optional.of(testFanpage));
        when(fanpageMemberRepository.findById(dto.fanpageId(), testUser.getId())).thenReturn(Optional.of(testMember));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(testEventType);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                eventCommandService.createEvent(dto));

        assertTrue(ex.getMessage().contains("School not found with id: 999"));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when school is inactive on create")
    void createEvent_throwsBusinessException_whenSchoolIsInactive() {
        Long schoolId = 20L;
        EventCreateDto dto = buildCreateDto(schoolId);

        School inactiveSchool = new School();
        inactiveSchool.setId(schoolId);
        inactiveSchool.setActive(false);

        when(securityContextPort.getCurrentUserId()).thenReturn(testUser.getId());
        when(fanpageRepository.findById(dto.fanpageId())).thenReturn(Optional.of(testFanpage));
        when(fanpageMemberRepository.findById(dto.fanpageId(), testUser.getId())).thenReturn(Optional.of(testMember));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(testEventType);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(inactiveSchool));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                eventCommandService.createEvent(dto));

        assertTrue(ex.getMessage().contains("deactivated or is no longer active"));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when schoolId does not exist on update")
    void updateEvent_throwsResourceNotFoundException_whenSchoolNotFound() {
        Long eventId = 100L;
        Long schoolId = 999L;
        EventUpdateDto dto = buildUpdateDto(schoolId);

        Event existingEvent = new Event();
        existingEvent.setId(eventId);
        existingEvent.setFanpageId(testFanpage.getId());

        when(eventRepository.findById(eventId)).thenReturn(existingEvent);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(testUser);
        when(fanpageMemberRepository.findById(testFanpage.getId(), testUser.getId())).thenReturn(Optional.of(testMember));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(testEventType);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                eventCommandService.updateEvent(eventId, dto, testUser.getEmail()));

        assertTrue(ex.getMessage().contains("School not found with id: 999"));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when school is inactive on update")
    void updateEvent_throwsBusinessException_whenSchoolIsInactive() {
        Long eventId = 100L;
        Long schoolId = 20L;
        EventUpdateDto dto = buildUpdateDto(schoolId);

        Event existingEvent = new Event();
        existingEvent.setId(eventId);
        existingEvent.setFanpageId(testFanpage.getId());

        School inactiveSchool = new School();
        inactiveSchool.setId(schoolId);
        inactiveSchool.setActive(false);

        when(eventRepository.findById(eventId)).thenReturn(existingEvent);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(testUser);
        when(fanpageMemberRepository.findById(testFanpage.getId(), testUser.getId())).thenReturn(Optional.of(testMember));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(testEventType);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(inactiveSchool));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                eventCommandService.updateEvent(eventId, dto, testUser.getEmail()));

        assertTrue(ex.getMessage().contains("deactivated or is no longer active"));
        verify(eventRepository, never()).save(any(Event.class));
    }
}
