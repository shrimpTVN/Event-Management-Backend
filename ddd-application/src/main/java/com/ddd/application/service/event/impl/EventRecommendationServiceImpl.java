package com.ddd.application.service.event.impl;

import com.ddd.application.dto.recommendation.EventRecommendationDto;
import com.ddd.application.dto.recommendation.PointCategoryGainDto;
import com.ddd.application.mapper.EventDtoMapper;
import com.ddd.application.service.event.EventRecommendationService;
import com.ddd.application.service.event.cache.RecommendationCacheService;
import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.domain.enums.FollowingStatusEnum;
import com.ddd.domain.enums.RegistrationStatusEnum;
import com.ddd.domain.enums.SemesterStatusEnum;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.port.EventRecommendationAiPort;
import com.ddd.domain.model.recommendation.AiRecommendationResult;
import com.ddd.domain.model.recommendation.CandidateEventRecommendation;
import com.ddd.domain.model.recommendation.StudentRecommendationContext;
import com.ddd.infrastructure.entity.*;
import com.ddd.infrastructure.repository.jpaRepository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class EventRecommendationServiceImpl implements EventRecommendationService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserJpaRepository userJpaRepository;
    private final SemesterJpaRepository semesterJpaRepository;
    private final AcademicYearJpaRepository academicYearJpaRepository;
    private final PointCategoryJpaRepository pointCategoryJpaRepository;
    private final CriteriaJpaRepository criteriaJpaRepository;
    private final StudentSemesterPointJpaRepository studentSemesterPointJpaRepository;
    private final StudentCriteriaProgressJpaRepository studentCriteriaProgressJpaRepository;
    private final EventJpaRepository eventJpaRepository;
    private final RegistrationJpaRepository registrationJpaRepository;
    private final FollowingJpaRepository followingJpaRepository;
    private final FanpageMemberJpaRepository fanpageMemberJpaRepository;
    private final RecommendationCacheService recommendationCacheService;
    private final EventRecommendationAiPort aiPort;
    private final EventDtoMapper eventDtoMapper;

    @Override
    public Page<EventRecommendationDto> getRecommendationsForStudent(String email, Pageable pageable) {
        UserJpaEntity user = userJpaRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        // 1. Check in-memory recommendation cache for fast pagination (< 1ms)
        List<EventRecommendationDto> cached = recommendationCacheService.get(user.getId());
        if (cached != null) {
            log.info("Serving event recommendations from cache for userId: {}", user.getId());
            return paginate(cached, pageable);
        }

        LocalDate today = LocalDate.now(VIETNAM_ZONE);

        // 2. Resolve current active Semester and Academic Year
        SemesterJpaEntity activeSemester = resolveActiveSemester();
        AcademicYearJpaEntity activeAcademicYear = resolveActiveAcademicYear(today);

        // 3. Fetch point categories and student's missing training point gaps
        List<PointCategoryJpaEntity> allCategories = pointCategoryJpaRepository.findAll();
        Map<Long, Integer> studentPointsMap = fetchStudentSemesterPoints(user.getId(), activeSemester);
        Map<Long, Integer> missingPointGaps = calculatePointGaps(allCategories, studentPointsMap);

        // 4. Fetch student's achieved SV5T criteria and calculate missing criteria for the active academic year
        Set<Long> completedCriteriaIds = fetchCompletedCriteriaIds(user.getId(), activeAcademicYear);
        Set<Long> allActiveCriteriaIds = criteriaJpaRepository.findAllByIsActive(true).stream()
                .map(CriteriaJpaEntity::getId)
                .collect(Collectors.toSet());
        Set<Long> missingCriteriaIds = new HashSet<>(allActiveCriteriaIds);
        missingCriteriaIds.removeAll(completedCriteriaIds);

        // 5. Query upcoming published events (eagerly fetched via @EntityGraph with DISTINCT)
        Instant now = Instant.now();
        List<EventJpaEntity> upcomingEvents = eventJpaRepository.findUpcomingPublishedEvents(EventStatusEnum.PUBLISHED, now);

        // 6. Query user's registered event IDs to filter out already registered events
        Set<Long> registeredEventIds = new HashSet<>(
                registrationJpaRepository.findActiveRegisteredEventIdsByUserId(user.getId(), RegistrationStatusEnum.CANCELLED)
        );

        // 7. Batch Query: Active registrations count for ALL upcoming events in 1 single query (Eliminate N+1)
        List<Long> eventIds = upcomingEvents.stream().map(EventJpaEntity::getId).toList();
        Map<Long, Long> activeCountMap = new HashMap<>();
        if (!eventIds.isEmpty()) {
            List<Object[]> countRows = registrationJpaRepository.countActiveRegistrationsByEventIds(
                    eventIds, RegistrationStatusEnum.CANCELLED
            );
            for (Object[] row : countRows) {
                if (row[0] != null && row[1] != null) {
                    activeCountMap.put((Long) row[0], ((Number) row[1]).longValue());
                }
            }
        }

        // 8. Behavioral Data 1: Followed and Member Fanpages
        Set<Long> followedFanpageIds = followingJpaRepository.findFollowedFanpageIdsByUserId(
                user.getId(), FollowingStatusEnum.FOLLOWING
        );
        Set<Long> memberFanpageIds = fanpageMemberJpaRepository.findJoinedFanpageIdsByUserId(user.getId());
        Set<Long> affiliatedFanpageIds = new HashSet<>();
        if (followedFanpageIds != null) {
            affiliatedFanpageIds.addAll(followedFanpageIds);
        }
        if (memberFanpageIds != null) {
            affiliatedFanpageIds.addAll(memberFanpageIds);
        }

        // 9. Behavioral Data 2: Attendance History (CHECKED_IN / COMPLETED)
        List<RegistrationStatusEnum> attendedStatuses = List.of(
                RegistrationStatusEnum.CHECKED_IN,
                RegistrationStatusEnum.COMPLETED
        );
        List<Object[]> attendedTypeRows = registrationJpaRepository.countAttendedEventTypesByUserId(
                user.getId(), attendedStatuses
        );
        Map<Long, Long> attendedEventTypeCounts = new HashMap<>();
        for (Object[] row : attendedTypeRows) {
            if (row[0] != null && row[1] != null) {
                attendedEventTypeCounts.put((Long) row[0], ((Number) row[1]).longValue());
            }
        }

        Set<Long> attendedPointCategoryIds = new HashSet<>(
                registrationJpaRepository.findAttendedPointCategoryIdsByUserId(user.getId(), attendedStatuses)
        );

        // 10. Score candidate events
        List<ScoredCandidate> scoredCandidates = new ArrayList<>();
        for (EventJpaEntity event : upcomingEvents) {
            // Filter 1: Student is already registered
            if (registeredEventIds.contains(event.getId())) {
                continue;
            }

            // Filter 2: Event is at full capacity (0 remaining slots)
            int capacity = event.getCapacity() != null ? event.getCapacity() : 0;
            long activeRegistrations = activeCountMap.getOrDefault(event.getId(), 0L);
            int remainingSlots = capacity - (int) activeRegistrations;
            if (remainingSlots <= 0) {
                continue;
            }

            // Filter 3: Days until event (urgency)
            LocalDate eventDate = event.getDateHappen() != null
                    ? event.getDateHappen().atZone(VIETNAM_ZONE).toLocalDate()
                    : today;
            long daysUntil = ChronoUnit.DAYS.between(today, eventDate);
            if (daysUntil < 0) {
                continue;
            }

            // Calculate Missing Training Points Gain
            int totalEffectiveGain = 0;
            List<PointCategoryGainDto> pointGains = new ArrayList<>();
            if (event.getEventPoints() != null) {
                for (EventPointJpaEntity ep : event.getEventPoints()) {
                    if (ep.getPointCategory() != null) {
                        Long catId = ep.getPointCategory().getId();
                        int offered = ep.getPoint() != null ? ep.getPoint() : 0;
                        int gap = missingPointGaps.getOrDefault(catId, 0);
                        int effective = Math.min(offered, gap);
                        if (effective > 0) {
                            totalEffectiveGain += effective;
                        }
                        pointGains.add(new PointCategoryGainDto(
                                catId,
                                ep.getPointCategory().getName(),
                                offered,
                                effective
                        ));
                    }
                }
            }
            // Normalized for university events (2 to 5 training points per event): totalEffectiveGain * 20.0
            double pointScore = Math.min(100.0, totalEffectiveGain * 20.0);

            // Calculate SV5T Criteria Match Score
            boolean fulfillsCriteria = false;
            String criteriaName = null;
            double criteriaScore = 0.0;
            if (event.getCriteria() != null) {
                Long critId = event.getCriteria().getId();
                criteriaName = event.getCriteria().getName();
                if (!completedCriteriaIds.contains(critId)) {
                    fulfillsCriteria = true;
                    criteriaScore = 100.0;
                }
            }

            // Calculate Fanpage Affinity Score (NEW)
            boolean isFollowedFanpage = false;
            double fanpageScore = 0.0;
            if (event.getFanpage() != null && affiliatedFanpageIds.contains(event.getFanpage().getId())) {
                isFollowedFanpage = true;
                fanpageScore = 100.0;
            }

            // Calculate Attendance History Affinity Score
            double eventTypeScore = 0.0;
            if (event.getEventType() != null) {
                long attendedCount = attendedEventTypeCounts.getOrDefault(event.getEventType().getId(), 0L);
                if (attendedCount >= 2) {
                    eventTypeScore = 60.0;
                } else if (attendedCount == 1) {
                    eventTypeScore = 40.0;
                }
            }

            double pointCategoryHistoryScore = 0.0;
            if (event.getEventPoints() != null) {
                boolean matchesPastCategory = event.getEventPoints().stream()
                        .filter(ep -> ep.getPointCategory() != null)
                        .anyMatch(ep -> attendedPointCategoryIds.contains(ep.getPointCategory().getId()));
                if (matchesPastCategory) {
                    pointCategoryHistoryScore = 40.0;
                }
            }
            double historyScore = Math.min(100.0, eventTypeScore + pointCategoryHistoryScore);

            // Calculate Urgency Score
            double urgencyScore;
            if (daysUntil <= 3) {
                urgencyScore = 100.0;
            } else if (daysUntil <= 7) {
                urgencyScore = 75.0;
            } else if (daysUntil <= 14) {
                urgencyScore = 50.0;
            } else if (daysUntil <= 30) {
                urgencyScore = 25.0;
            } else {
                urgencyScore = 10.0;
            }

            // Restructured base score (Sum = 100%):
            // 35% Points + 25% Criteria + 15% Fanpage + 15% History + 10% Urgency
            double baseScore = (pointScore * 0.35)
                    + (criteriaScore * 0.25)
                    + (fanpageScore * 0.15)
                    + (historyScore * 0.15)
                    + (urgencyScore * 0.10);

            scoredCandidates.add(new ScoredCandidate(
                    event,
                    baseScore,
                    pointScore,
                    criteriaScore,
                    fanpageScore,
                    historyScore,
                    urgencyScore,
                    remainingSlots,
                    daysUntil,
                    fulfillsCriteria,
                    criteriaName,
                    totalEffectiveGain,
                    pointGains,
                    isFollowedFanpage
            ));
        }

        if (scoredCandidates.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // 11. Sort by base score descending
        scoredCandidates.sort(Comparator.comparingDouble(ScoredCandidate::baseScore).reversed());

        // 12. Select Top candidates for AI evaluation and personalized explanation
        int topKCount = Math.min(10, scoredCandidates.size());
        List<ScoredCandidate> topCandidates = scoredCandidates.subList(0, topKCount);

        List<CandidateEventRecommendation> aiCandidatePayload = topCandidates.stream()
                .map(c -> new CandidateEventRecommendation(
                        c.event().getId(),
                        c.event().getName(),
                        c.event().getDescription(),
                        c.remainingSlots(),
                        c.daysUntil(),
                        c.totalEffectiveGain(),
                        c.fulfillsCriteria(),
                        c.criteriaName(),
                        c.baseScore(),
                        c.isFollowedFanpage(),
                        c.historyScore()
                ))
                .toList();

        StudentRecommendationContext aiContext = new StudentRecommendationContext(
                user.getId(),
                email,
                missingPointGaps,
                missingCriteriaIds
        );

        // Safe Resilient Fallback: Wrap AI call with try-catch
        List<AiRecommendationResult> aiResults;
        try {
            aiResults = aiPort.generateExplanations(aiContext, aiCandidatePayload);
        } catch (Exception ex) {
            log.warn("Gemini AI recommendation call failed: {}. Falling back to rule-based explanations.", ex.getMessage());
            aiResults = Collections.emptyList();
        }

        Map<Long, AiRecommendationResult> aiResultMap = (aiResults != null)
                ? aiResults.stream().filter(Objects::nonNull).collect(Collectors.toMap(AiRecommendationResult::eventId, r -> r, (e1, e2) -> e1))
                : Collections.emptyMap();

        // 13. Assemble final recommendations (Two-Stage Ranking: Top K AI Re-ranking + Non-Top K Heuristic Ranking)
        List<ScoredCandidate> remainingCandidates = scoredCandidates.subList(topKCount, scoredCandidates.size());

        List<EventRecommendationDto> topRecommendations = new ArrayList<>();
        for (ScoredCandidate c : topCandidates) {
            AiRecommendationResult aiRes = aiResultMap.get(c.event().getId());
            double aiScore = (aiRes != null) ? aiRes.aiScore() : c.baseScore();
            double finalScore = (c.baseScore() * 0.70) + (aiScore * 0.30);
            String explanation = (aiRes != null && aiRes.aiExplanation() != null && !aiRes.aiExplanation().isBlank())
                    ? aiRes.aiExplanation()
                    : buildTemplateExplanation(c);

            topRecommendations.add(new EventRecommendationDto(
                    eventDtoMapper.toInfoDto(c.event()),
                    Math.round(finalScore * 10.0) / 10.0,
                    explanation,
                    c.remainingSlots(),
                    c.daysUntil(),
                    c.fulfillsCriteria(),
                    c.pointGains(),
                    c.isFollowedFanpage(),
                    Math.round(c.fanpageScore() * 10.0) / 10.0,
                    Math.round(c.historyScore() * 10.0) / 10.0
            ));
        }

        // Re-rank Top K recommendations among themselves by AI-blended score descending
        topRecommendations.sort(Comparator.comparingDouble(EventRecommendationDto::matchScore).reversed());

        // Process remaining candidates (11+): Ensure they remain below Top K recommendations
        double minTopScore = topRecommendations.isEmpty() ? 100.0 : topRecommendations.get(topRecommendations.size() - 1).matchScore();
        List<EventRecommendationDto> remainingRecommendations = new ArrayList<>();
        for (ScoredCandidate c : remainingCandidates) {
            // Cap score smoothly so non-Top-K candidates do not surpass AI-evaluated top candidates
            double cappedScore = Math.min(c.baseScore(), Math.max(0.0, minTopScore - 0.1));
            String explanation = buildTemplateExplanation(c);

            remainingRecommendations.add(new EventRecommendationDto(
                    eventDtoMapper.toInfoDto(c.event()),
                    Math.round(cappedScore * 10.0) / 10.0,
                    explanation,
                    c.remainingSlots(),
                    c.daysUntil(),
                    c.fulfillsCriteria(),
                    c.pointGains(),
                    c.isFollowedFanpage(),
                    Math.round(c.fanpageScore() * 10.0) / 10.0,
                    Math.round(c.historyScore() * 10.0) / 10.0
            ));
        }

        // Sort remaining candidates among themselves by base score descending
        remainingRecommendations.sort(Comparator.comparingDouble(EventRecommendationDto::matchScore).reversed());

        // Concatenate: Top K (AI evaluated & re-ranked) followed by remaining candidates
        List<EventRecommendationDto> finalRecommendations = new ArrayList<>(topRecommendations);
        finalRecommendations.addAll(remainingRecommendations);

        // 14. Cache final recommendations for subsequent pagination calls (TTL 30 min)
        recommendationCacheService.put(user.getId(), finalRecommendations);

        // 15. Paginate and return
        return paginate(finalRecommendations, pageable);
    }

    private Page<EventRecommendationDto> paginate(List<EventRecommendationDto> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<EventRecommendationDto> pageList = (start < list.size())
                ? list.subList(start, end)
                : Collections.emptyList();
        return new PageImpl<>(pageList, pageable, list.size());
    }

    private SemesterJpaEntity resolveActiveSemester() {
        return semesterJpaRepository.findByStatus(SemesterStatusEnum.ONGOING)
                .orElse(null);
    }

    private AcademicYearJpaEntity resolveActiveAcademicYear(LocalDate today) {
        return academicYearJpaRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIsActiveTrue(today, today)
                .orElse(null);
    }

    private Map<Long, Integer> fetchStudentSemesterPoints(Long userId, SemesterJpaEntity activeSemester) {
        Map<Long, Integer> pointsMap = new HashMap<>();
        if (activeSemester == null) {
            return pointsMap;
        }

        List<StudentSemesterPointJpaEntity> pointsList =
                studentSemesterPointJpaRepository.findByUserIdAndSemesterId(userId, activeSemester.getId());
        for (StudentSemesterPointJpaEntity p : pointsList) {
            if (p.getPointCategory() != null && p.getTotalPoint() != null) {
                pointsMap.put(p.getPointCategory().getId(), p.getTotalPoint());
            }
        }
        return pointsMap;
    }

    private Map<Long, Integer> calculatePointGaps(
            List<PointCategoryJpaEntity> allCategories,
            Map<Long, Integer> studentPointsMap
    ) {
        Map<Long, Integer> gaps = new HashMap<>();
        for (PointCategoryJpaEntity cat : allCategories) {
            int maxPoint = cat.getMaximum() != null ? cat.getMaximum() : 0;
            int currentPoint = studentPointsMap.getOrDefault(cat.getId(), 0);
            int deficit = Math.max(0, maxPoint - currentPoint);
            gaps.put(cat.getId(), deficit);
        }
        return gaps;
    }

    private Set<Long> fetchCompletedCriteriaIds(Long userId, AcademicYearJpaEntity academicYear) {
        Set<Long> completedIds = new HashSet<>();
        if (academicYear == null) {
            return completedIds;
        }

        List<StudentCriteriaProgressJpaEntity> progressList =
                studentCriteriaProgressJpaRepository.findAllByUserIdAndAcademicYearId(userId, academicYear.getId());
        for (StudentCriteriaProgressJpaEntity p : progressList) {
            if (p.getCriteria() != null && Boolean.TRUE.equals(p.getIsCompleted())) {
                completedIds.add(p.getCriteria().getId());
            }
        }
        return completedIds;
    }

    private String buildTemplateExplanation(ScoredCandidate c) {
        StringBuilder sb = new StringBuilder();
        if (c.totalEffectiveGain() > 0 && c.fulfillsCriteria()) {
            sb.append(String.format("Giúp bạn tích lũy thêm %d điểm rèn luyện và hoàn thành tiêu chí %s.",
                    c.totalEffectiveGain(), c.criteriaName() != null ? c.criteriaName() : "SV5T"));
        } else if (c.totalEffectiveGain() > 0) {
            sb.append(String.format("Giúp bạn bù đắp %d điểm rèn luyện còn thiếu trong học kỳ này.",
                    c.totalEffectiveGain()));
        } else if (c.fulfillsCriteria()) {
            sb.append(String.format("Giúp bạn đạt tiêu chí %s cho danh hiệu Sinh viên 5 tốt.",
                    c.criteriaName() != null ? c.criteriaName() : "SV5T"));
        } else {
            sb.append("Sự kiện phù hợp để tích lũy kinh nghiệm và mở rộng kết nối.");
        }

        if (c.daysUntil() <= 3 && c.remainingSlots() > 0) {
            sb.append(String.format(" Sự kiện diễn ra trong %d ngày tới và chỉ còn %d slot!",
                    c.daysUntil(), c.remainingSlots()));
        } else if (c.remainingSlots() > 0) {
            sb.append(String.format(" Hiện còn %d slot trống.", c.remainingSlots()));
        }

        if (c.isFollowedFanpage()) {
            sb.append(" Thuộc CLB/Đơn vị bạn đang theo dõi.");
        }

        return sb.toString().trim();
    }

    private record ScoredCandidate(
            EventJpaEntity event,
            double baseScore,
            double pointScore,
            double criteriaScore,
            double fanpageScore,
            double historyScore,
            double urgencyScore,
            int remainingSlots,
            long daysUntil,
            boolean fulfillsCriteria,
            String criteriaName,
            int totalEffectiveGain,
            List<PointCategoryGainDto> pointGains,
            boolean isFollowedFanpage
    ) {}
}
