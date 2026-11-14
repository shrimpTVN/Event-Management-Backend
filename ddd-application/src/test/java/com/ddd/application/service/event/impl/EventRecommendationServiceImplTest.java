package com.ddd.application.service.event.impl;

import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.dto.recommendation.EventRecommendationDto;
import com.ddd.application.mapper.EventDtoMapper;
import com.ddd.application.service.event.cache.RecommendationCacheService;
import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.domain.enums.FollowingStatusEnum;
import com.ddd.domain.enums.RegistrationStatusEnum;
import com.ddd.domain.enums.SemesterStatusEnum;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.port.EventRecommendationAiPort;
import com.ddd.domain.model.recommendation.AiRecommendationResult;
import com.ddd.domain.model.recommendation.StudentRecommendationContext;
import com.ddd.infrastructure.entity.*;
import com.ddd.infrastructure.repository.jpaRepository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventRecommendationServiceImplTest {

    @Mock
    private UserJpaRepository userJpaRepository;
    @Mock
    private SemesterJpaRepository semesterJpaRepository;
    @Mock
    private AcademicYearJpaRepository academicYearJpaRepository;
    @Mock
    private PointCategoryJpaRepository pointCategoryJpaRepository;
    @Mock
    private CriteriaJpaRepository criteriaJpaRepository;
    @Mock
    private StudentSemesterPointJpaRepository studentSemesterPointJpaRepository;
    @Mock
    private StudentCriteriaProgressJpaRepository studentCriteriaProgressJpaRepository;
    @Mock
    private EventJpaRepository eventJpaRepository;
    @Mock
    private RegistrationJpaRepository registrationJpaRepository;
    @Mock
    private FollowingJpaRepository followingJpaRepository;
    @Mock
    private FanpageMemberJpaRepository fanpageMemberJpaRepository;
    @Mock
    private RecommendationCacheService recommendationCacheService;
    @Mock
    private EventRecommendationAiPort aiPort;
    @Mock
    private EventDtoMapper eventDtoMapper;

    @InjectMocks
    private EventRecommendationServiceImpl eventRecommendationService;

    private UserJpaEntity testUser;
    private SemesterJpaEntity testSemester;
    private AcademicYearJpaEntity testAcademicYear;

    @BeforeEach
    void setUp() {
        testUser = new UserJpaEntity();
        testUser.setId(1L);
        testUser.setEmail("student@test.edu.vn");

        testSemester = new SemesterJpaEntity();
        testSemester.setId(10L);
        testSemester.setStatus(SemesterStatusEnum.ONGOING);

        testAcademicYear = new AcademicYearJpaEntity();
        testAcademicYear.setId(100L);
        testAcademicYear.setCode("2026-2027");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user is not found")
    void getRecommendationsForStudent_userNotFound_throwsException() {
        when(userJpaRepository.findByEmail("unknown@test.com")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () ->
                eventRecommendationService.getRecommendationsForStudent("unknown@test.com", PageRequest.of(0, 10))
        );
    }

    @Test
    @DisplayName("Should serve recommendations immediately from cache when cache hit occurs")
    void getRecommendationsForStudent_whenCacheHit_returnsCachedListInstantly() {
        String email = "student@test.edu.vn";
        Pageable pageable = PageRequest.of(0, 5);

        when(userJpaRepository.findByEmail(email)).thenReturn(testUser);

        EventInfoDto cachedInfo = createEventInfoDto(101L, "Cached Event");
        EventRecommendationDto cachedDto = new EventRecommendationDto(
                cachedInfo, 95.0, "Cached explanation", 10, 2, true, List.of(), true, 100.0, 60.0
        );
        when(recommendationCacheService.get(1L)).thenReturn(List.of(cachedDto));

        Page<EventRecommendationDto> result = eventRecommendationService.getRecommendationsForStudent(email, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(101L, result.getContent().get(0).event().id());
        assertTrue(result.getContent().get(0).isFollowedFanpage());

        // Zero DB queries or AI calls should have been triggered
        verify(eventJpaRepository, never()).findUpcomingPublishedEvents(any(), any());
        verify(aiPort, never()).generateExplanations(any(), any());
    }

    @Test
    @DisplayName("Should batch count registrations and filter out full and already registered events")
    void getRecommendationsForStudent_shouldExcludeFullAndRegisteredEvents() {
        String email = "student@test.edu.vn";
        Pageable pageable = PageRequest.of(0, 10);

        when(userJpaRepository.findByEmail(email)).thenReturn(testUser);
        when(recommendationCacheService.get(1L)).thenReturn(null); // Cache miss
        when(semesterJpaRepository.findByStatus(SemesterStatusEnum.ONGOING)).thenReturn(Optional.of(testSemester));
        when(academicYearJpaRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIsActiveTrue(any(), any()))
                .thenReturn(Optional.of(testAcademicYear));
        when(pointCategoryJpaRepository.findAll()).thenReturn(Collections.emptyList());
        when(studentSemesterPointJpaRepository.findByUserIdAndSemesterId(1L, 10L)).thenReturn(Collections.emptyList());
        when(studentCriteriaProgressJpaRepository.findAllByUserIdAndAcademicYearId(1L, 100L)).thenReturn(Collections.emptyList());
        when(criteriaJpaRepository.findAllByIsActive(true)).thenReturn(Collections.emptyList());

        // Event 1: Already registered
        EventJpaEntity registeredEvent = new EventJpaEntity();
        registeredEvent.setId(101L);
        registeredEvent.setName("Registered Event");
        registeredEvent.setCapacity(50);
        registeredEvent.setDateHappen(Instant.now().plus(5, ChronoUnit.DAYS));

        // Event 2: Full capacity (capacity 10, active registrations 10)
        EventJpaEntity fullEvent = new EventJpaEntity();
        fullEvent.setId(102L);
        fullEvent.setName("Full Event");
        fullEvent.setCapacity(10);
        fullEvent.setDateHappen(Instant.now().plus(5, ChronoUnit.DAYS));

        // Event 3: Valid with slots (capacity 50, active registrations 5)
        EventJpaEntity validEvent = new EventJpaEntity();
        validEvent.setId(103L);
        validEvent.setName("Valid Open Event");
        validEvent.setCapacity(50);
        validEvent.setDateHappen(Instant.now().plus(2, ChronoUnit.DAYS));

        when(eventJpaRepository.findUpcomingPublishedEvents(eq(EventStatusEnum.PUBLISHED), any()))
                .thenReturn(List.of(registeredEvent, fullEvent, validEvent));

        // Student registered for event 101
        when(registrationJpaRepository.findActiveRegisteredEventIdsByUserId(1L, RegistrationStatusEnum.CANCELLED))
                .thenReturn(List.of(101L));

        // Batch count registrations query (Replaces N+1 single queries)
        Object[] rowFull = new Object[]{102L, 10L};
        Object[] rowValid = new Object[]{103L, 5L};
        when(registrationJpaRepository.countActiveRegistrationsByEventIds(any(), eq(RegistrationStatusEnum.CANCELLED)))
                .thenReturn(List.of(rowFull, rowValid));

        when(followingJpaRepository.findFollowedFanpageIdsByUserId(1L, FollowingStatusEnum.FOLLOWING))
                .thenReturn(Set.of());
        when(fanpageMemberJpaRepository.findJoinedFanpageIdsByUserId(1L))
                .thenReturn(Set.of());
        when(registrationJpaRepository.countAttendedEventTypesByUserId(eq(1L), any()))
                .thenReturn(Collections.emptyList());
        when(registrationJpaRepository.findAttendedPointCategoryIdsByUserId(eq(1L), any()))
                .thenReturn(Collections.emptyList());

        when(aiPort.generateExplanations(any(), any())).thenReturn(List.of(
                new AiRecommendationResult(103L, 90.0, "AI explanation: Gợi ý tham gia vì còn slot và sắp diễn ra.")
        ));

        EventInfoDto infoDto103 = createEventInfoDto(103L, "Valid Open Event");
        when(eventDtoMapper.toInfoDto(validEvent)).thenReturn(infoDto103);

        Page<EventRecommendationDto> result = eventRecommendationService.getRecommendationsForStudent(email, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        EventRecommendationDto recommended = result.getContent().get(0);
        assertEquals(45, recommended.remainingSlots()); // 50 - 5
        assertEquals("AI explanation: Gợi ý tham gia vì còn slot và sắp diễn ra.", recommended.aiExplanation());

        // Verify result was stored in cache
        verify(recommendationCacheService, times(1)).put(eq(1L), anyList());
    }

    @Test
    @DisplayName("Should prioritize event providing missing points, SV5T criteria, and followed fanpage affinity")
    void getRecommendationsForStudent_shouldPrioritizeMissingPointsCriteriaAndFollowedFanpage() {
        String email = "student@test.edu.vn";
        Pageable pageable = PageRequest.of(0, 10);

        when(userJpaRepository.findByEmail(email)).thenReturn(testUser);
        when(recommendationCacheService.get(1L)).thenReturn(null);
        when(semesterJpaRepository.findByStatus(SemesterStatusEnum.ONGOING)).thenReturn(Optional.of(testSemester));
        when(academicYearJpaRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIsActiveTrue(any(), any()))
                .thenReturn(Optional.of(testAcademicYear));

        // Category 1: max 20, student currently has 5 (gap = 15)
        PointCategoryJpaEntity cat1 = new PointCategoryJpaEntity();
        cat1.setId(1L);
        cat1.setName("Học tập");
        cat1.setMaximum(20);

        when(pointCategoryJpaRepository.findAll()).thenReturn(List.of(cat1));

        StudentSemesterPointJpaEntity ssp = new StudentSemesterPointJpaEntity();
        ssp.setPointCategory(cat1);
        ssp.setTotalPoint(5);
        when(studentSemesterPointJpaRepository.findByUserIdAndSemesterId(1L, 10L)).thenReturn(List.of(ssp));

        // Criteria 2 is incomplete
        CriteriaJpaEntity criteria2 = new CriteriaJpaEntity();
        criteria2.setId(2L);
        criteria2.setName("Đạo đức tốt");

        when(studentCriteriaProgressJpaRepository.findAllByUserIdAndAcademicYearId(1L, 100L)).thenReturn(Collections.emptyList());
        when(criteriaJpaRepository.findAllByIsActive(true)).thenReturn(List.of(criteria2));

        // Fanpage 99 is followed by student
        FanpageJpaEntity followedFanpage = new FanpageJpaEntity();
        followedFanpage.setId(99L);
        followedFanpage.setName("CLB AI & Big Data");

        // Event A: Gives 5 points in cat1 + fulfills criteria2 + followed fanpage + happening in 2 days
        EventJpaEntity eventA = new EventJpaEntity();
        eventA.setId(201L);
        eventA.setName("Event High Priority");
        eventA.setCapacity(50);
        eventA.setCriteria(criteria2);
        eventA.setFanpage(followedFanpage);
        eventA.setDateHappen(Instant.now().plus(2, ChronoUnit.DAYS));

        EventPointJpaEntity epA = new EventPointJpaEntity();
        epA.setPointCategory(cat1);
        epA.setPoint(5);
        eventA.setEventPoints(List.of(epA));

        // Event B: Gives 0 points + no criteria + unaffiliated fanpage + happening in 40 days
        EventJpaEntity eventB = new EventJpaEntity();
        eventB.setId(202L);
        eventB.setName("Event Low Priority");
        eventB.setCapacity(50);
        eventB.setDateHappen(Instant.now().plus(40, ChronoUnit.DAYS));

        when(eventJpaRepository.findUpcomingPublishedEvents(eq(EventStatusEnum.PUBLISHED), any()))
                .thenReturn(List.of(eventB, eventA));

        when(registrationJpaRepository.findActiveRegisteredEventIdsByUserId(1L, RegistrationStatusEnum.CANCELLED))
                .thenReturn(Collections.emptyList());
        when(registrationJpaRepository.countActiveRegistrationsByEventIds(any(), any()))
                .thenReturn(Collections.emptyList());

        when(followingJpaRepository.findFollowedFanpageIdsByUserId(1L, FollowingStatusEnum.FOLLOWING))
                .thenReturn(Set.of(99L));
        when(fanpageMemberJpaRepository.findJoinedFanpageIdsByUserId(1L))
                .thenReturn(Set.of());
        when(registrationJpaRepository.countAttendedEventTypesByUserId(eq(1L), any()))
                .thenReturn(Collections.emptyList());
        when(registrationJpaRepository.findAttendedPointCategoryIdsByUserId(eq(1L), any()))
                .thenReturn(Collections.emptyList());

        when(aiPort.generateExplanations(any(), any())).thenReturn(List.of(
                new AiRecommendationResult(201L, 95.0, "Cơ hội tốt hoàn thành điểm và tiêu chí."),
                new AiRecommendationResult(202L, 20.0, "Sự kiện diễn ra trong tương lai.")
        ));

        when(eventDtoMapper.toInfoDto(eventA)).thenReturn(createEventInfoDto(201L, "Event High Priority"));
        when(eventDtoMapper.toInfoDto(eventB)).thenReturn(createEventInfoDto(202L, "Event Low Priority"));

        Page<EventRecommendationDto> result = eventRecommendationService.getRecommendationsForStudent(email, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        EventRecommendationDto topEvent = result.getContent().get(0);
        assertEquals(201L, topEvent.event().id());
        assertTrue(topEvent.isFollowedFanpage());
        assertEquals(100.0, topEvent.fanpageScore());
        assertTrue(topEvent.matchScore() > result.getContent().get(1).matchScore());
    }

    @Test
    @DisplayName("Should send true missing criteria (all minus completed) to AI prompt context")
    void getRecommendationsForStudent_shouldSendTrueMissingCriteriaToAi() {
        String email = "student@test.edu.vn";
        Pageable pageable = PageRequest.of(0, 10);

        when(userJpaRepository.findByEmail(email)).thenReturn(testUser);
        when(recommendationCacheService.get(1L)).thenReturn(null);
        when(semesterJpaRepository.findByStatus(SemesterStatusEnum.ONGOING)).thenReturn(Optional.of(testSemester));
        when(academicYearJpaRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIsActiveTrue(any(), any()))
                .thenReturn(Optional.of(testAcademicYear));
        when(pointCategoryJpaRepository.findAll()).thenReturn(Collections.emptyList());
        when(studentSemesterPointJpaRepository.findByUserIdAndSemesterId(1L, 10L)).thenReturn(Collections.emptyList());

        // Criteria 1 is COMPLETED by student
        CriteriaJpaEntity crit1 = new CriteriaJpaEntity();
        crit1.setId(1L);
        crit1.setName("Criteria 1");

        CriteriaJpaEntity crit2 = new CriteriaJpaEntity();
        crit2.setId(2L);
        crit2.setName("Criteria 2");

        StudentCriteriaProgressJpaEntity progress1 = new StudentCriteriaProgressJpaEntity();
        progress1.setCriteria(crit1);
        progress1.setIsCompleted(true);

        when(studentCriteriaProgressJpaRepository.findAllByUserIdAndAcademicYearId(1L, 100L)).thenReturn(List.of(progress1));

        // Active criteria in system are Criteria 1 and Criteria 2
        when(criteriaJpaRepository.findAllByIsActive(true)).thenReturn(List.of(crit1, crit2));

        EventJpaEntity event = new EventJpaEntity();
        event.setId(301L);
        event.setName("Event with Criteria 2");
        event.setCapacity(50);
        event.setDateHappen(Instant.now().plus(2, ChronoUnit.DAYS));
        event.setCriteria(crit2);

        when(eventJpaRepository.findUpcomingPublishedEvents(eq(EventStatusEnum.PUBLISHED), any()))
                .thenReturn(List.of(event));
        when(registrationJpaRepository.findActiveRegisteredEventIdsByUserId(1L, RegistrationStatusEnum.CANCELLED))
                .thenReturn(Collections.emptyList());
        when(registrationJpaRepository.countActiveRegistrationsByEventIds(any(), any()))
                .thenReturn(Collections.emptyList());
        when(followingJpaRepository.findFollowedFanpageIdsByUserId(1L, FollowingStatusEnum.FOLLOWING))
                .thenReturn(Set.of());
        when(fanpageMemberJpaRepository.findJoinedFanpageIdsByUserId(1L))
                .thenReturn(Set.of());
        when(registrationJpaRepository.countAttendedEventTypesByUserId(eq(1L), any()))
                .thenReturn(Collections.emptyList());
        when(registrationJpaRepository.findAttendedPointCategoryIdsByUserId(eq(1L), any()))
                .thenReturn(Collections.emptyList());

        ArgumentCaptor<StudentRecommendationContext> contextCaptor = ArgumentCaptor.forClass(StudentRecommendationContext.class);
        when(aiPort.generateExplanations(contextCaptor.capture(), any())).thenReturn(Collections.emptyList());
        when(eventDtoMapper.toInfoDto(event)).thenReturn(createEventInfoDto(301L, "Event with Criteria 2"));

        eventRecommendationService.getRecommendationsForStudent(email, pageable);

        StudentRecommendationContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        // Completed criteria 1 should NOT be in missingCriteriaIds
        assertFalse(capturedContext.missingCriteriaIds().contains(1L), "Completed criteria 1 must not be in missingCriteriaIds");
        // Incomplete criteria 2 MUST be in missingCriteriaIds
        assertTrue(capturedContext.missingCriteriaIds().contains(2L), "Incomplete criteria 2 must be in missingCriteriaIds");
    }

    @Test
    @DisplayName("Non-Top-K candidates should remain behind Top K candidates and never leapfrog them")
    void getRecommendationsForStudent_nonTopKCandidatesShouldNotOvertakeTopK() {
        String email = "student@test.edu.vn";
        Pageable pageable = PageRequest.of(0, 20);

        when(userJpaRepository.findByEmail(email)).thenReturn(testUser);
        when(recommendationCacheService.get(1L)).thenReturn(null);
        when(semesterJpaRepository.findByStatus(SemesterStatusEnum.ONGOING)).thenReturn(Optional.of(testSemester));
        when(academicYearJpaRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIsActiveTrue(any(), any()))
                .thenReturn(Optional.of(testAcademicYear));
        when(pointCategoryJpaRepository.findAll()).thenReturn(Collections.emptyList());
        when(studentSemesterPointJpaRepository.findByUserIdAndSemesterId(1L, 10L)).thenReturn(Collections.emptyList());
        when(studentCriteriaProgressJpaRepository.findAllByUserIdAndAcademicYearId(1L, 100L)).thenReturn(Collections.emptyList());
        when(criteriaJpaRepository.findAllByIsActive(true)).thenReturn(Collections.emptyList());

        // Create 12 events: 10 in Top K, 2 outside Top K
        List<EventJpaEntity> allEvents = new java.util.ArrayList<>();
        for (long i = 1; i <= 12; i++) {
            EventJpaEntity e = new EventJpaEntity();
            e.setId(i);
            e.setName("Event " + i);
            e.setCapacity(100);
            e.setDateHappen(Instant.now().plus(i <= 10 ? i : 30, ChronoUnit.DAYS));
            allEvents.add(e);
            when(eventDtoMapper.toInfoDto(e)).thenReturn(createEventInfoDto(i, "Event " + i));
        }

        when(eventJpaRepository.findUpcomingPublishedEvents(eq(EventStatusEnum.PUBLISHED), any()))
                .thenReturn(allEvents);
        when(registrationJpaRepository.findActiveRegisteredEventIdsByUserId(1L, RegistrationStatusEnum.CANCELLED))
                .thenReturn(Collections.emptyList());
        when(registrationJpaRepository.countActiveRegistrationsByEventIds(any(), any()))
                .thenReturn(Collections.emptyList());
        when(followingJpaRepository.findFollowedFanpageIdsByUserId(1L, FollowingStatusEnum.FOLLOWING))
                .thenReturn(Set.of());
        when(fanpageMemberJpaRepository.findJoinedFanpageIdsByUserId(1L))
                .thenReturn(Set.of());
        when(registrationJpaRepository.countAttendedEventTypesByUserId(eq(1L), any()))
                .thenReturn(Collections.emptyList());
        when(registrationJpaRepository.findAttendedPointCategoryIdsByUserId(eq(1L), any()))
                .thenReturn(Collections.emptyList());

        // AI gives low score (e.g. 10.0) to Top 10 events
        when(aiPort.generateExplanations(any(), any())).thenAnswer(invocation -> {
            List<com.ddd.domain.model.recommendation.CandidateEventRecommendation> candidates = invocation.getArgument(1);
            return candidates.stream()
                    .map(c -> new AiRecommendationResult(c.eventId(), 10.0, "Low AI score"))
                    .toList();
        });

        Page<EventRecommendationDto> result = eventRecommendationService.getRecommendationsForStudent(email, pageable);

        assertNotNull(result);
        assertEquals(12, result.getTotalElements());

        // Events 11 and 12 must strictly be at index 10 and 11, NOT overtaking Top 10
        List<Long> resultEventIds = result.getContent().stream()
                .map(r -> r.event().id())
                .toList();

        // The first 10 items must be events 1..10
        Set<Long> top10Ids = Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        for (int idx = 0; idx < 10; idx++) {
            assertTrue(top10Ids.contains(resultEventIds.get(idx)),
                    "Top 10 must only contain candidates evaluated in Top-K. Found event " + resultEventIds.get(idx) + " at index " + idx);
        }

        // Remaining events must be at the end
        assertEquals(11L, (long) resultEventIds.get(10));
        assertEquals(12L, (long) resultEventIds.get(11));

        // And score of event 11 must be <= score of event 10
        assertTrue(result.getContent().get(10).matchScore() <= result.getContent().get(9).matchScore(),
                "Non-Top-K match score must not exceed the lowest Top-K match score");
    }

    private EventInfoDto createEventInfoDto(Long id, String name) {
        return new EventInfoDto(
                id, name, "desc", "loc",
                LocalDate.now(), LocalDate.now().plusDays(10), LocalDate.now().plusDays(15),
                100, 50, 50, "url", "PUBLISHED", "TYPE", "CRIT", "PAGE", 1L, true, null, Instant.now(), List.of()
        );
    }
}
