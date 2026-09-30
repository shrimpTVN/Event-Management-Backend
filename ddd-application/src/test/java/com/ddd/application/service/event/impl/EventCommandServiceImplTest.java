package com.ddd.application.service.event.impl;

import com.ddd.application.dto.event.EventCreateDto;
import com.ddd.application.dto.event.EventPointCreateDto;
import com.ddd.application.mapper.EventDtoMapper;
import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Criteria;
import com.ddd.domain.model.Event;
import com.ddd.domain.model.EventPoint;
import com.ddd.domain.model.EventType;
import com.ddd.domain.model.Fanpage;
import com.ddd.domain.model.FanpageMember;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.CriteriaRepository;
import com.ddd.domain.repository.EventPointRepository;
import com.ddd.domain.repository.EventRepository;
import com.ddd.domain.repository.EventTypeRepository;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.FanpageRepository;
import com.ddd.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private EventPointRepository eventPointRepository;

    @InjectMocks
    private EventCommandServiceImpl eventCommandService;

    private EventCreateDto buildValidEventCreateDto(Long criteriaId, Integer maleQty, Integer femaleQty) {
        LocalDate today = LocalDate.now();
        List<EventPointCreateDto> pointDtos = List.of(new EventPointCreateDto(10L, 5L));

        return new EventCreateDto(
                "Tech Workshop",
                "Workshop description",
                "Hall A",
                today.plusDays(1),
                today.plusDays(5),
                today.plusDays(10),
                100,
                maleQty,
                femaleQty,
                "http://example.com/banner.png",
                1L,
                criteriaId,
                1L,
                pointDtos
        );
    }

    @Test
    @DisplayName("Should create event successfully when all inputs and relations are valid")
    void createEvent_whenAllValid_shouldSucceed() {
        String email = "organizer@example.com";
        EventCreateDto dto = buildValidEventCreateDto(1L, 30, 40);

        Fanpage fanpage = new Fanpage();
        fanpage.setId(dto.fanpageId());

        EventType eventType = new EventType();
        eventType.setId(dto.eventTypeId());

        User user = new User();
        user.setId(2L);
        user.setEmail(email);

        FanpageMember fanpageMember = new FanpageMember(dto.fanpageId(), user.getId(), "ADMIN");
        fanpageMember.setActive(true);

        Criteria criteria = new Criteria();
        criteria.setId(dto.criteriaId());

        Event mappedEvent = new Event();
        mappedEvent.setName(dto.name());

        Event savedEvent = new Event();
        savedEvent.setId(99L);
        savedEvent.setName(dto.name());
        savedEvent.setStatus(EventStatusEnum.DRAFT.name());

        EventPoint eventPoint = new EventPoint();
        eventPoint.setPointCategoryId(10L);
        eventPoint.setPoint(5);

        when(fanpageRepository.findById(dto.fanpageId())).thenReturn(Optional.of(fanpage));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(eventType);
        when(userRepository.findByEmail(email)).thenReturn(user);
        when(fanpageMemberRepository.findById(fanpage.getId(), user.getId())).thenReturn(Optional.of(fanpageMember));
        when(criteriaRepository.findById(dto.criteriaId())).thenReturn(criteria);
        when(eventDtoMapper.toEvent(dto)).thenReturn(mappedEvent);
        when(eventRepository.save(mappedEvent)).thenReturn(savedEvent);
        when(eventDtoMapper.toEventPoints(dto.eventPoints())).thenReturn(List.of(eventPoint));

        eventCommandService.createEvent(dto, email);

        assertEquals(30, mappedEvent.getMaleQuantity());
        assertEquals(40, mappedEvent.getFemaleQuantity());
        assertEquals(EventStatusEnum.DRAFT.name(), mappedEvent.getStatus());

        verify(eventRepository).save(mappedEvent);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EventPoint>> pointsCaptor = ArgumentCaptor.forClass(List.class);
        verify(eventPointRepository).saveAll(pointsCaptor.capture());
        List<EventPoint> capturedPoints = pointsCaptor.getValue();
        assertEquals(1, capturedPoints.size());
        assertEquals(savedEvent.getId(), capturedPoints.get(0).getEventId());
    }

    @Test
    @DisplayName("Should create event successfully when criteriaId is null and quantities are null")
    void createEvent_whenCriteriaIsNullAndQuantitiesAreNull_shouldDefaultQuantitiesToZeroAndSucceed() {
        String email = "organizer@example.com";
        EventCreateDto dto = buildValidEventCreateDto(null, null, null);

        Fanpage fanpage = new Fanpage();
        fanpage.setId(dto.fanpageId());

        EventType eventType = new EventType();
        eventType.setId(dto.eventTypeId());

        User user = new User();
        user.setId(2L);
        user.setEmail(email);

        FanpageMember fanpageMember = new FanpageMember(dto.fanpageId(), user.getId(), "ADMIN");
        fanpageMember.setActive(true);

        Event mappedEvent = new Event();
        Event savedEvent = new Event();
        savedEvent.setId(100L);

        when(fanpageRepository.findById(dto.fanpageId())).thenReturn(Optional.of(fanpage));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(eventType);
        when(userRepository.findByEmail(email)).thenReturn(user);
        when(fanpageMemberRepository.findById(fanpage.getId(), user.getId())).thenReturn(Optional.of(fanpageMember));
        when(eventDtoMapper.toEvent(dto)).thenReturn(mappedEvent);
        when(eventRepository.save(mappedEvent)).thenReturn(savedEvent);
        when(eventDtoMapper.toEventPoints(dto.eventPoints())).thenReturn(Collections.emptyList());

        eventCommandService.createEvent(dto, email);

        verify(criteriaRepository, never()).findById(any());
        assertEquals(0, mappedEvent.getMaleQuantity());
        assertEquals(0, mappedEvent.getFemaleQuantity());
        assertEquals(EventStatusEnum.DRAFT.name(), mappedEvent.getStatus());
        verify(eventRepository).save(mappedEvent);
        verify(eventPointRepository).saveAll(Collections.emptyList());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when fanpage is not found")
    void createEvent_whenFanpageNotFound_shouldThrowResourceNotFoundException() {
        String email = "organizer@example.com";
        EventCreateDto dto = buildValidEventCreateDto(1L, 10, 10);

        when(fanpageRepository.findById(dto.fanpageId())).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> eventCommandService.createEvent(dto, email)
        );

        assertEquals("Fanpage not found with id: " + dto.fanpageId(), exception.getMessage());
        verify(eventTypeRepository, never()).findById(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when event type is not found")
    void createEvent_whenEventTypeNotFound_shouldThrowResourceNotFoundException() {
        String email = "organizer@example.com";
        EventCreateDto dto = buildValidEventCreateDto(1L, 10, 10);

        Fanpage fanpage = new Fanpage();
        fanpage.setId(dto.fanpageId());

        when(fanpageRepository.findById(dto.fanpageId())).thenReturn(Optional.of(fanpage));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> eventCommandService.createEvent(dto, email)
        );

        assertEquals("EventType not found with id: " + dto.eventTypeId(), exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user is not found")
    void createEvent_whenUserNotFound_shouldThrowResourceNotFoundException() {
        String email = "missing@example.com";
        EventCreateDto dto = buildValidEventCreateDto(1L, 10, 10);

        Fanpage fanpage = new Fanpage();
        fanpage.setId(dto.fanpageId());

        EventType eventType = new EventType();
        eventType.setId(dto.eventTypeId());

        when(fanpageRepository.findById(dto.fanpageId())).thenReturn(Optional.of(fanpage));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(eventType);
        when(userRepository.findByEmail(email)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> eventCommandService.createEvent(dto, email)
        );

        assertEquals("User not found with email: " + email, exception.getMessage());
        verify(fanpageMemberRepository, never()).findById(any(), any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when user is not a member of fanpage")
    void createEvent_whenUserIsNotMember_shouldThrowAccessDeniedException() {
        String email = "organizer@example.com";
        EventCreateDto dto = buildValidEventCreateDto(1L, 10, 10);

        Fanpage fanpage = new Fanpage();
        fanpage.setId(dto.fanpageId());

        EventType eventType = new EventType();
        eventType.setId(dto.eventTypeId());

        User user = new User();
        user.setId(2L);
        user.setEmail(email);

        when(fanpageRepository.findById(dto.fanpageId())).thenReturn(Optional.of(fanpage));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(eventType);
        when(userRepository.findByEmail(email)).thenReturn(user);
        when(fanpageMemberRepository.findById(fanpage.getId(), user.getId())).thenReturn(Optional.empty());

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> eventCommandService.createEvent(dto, email)
        );

        assertEquals("User is not a member of this fanpage", exception.getMessage());
        verify(criteriaRepository, never()).findById(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when user membership is inactive")
    void createEvent_whenUserMembershipIsInactive_shouldThrowAccessDeniedException() {
        String email = "organizer@example.com";
        EventCreateDto dto = buildValidEventCreateDto(1L, 10, 10);

        Fanpage fanpage = new Fanpage();
        fanpage.setId(dto.fanpageId());

        EventType eventType = new EventType();
        eventType.setId(dto.eventTypeId());

        User user = new User();
        user.setId(2L);
        user.setEmail(email);

        FanpageMember fanpageMember = new FanpageMember(dto.fanpageId(), user.getId(), "MEMBER");
        fanpageMember.setActive(false);

        when(fanpageRepository.findById(dto.fanpageId())).thenReturn(Optional.of(fanpage));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(eventType);
        when(userRepository.findByEmail(email)).thenReturn(user);
        when(fanpageMemberRepository.findById(fanpage.getId(), user.getId())).thenReturn(Optional.of(fanpageMember));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> eventCommandService.createEvent(dto, email)
        );

        assertEquals("User no longer has active membership in this fanpage", exception.getMessage());
        verify(criteriaRepository, never()).findById(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when criteriaId is provided but criteria does not exist")
    void createEvent_whenCriteriaNotFound_shouldThrowResourceNotFoundException() {
        String email = "organizer@example.com";
        EventCreateDto dto = buildValidEventCreateDto(999L, 10, 10);

        Fanpage fanpage = new Fanpage();
        fanpage.setId(dto.fanpageId());

        EventType eventType = new EventType();
        eventType.setId(dto.eventTypeId());

        User user = new User();
        user.setId(2L);
        user.setEmail(email);

        FanpageMember fanpageMember = new FanpageMember(dto.fanpageId(), user.getId(), "ADMIN");
        fanpageMember.setActive(true);

        when(fanpageRepository.findById(dto.fanpageId())).thenReturn(Optional.of(fanpage));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(eventType);
        when(userRepository.findByEmail(email)).thenReturn(user);
        when(fanpageMemberRepository.findById(fanpage.getId(), user.getId())).thenReturn(Optional.of(fanpageMember));
        when(criteriaRepository.findById(999L)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> eventCommandService.createEvent(dto, email)
        );

        assertEquals("Criteria not found with id: 999", exception.getMessage());
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when dateOpen is in the past")
    void createEvent_whenDateOpenIsInPast_shouldThrowIllegalArgumentException() {
        String email = "organizer@example.com";
        LocalDate today = LocalDate.now();

        EventCreateDto dto = new EventCreateDto(
                "Tech Workshop",
                "Description",
                "Hall A",
                today.minusDays(1),
                today.plusDays(5),
                today.plusDays(10),
                100,
                10,
                10,
                "banner",
                1L,
                null,
                1L,
                Collections.emptyList()
        );

        Fanpage fanpage = new Fanpage();
        fanpage.setId(dto.fanpageId());

        EventType eventType = new EventType();
        eventType.setId(dto.eventTypeId());

        User user = new User();
        user.setId(2L);
        user.setEmail(email);

        FanpageMember fanpageMember = new FanpageMember(dto.fanpageId(), user.getId(), "ADMIN");
        fanpageMember.setActive(true);

        when(fanpageRepository.findById(dto.fanpageId())).thenReturn(Optional.of(fanpage));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(eventType);
        when(userRepository.findByEmail(email)).thenReturn(user);
        when(fanpageMemberRepository.findById(fanpage.getId(), user.getId())).thenReturn(Optional.of(fanpageMember));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventCommandService.createEvent(dto, email)
        );

        assertEquals("Registration open date cannot be in the past", exception.getMessage());
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when dateOpen is after dateClose")
    void createEvent_whenDateOpenIsAfterDateClose_shouldThrowIllegalArgumentException() {
        String email = "organizer@example.com";
        LocalDate today = LocalDate.now();

        EventCreateDto dto = new EventCreateDto(
                "Tech Workshop",
                "Description",
                "Hall A",
                today.plusDays(6),
                today.plusDays(5),
                today.plusDays(10),
                100,
                10,
                10,
                "banner",
                1L,
                null,
                1L,
                Collections.emptyList()
        );

        Fanpage fanpage = new Fanpage();
        fanpage.setId(dto.fanpageId());

        EventType eventType = new EventType();
        eventType.setId(dto.eventTypeId());

        User user = new User();
        user.setId(2L);
        user.setEmail(email);

        FanpageMember fanpageMember = new FanpageMember(dto.fanpageId(), user.getId(), "ADMIN");
        fanpageMember.setActive(true);

        when(fanpageRepository.findById(dto.fanpageId())).thenReturn(Optional.of(fanpage));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(eventType);
        when(userRepository.findByEmail(email)).thenReturn(user);
        when(fanpageMemberRepository.findById(fanpage.getId(), user.getId())).thenReturn(Optional.of(fanpageMember));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventCommandService.createEvent(dto, email)
        );

        assertEquals("Registration open date must be before or equal to registration close date", exception.getMessage());
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when dateClose is after dateHappen")
    void createEvent_whenDateCloseIsAfterDateHappen_shouldThrowIllegalArgumentException() {
        String email = "organizer@example.com";
        LocalDate today = LocalDate.now();

        EventCreateDto dto = new EventCreateDto(
                "Tech Workshop",
                "Description",
                "Hall A",
                today.plusDays(2),
                today.plusDays(8),
                today.plusDays(6),
                100,
                10,
                10,
                "banner",
                1L,
                null,
                1L,
                Collections.emptyList()
        );

        Fanpage fanpage = new Fanpage();
        fanpage.setId(dto.fanpageId());

        EventType eventType = new EventType();
        eventType.setId(dto.eventTypeId());

        User user = new User();
        user.setId(2L);
        user.setEmail(email);

        FanpageMember fanpageMember = new FanpageMember(dto.fanpageId(), user.getId(), "ADMIN");
        fanpageMember.setActive(true);

        when(fanpageRepository.findById(dto.fanpageId())).thenReturn(Optional.of(fanpage));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(eventType);
        when(userRepository.findByEmail(email)).thenReturn(user);
        when(fanpageMemberRepository.findById(fanpage.getId(), user.getId())).thenReturn(Optional.of(fanpageMember));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventCommandService.createEvent(dto, email)
        );

        assertEquals("Registration close date must be before or equal to event happen date", exception.getMessage());
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when male or female quantity is negative")
    void createEvent_whenQuantityIsNegative_shouldThrowIllegalArgumentException() {
        String email = "organizer@example.com";
        EventCreateDto dto = buildValidEventCreateDto(null, -1, 10);

        Fanpage fanpage = new Fanpage();
        fanpage.setId(dto.fanpageId());

        EventType eventType = new EventType();
        eventType.setId(dto.eventTypeId());

        User user = new User();
        user.setId(2L);
        user.setEmail(email);

        FanpageMember fanpageMember = new FanpageMember(dto.fanpageId(), user.getId(), "ADMIN");
        fanpageMember.setActive(true);

        when(fanpageRepository.findById(dto.fanpageId())).thenReturn(Optional.of(fanpage));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(eventType);
        when(userRepository.findByEmail(email)).thenReturn(user);
        when(fanpageMemberRepository.findById(fanpage.getId(), user.getId())).thenReturn(Optional.of(fanpageMember));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventCommandService.createEvent(dto, email)
        );

        assertEquals("Male and female quantities must be non-negative", exception.getMessage());
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when male plus female quantity exceeds capacity")
    void createEvent_whenQuantitiesExceedCapacity_shouldThrowIllegalArgumentException() {
        String email = "organizer@example.com";
        EventCreateDto dto = buildValidEventCreateDto(null, 60, 50); // Total 110 > capacity 100

        Fanpage fanpage = new Fanpage();
        fanpage.setId(dto.fanpageId());

        EventType eventType = new EventType();
        eventType.setId(dto.eventTypeId());

        User user = new User();
        user.setId(2L);
        user.setEmail(email);

        FanpageMember fanpageMember = new FanpageMember(dto.fanpageId(), user.getId(), "ADMIN");
        fanpageMember.setActive(true);

        when(fanpageRepository.findById(dto.fanpageId())).thenReturn(Optional.of(fanpage));
        when(eventTypeRepository.findById(dto.eventTypeId())).thenReturn(eventType);
        when(userRepository.findByEmail(email)).thenReturn(user);
        when(fanpageMemberRepository.findById(fanpage.getId(), user.getId())).thenReturn(Optional.of(fanpageMember));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventCommandService.createEvent(dto, email)
        );

        assertEquals("Total male and female quantities (110) cannot exceed event capacity (100)", exception.getMessage());
        verify(eventRepository, never()).save(any());
    }
}
