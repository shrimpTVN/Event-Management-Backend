package com.ddd.application.service.event.impl;

import com.ddd.application.dto.event.EventCreateDto;
import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.mapper.EventDtoMapper;
import com.ddd.application.service.event.EventCommandService;
import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.*;
import com.ddd.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventCommandServiceImpl implements EventCommandService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final FanpageMemberRepository fanpageMemberRepository;
    private final FanpageRepository fanpageRepository;
    private final EventTypeRepository eventTypeRepository;
    private final CriteriaRepository criteriaRepository;
    private final EventDtoMapper eventDtoMapper;
    private final EventPointRepository eventPointRepository;

    @Override
    @Transactional
    public void createEvent(EventCreateDto dto, String email) {
        log.info("Creating event for fanpageId: {} by user email: {}", dto.fanpageId(), email);

        Fanpage fanpage = fanpageRepository.findById(dto.fanpageId())
                .orElseThrow(() -> new ResourceNotFoundException("Fanpage not found with id: " + dto.fanpageId()));

        EventType eventType = eventTypeRepository.findById(dto.eventTypeId());
        if (eventType == null) {
            throw new ResourceNotFoundException("EventType not found with id: " + dto.eventTypeId());
        }

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        //Verify user is an active member of the target fanpage
        FanpageMember fanpageMember = fanpageMemberRepository.findById(fanpage.getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this fanpage"));
        if (!fanpageMember.isActive()) {
            throw new AccessDeniedException("User no longer has active membership in this fanpage");
        }

        if (dto.criteriaId() != null) {
            Criteria criteria = criteriaRepository.findById(dto.criteriaId());
            if (criteria == null) {
                throw new ResourceNotFoundException("Criteria not found with id: " + dto.criteriaId());
            }
        }

        // Validate dates
        validateEventDates(dto.dateOpen(), dto.dateClose(), dto.dateHappen());

        //Validate capacity & gender quotas
        int male = dto.maleQuantity() != null ? dto.maleQuantity() : 0;
        int female = dto.femaleQuantity() != null ? dto.femaleQuantity() : 0;
        validateCapacityAndQuantities(dto.capacity(), male, female);

        //Build domain Event with default DRAFT status
        Event event = eventDtoMapper.toEvent(dto);
        event.setMaleQuantity(male);
        event.setFemaleQuantity(female);
        event.setStatus(EventStatusEnum.DRAFT.name());

        Event savedEvent = eventRepository.save(event);
        log.info("Event created successfully with id: {} and status: {}", savedEvent.getId(), savedEvent.getStatus());

        // Save event points
        List<EventPoint> eventPoints = eventDtoMapper.toEventPoints(dto.eventPoints());
       eventPoints =  eventPoints.stream().peek(ep -> ep.setEventId(savedEvent.getId())).toList();
        eventPointRepository.saveAll(eventPoints);
    }

    private void validateEventDates(LocalDate dateOpen, LocalDate dateClose, LocalDate dateHappen) {
        LocalDate today = LocalDate.now();
        if (dateOpen.isBefore(today)) {
            throw new IllegalArgumentException("Registration open date cannot be in the past");
        }
        if (dateOpen.isAfter(dateClose)) {
            throw new IllegalArgumentException("Registration open date must be before or equal to registration close date");
        }
        if (dateClose.isAfter(dateHappen)) {
            throw new IllegalArgumentException("Registration close date must be before or equal to event happen date");
        }
    }

    private void validateCapacityAndQuantities(Integer capacity, int maleQuantity, int femaleQuantity) {
        if (maleQuantity < 0 || femaleQuantity < 0) {
            throw new IllegalArgumentException("Male and female quantities must be non-negative");
        }
        if (maleQuantity + femaleQuantity > capacity) {
            throw new IllegalArgumentException(String.format(
                    "Total male and female quantities (%d) cannot exceed event capacity (%d)",
                    maleQuantity + femaleQuantity, capacity
            ));
        }
    }
}