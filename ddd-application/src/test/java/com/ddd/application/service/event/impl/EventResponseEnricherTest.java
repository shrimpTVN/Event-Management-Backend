package com.ddd.application.service.event.impl;

import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.domain.enums.FollowingStatusEnum;
import com.ddd.infrastructure.repository.jpaRepository.FollowingJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.RegistrationJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventResponseEnricherTest {
    private static final Instant NOW = Instant.parse("2026-11-24T03:00:00Z");
    private final RegistrationJpaRepository registrations = mock(RegistrationJpaRepository.class);
    private final FollowingJpaRepository following = mock(FollowingJpaRepository.class);
    private final EventResponseEnricher enricher =
            new EventResponseEnricher(registrations, following, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void enrichesSignedInPageWithOneQueryPerDataSourceAndPreservesPaging() {
        var pageable = PageRequest.of(1, 2);
        var events = new PageImpl<>(List.of(event(42L, 14L, NOW), event(57L, null, NOW.plusSeconds(1))), pageable, 9);
        when(registrations.countActiveRegistrationsByEventIds(List.of(42L, 57L)))
                .thenReturn(List.<Object[]>of(new Object[]{42L, 12L}));
        when(following.findFollowedFanpageIdsByUserId(9L, FollowingStatusEnum.FOLLOWING))
                .thenReturn(Set.of(14L));

        var result = enricher.enrich(events, 9L);

        assertEquals(pageable, result.getPageable());
        assertEquals(9, result.getTotalElements());
        var first = result.getContent().get(0);
        assertEquals(12L, first.registeredCount());
        assertTrue(first.isFollowFanpage());
        assertTrue(first.isFull());
        assertTrue(first.registrationClosed());
        var second = result.getContent().get(1);
        assertEquals(57L, second.id());
        assertEquals(0L, second.registeredCount());
        assertFalse(second.isFollowFanpage());
        assertFalse(second.isFull());
        assertFalse(second.registrationClosed());
        verify(registrations).countActiveRegistrationsByEventIds(List.of(42L, 57L));
        verify(following).findFollowedFanpageIdsByUserId(9L, FollowingStatusEnum.FOLLOWING);
        verifyNoMoreInteractions(registrations, following);
    }

    @Test
    void enrichesAnonymousDetailWithoutLookingUpFollows() {
        when(registrations.countActiveRegistrationsByEventId(42L)).thenReturn(11L);
        var result = enricher.enrich(event(42L, 14L, NOW.minusSeconds(1)), null);

        assertEquals(11L, result.registeredCount());
        assertFalse(result.isFollowFanpage());
        assertFalse(result.isFull());
        assertTrue(result.registrationClosed());
        verifyNoInteractions(following);
    }

    private EventInfoDto event(long id, Long fanpageId, Instant close) {
        return new EventInfoDto(id, "Volunteering", "Help out", "Campus hall",
                NOW.minusSeconds(3600), close, NOW.plusSeconds(3600), 12, 6, 6,
                "banner.png", "PUBLISHED", 10L, "Volunteer", 11L, "Service",
                "Campus volunteers", fanpageId, true, true, null, NOW, List.of());
    }
}
