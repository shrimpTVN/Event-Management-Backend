package com.ddd.application.service.event.impl;

import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.domain.enums.FollowingStatusEnum;
import com.ddd.infrastructure.repository.jpaRepository.FollowingJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.RegistrationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventResponseEnricher {
    private final RegistrationJpaRepository registrations;
    private final FollowingJpaRepository following;
    private final Clock clock;

    public EventInfoDto enrich(EventInfoDto event, Long userId) {
        var now = clock.instant();
        Long count = registrations.countActiveRegistrationsByEventId(event.id());
        return withRegistrationInfo(event, count == null ? 0 : count, followedFanpages(userId), now);
    }

    public Page<EventInfoDto> enrich(Page<EventInfoDto> page, Long userId) {
        if (page.isEmpty()) {
            return page;
        }
        var now = clock.instant();
        var ids = page.getContent().stream().map(EventInfoDto::id).toList();
        var counts = registrations.countActiveRegistrationsByEventIds(ids).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
        var followed = followedFanpages(userId);
        return page.map(event -> withRegistrationInfo(event, counts.getOrDefault(event.id(), 0L), followed, now));
    }

    private Set<Long> followedFanpages(Long userId) {
        return userId == null ? Set.of()
                : following.findFollowedFanpageIdsByUserId(userId, FollowingStatusEnum.FOLLOWING);
    }

    private EventInfoDto withRegistrationInfo(EventInfoDto event, long count, Set<Long> followed, Instant now) {
        boolean followsFanpage = event.fanpageId() != null && followed.contains(event.fanpageId());
        boolean full = event.capacity() != null && count >= event.capacity();
        boolean closed = event.dateClose() != null && !now.isBefore(event.dateClose());
        return event.withRegistrationInfo(count, followsFanpage, full, closed);
    }
}
