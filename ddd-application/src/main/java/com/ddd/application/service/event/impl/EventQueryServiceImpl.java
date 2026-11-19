package com.ddd.application.service.event.impl;

import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.dto.event.EventSearchCriteriaDto;
import com.ddd.application.mapper.EventDtoMapper;
import com.ddd.application.service.event.EventQueryService;
import com.ddd.application.utils.ServiceUtil;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.EventJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.RegistrationJpaRepository;
import com.ddd.infrastructure.repository.specification.EventSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventQueryServiceImpl implements EventQueryService {

    private final EventJpaRepository eventJpaRepository;
    private final EventDtoMapper eventDtoMapper;
    private final RegistrationJpaRepository registrationJpaRepository;

    @Override
    public EventInfoDto getEventById(Long id) {
        EventJpaEntity eventJpaEntity = eventJpaRepository.findWithDetailsByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));

        EventInfoDto dto = eventDtoMapper.toInfoDto(eventJpaEntity);
        Long count = registrationJpaRepository.countActiveRegistrationsByEventId(id);
        return dto.withRegisteredCount(count != null ? count : 0L);
    }

    @Override
    public Page<EventInfoDto> getAllEventsByFanpageId(Long fanpageId, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = ServiceUtil.createPageable(page, size, sortBy, sortDir);
        Page<EventInfoDto> dtoPage = eventJpaRepository.findWithDetailsByFanpage_IdAndIsActiveTrue(fanpageId, pageable)
                .map(eventDtoMapper::toInfoDto);
        return populateRegisteredCounts(dtoPage);
    }

    @Override
    public Page<EventInfoDto> getAllEvents(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = ServiceUtil.createPageable(page, size, sortBy, sortDir);
        Page<EventInfoDto> dtoPage = eventJpaRepository.findAll(pageable)
                .map(eventDtoMapper::toInfoDto);
        return populateRegisteredCounts(dtoPage);
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
                criteria.criteriaIds(),
                criteria.pointCategoryIds(),
                criteria.dateHappenFrom(),
                criteria.dateHappenTo(),
                criteria.isActive()
        );
        Page<EventInfoDto> dtoPage = eventJpaRepository.findAll(spec, pageable).map(eventDtoMapper::toInfoDto);
        return populateRegisteredCounts(dtoPage);
    }

    @Override
    public Page<EventInfoDto> getRecommendEvents(String email) {
        return null;
    }

    private Page<EventInfoDto> populateRegisteredCounts(Page<EventInfoDto> page) {
        if (page.isEmpty()) {
            return page;
        }
        List<Long> eventIds = page.map(EventInfoDto::id).toList();
        List<Object[]> countResults = registrationJpaRepository.countActiveRegistrationsByEventIds(eventIds);
        Map<Long, Long> countMap = countResults.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).longValue(),
                        (existing, replacement) -> existing
                ));

        return page.map(dto -> dto.withRegisteredCount(countMap.getOrDefault(dto.id(), 0L)));
    }
}
