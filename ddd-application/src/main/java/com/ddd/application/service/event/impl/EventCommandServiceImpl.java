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
    private final SemesterRepository semesterRepository;

    @Override
    @Transactional
    public void createEvent(EventCreateDto dto, String email) {
        log.info("Creating event for fanpageId: {} by user email: {}", dto.fanpageId(), email);
        validateReferences(dto, email);

        // Map DTO to Event entity
        Event event = eventDtoMapper.toEvent(dto);
        event.setStatus(EventStatusEnum.DRAFT.name());

        int male = dto.maleQuantity() != null ? dto.maleQuantity() : 0;
        int female = dto.femaleQuantity() != null ? dto.femaleQuantity() : 0;
        event.setMaleQuantity(male);
        event.setFemaleQuantity(female);
        event.setCapacity(dto.capacity());

        // Validate dates and quantities
        event.validateEventDates();
        event.validateCapacityAndQuantities();

        Event savedEvent = eventRepository.save(event);
        log.info("Event created successfully with id: {} and status: {}", savedEvent.getId(), savedEvent.getStatus());
    }

    private void validateReferences(EventCreateDto dto, String email) {
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

        if (dto.semesterId() != null) {
            semesterRepository.findById(dto.semesterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + dto.semesterId()));
        }
    }
}