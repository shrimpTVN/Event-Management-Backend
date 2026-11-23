package com.ddd.application.service.event.impl;

import com.ddd.application.dto.recommendation.EventRecommendationDto;
import com.ddd.application.mapper.EventDtoMapper;
import com.ddd.application.mapper.RecommendationDtoMapper;
import com.ddd.domain.model.recommendation.RecommendationFeatures.Ranked;
import com.ddd.domain.model.recommendation.RecommendationScoringPolicy;
import com.ddd.domain.repository.RecommendationRepository;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.EventJpaRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import static com.ddd.application.service.event.impl.RecommendationRefreshService.VIETNAM;

@Service
@RequiredArgsConstructor
public class RecommendationPageReader {
    private final RecommendationRepository repository;
    private final EventJpaRepository events;
    private final EventDtoMapper eventMapper;
    private final RecommendationDtoMapper recommendationMapper;
    private final RecommendationScoringPolicy policy = new RecommendationScoringPolicy();

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<EventRecommendationDto> read(long studentId, Pageable page, Instant now) {
        var today = now.atZone(VIETNAM).toLocalDate();
        var ranked = repository.findCandidates(studentId, now, today.atStartOfDay(VIETNAM).toInstant())
                .stream().map(event -> policy.rank(event, now)).sorted(RecommendationScoringPolicy.ORDER).toList();
        int from = page.isPaged() ? (int) Math.min(page.getOffset(), ranked.size()) : 0;
        int to = page.isPaged() ? (int) Math.min((long) from + page.getPageSize(), ranked.size()) : ranked.size();
        var selected = ranked.subList(from, to);
        if (selected.isEmpty()) {
            return new PageImpl<>(java.util.List.of(), page, ranked.size());
        }
        var ids = selected.stream().map(Ranked::id).toList();
        var byId = events.findByIdInAndIsActiveTrue(ids).stream()
                .collect(Collectors.toMap(EventJpaEntity::getId, Function.identity()));
        var results = new ArrayList<EventRecommendationDto>(selected.size());
        for (Ranked event : selected) {
            var entity = byId.get(event.id());
            if (entity != null) {
                long days = ChronoUnit.DAYS.between(today, event.candidate().happensAt().atZone(VIETNAM).toLocalDate());
                results.add(recommendationMapper.toDto(event, eventMapper.toInfoDto(entity), days));
            }
        }
        return new PageImpl<>(results, page, ranked.size());
    }
}
