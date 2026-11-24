package com.ddd.application.service.event.impl;

import com.ddd.application.dto.recommendation.EventRecommendationDto;
import com.ddd.application.service.event.DailyEventRecommendationService;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DailyEventRecommendationServiceImpl implements DailyEventRecommendationService {
    private final SecurityContextPort security;
    private final RecommendationRefreshService refresh;
    private final RecommendationPageReader reader;
    private final Clock clock;

    @Override
    public Page<EventRecommendationDto> getRecommendations(Pageable page) {
        Long studentId = security.getCurrentUserId();
        if (studentId == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        var now = clock.instant();
        refresh.prepare(studentId, now);
        return reader.read(studentId, page, now);
    }
}
