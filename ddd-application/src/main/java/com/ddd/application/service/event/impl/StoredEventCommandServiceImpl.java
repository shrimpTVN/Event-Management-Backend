package com.ddd.application.service.event.impl;

import com.ddd.application.service.event.StoredEventCommandService;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Event;
import com.ddd.domain.model.StoredEvent;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.EventRepository;
import com.ddd.domain.repository.StoredEventRepository;
import com.ddd.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoredEventCommandServiceImpl implements StoredEventCommandService {

    private final StoredEventRepository storedEventRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public void storeEvent(Long eventId, String email) {
        log.info("Storing event id: {} for user email: {}", eventId, email);

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        Event event = eventRepository.findById(eventId);
        if (event == null || !event.isActive()) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
        }

        boolean alreadyStored = storedEventRepository.existsByUserIdAndEventId(user.getId(), eventId);
        if (alreadyStored) {
            throw new DuplicateResourceException("Event is already stored by user");
        }

        StoredEvent storedEvent = new StoredEvent(user.getId(), eventId);
        storedEventRepository.save(storedEvent);
        log.info("Event id: {} stored successfully for user id: {}", eventId, user.getId());
    }

    @Override
    @Transactional
    public void unstoreEvent(Long eventId, String email) {
        log.info("Unstoring event id: {} for user email: {}", eventId, email);

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        boolean exists = storedEventRepository.existsByUserIdAndEventId(user.getId(), eventId);
        if (!exists) {
            throw new ResourceNotFoundException("Stored event not found for event id: " + eventId);
        }

        storedEventRepository.deleteByUserIdAndEventId(user.getId(), eventId);
        log.info("Event id: {} unstored successfully for user id: {}", eventId, user.getId());
    }
}
