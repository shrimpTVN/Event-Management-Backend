package com.ddd.application.service.event.impl;

import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Event;
import com.ddd.domain.model.StoredEvent;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.EventRepository;
import com.ddd.domain.repository.StoredEventRepository;
import com.ddd.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoredEventCommandServiceImplTest {

    @Mock
    private StoredEventRepository storedEventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private StoredEventCommandServiceImpl storedEventCommandService;

    @Test
    @DisplayName("Should successfully store event when user and event exist and not already stored")
    void storeEvent_whenValid_shouldSaveStoredEvent() {
        String email = "student@university.edu.vn";
        Long eventId = 100L;
        Long userId = 10L;

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        Event event = new Event();
        event.setId(eventId);
        event.setActive(true);

        when(userRepository.findByEmail(email)).thenReturn(user);
        when(eventRepository.findById(eventId)).thenReturn(event);
        when(storedEventRepository.existsByUserIdAndEventId(userId, eventId)).thenReturn(false);

        storedEventCommandService.storeEvent(eventId, email);

        ArgumentCaptor<StoredEvent> captor = ArgumentCaptor.forClass(StoredEvent.class);
        verify(storedEventRepository).save(captor.capture());
        StoredEvent saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals(eventId, saved.getEventId());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user is not found during store")
    void storeEvent_whenUserNotFound_shouldThrowResourceNotFoundException() {
        String email = "unknown@university.edu.vn";
        Long eventId = 100L;

        when(userRepository.findByEmail(email)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () ->
                storedEventCommandService.storeEvent(eventId, email));

        verify(eventRepository, never()).findById(any());
        verify(storedEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when event is not found or inactive")
    void storeEvent_whenEventNotFoundOrInactive_shouldThrowResourceNotFoundException() {
        String email = "student@university.edu.vn";
        Long eventId = 100L;
        Long userId = 10L;

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(user);
        when(eventRepository.findById(eventId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () ->
                storedEventCommandService.storeEvent(eventId, email));

        Event inactiveEvent = new Event();
        inactiveEvent.setId(eventId);
        inactiveEvent.setActive(false);
        when(eventRepository.findById(eventId)).thenReturn(inactiveEvent);

        assertThrows(ResourceNotFoundException.class, () ->
                storedEventCommandService.storeEvent(eventId, email));

        verify(storedEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when event is already stored")
    void storeEvent_whenAlreadyStored_shouldThrowDuplicateResourceException() {
        String email = "student@university.edu.vn";
        Long eventId = 100L;
        Long userId = 10L;

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        Event event = new Event();
        event.setId(eventId);
        event.setActive(true);

        when(userRepository.findByEmail(email)).thenReturn(user);
        when(eventRepository.findById(eventId)).thenReturn(event);
        when(storedEventRepository.existsByUserIdAndEventId(userId, eventId)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
                storedEventCommandService.storeEvent(eventId, email));

        verify(storedEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully unstore event when stored record exists")
    void unstoreEvent_whenValid_shouldDeleteStoredEvent() {
        String email = "student@university.edu.vn";
        Long eventId = 100L;
        Long userId = 10L;

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(user);
        when(storedEventRepository.existsByUserIdAndEventId(userId, eventId)).thenReturn(true);

        storedEventCommandService.unstoreEvent(eventId, email);

        verify(storedEventRepository).deleteByUserIdAndEventId(userId, eventId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found during unstore")
    void unstoreEvent_whenUserNotFound_shouldThrowResourceNotFoundException() {
        String email = "unknown@university.edu.vn";
        Long eventId = 100L;

        when(userRepository.findByEmail(email)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () ->
                storedEventCommandService.unstoreEvent(eventId, email));

        verify(storedEventRepository, never()).deleteByUserIdAndEventId(any(), any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when un-storing an event that is not stored")
    void unstoreEvent_whenNotStored_shouldThrowResourceNotFoundException() {
        String email = "student@university.edu.vn";
        Long eventId = 100L;
        Long userId = 10L;

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(user);
        when(storedEventRepository.existsByUserIdAndEventId(userId, eventId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                storedEventCommandService.unstoreEvent(eventId, email));

        verify(storedEventRepository, never()).deleteByUserIdAndEventId(any(), any());
    }
}
