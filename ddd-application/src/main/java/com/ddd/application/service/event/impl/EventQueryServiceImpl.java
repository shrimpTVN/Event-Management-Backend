package com.ddd.application.service.event.impl;

import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.dto.event.EventSearchCriteriaDto;
import com.ddd.application.mapper.EventDtoMapper;
import com.ddd.application.service.event.EventQueryService;
import com.ddd.application.service.event.DailyEventRecommendationService;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import org.springframework.transaction.annotation.Propagation;
import com.ddd.application.dto.recommendation.EventRecommendationDto;
import com.ddd.application.utils.ServiceUtil;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.EventJpaRepository;
import com.ddd.infrastructure.repository.specification.EventSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventQueryServiceImpl implements EventQueryService {

    private final EventJpaRepository eventJpaRepository;
    private final EventDtoMapper eventDtoMapper;
    private final EventResponseEnricher responseEnricher;
    private final DailyEventRecommendationService eventRecommendationService;
    private final SecurityContextPort security;

    @Override
    public EventInfoDto getEventById(Long id) {
        EventJpaEntity eventJpaEntity = eventJpaRepository.findWithDetailsByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));

        return responseEnricher.enrich(eventDtoMapper.toInfoDto(eventJpaEntity), security.getCurrentUserId());
    }

    @Override
    public Page<EventInfoDto> getAllEventsByFanpageId(Long fanpageId, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = ServiceUtil.createPageable(page, size, sortBy, sortDir);
        Page<EventInfoDto> dtoPage = eventJpaRepository.findWithDetailsByFanpage_IdAndIsActiveTrue(fanpageId, pageable)
                .map(eventDtoMapper::toInfoDto);
        return responseEnricher.enrich(dtoPage, security.getCurrentUserId());
    }

    @Override
    public Page<EventInfoDto> getAllEvents(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = ServiceUtil.createPageable(page, size, sortBy, sortDir);
        Page<EventInfoDto> dtoPage = eventJpaRepository.findAll(pageable)
                .map(eventDtoMapper::toInfoDto);
        return responseEnricher.enrich(dtoPage, security.getCurrentUserId());
    }

    @Override
    public Page<EventInfoDto> searchAndFilterEvents(EventSearchCriteriaDto criteria) {
        Pageable pageable = ServiceUtil.createPageable(criteria.page(), criteria.size(),
                criteria.sortBy(), criteria.sortDir());
        Specification<EventJpaEntity> spec = EventSpecification.withFilter(
                criteria.keyword(),
                criteria.status(),
                criteria.eventTypeIds(),
                criteria.semesterId(),
                criteria.schoolId(),
                criteria.criteriaIds(),
                criteria.pointCategoryIds(),
                criteria.dateHappenFrom(),
                criteria.dateHappenTo(),
                criteria.isActive()
        );
        Page<EventInfoDto> dtoPage = eventJpaRepository.findAll(spec, pageable).map(eventDtoMapper::toInfoDto);
        return responseEnricher.enrich(dtoPage, security.getCurrentUserId());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Page<EventInfoDto> getRecommendEvents(String email) {
        if (email == null || !email.equals(security.getCurrentUserEmail())) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return eventRecommendationService.getRecommendations(PageRequest.of(0, 10))
                .map(EventRecommendationDto::event);
    }

}
