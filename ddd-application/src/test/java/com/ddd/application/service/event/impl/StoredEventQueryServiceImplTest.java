package com.ddd.application.service.event.impl;

import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.dto.event.StoredEventStatusDto;
import com.ddd.application.mapper.EventDtoMapper;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.StoredEventJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoredEventQueryServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoredEventJpaRepository storedEventJpaRepository;

    @Mock
    private EventDtoMapper eventDtoMapper;

    @InjectMocks
    private StoredEventQueryServiceImpl storedEventQueryService;

    @Test
    @DisplayName("Should return stored status true when event is stored by user")
    void isEventStored_whenStored_shouldReturnTrue() {
        String email = "student@university.edu.vn";
        Long eventId = 100L;
        Long userId = 10L;

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(user);
        when(storedEventJpaRepository.existsByUser_IdAndEvent_Id(userId, eventId)).thenReturn(true);

        StoredEventStatusDto result = storedEventQueryService.isEventStored(eventId, email);

        assertNotNull(result);
        assertEquals(eventId, result.eventId());
        assertTrue(result.isStored());
    }

    @Test
    @DisplayName("Should return stored status false when event is not stored by user")
    void isEventStored_whenNotStored_shouldReturnFalse() {
        String email = "student@university.edu.vn";
        Long eventId = 100L;
        Long userId = 10L;

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(user);
        when(storedEventJpaRepository.existsByUser_IdAndEvent_Id(userId, eventId)).thenReturn(false);

        StoredEventStatusDto result = storedEventQueryService.isEventStored(eventId, email);

        assertNotNull(result);
        assertEquals(eventId, result.eventId());
        assertFalse(result.isStored());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found in isEventStored")
    void isEventStored_whenUserNotFound_shouldThrowResourceNotFoundException() {
        String email = "unknown@university.edu.vn";
        Long eventId = 100L;

        when(userRepository.findByEmail(email)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () ->
                storedEventQueryService.isEventStored(eventId, email));
    }

    @Test
    @DisplayName("Should return paginated stored events for student")
    void getStoredEvents_whenUserFound_shouldReturnPagedEvents() {
        String email = "student@university.edu.vn";
        Long userId = 10L;
        Pageable pageable = PageRequest.of(0, 10);

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        EventJpaEntity eventJpa = new EventJpaEntity();
        eventJpa.setId(100L);
        eventJpa.setName("Coding Contest");

        EventInfoDto eventInfoDto = new EventInfoDto(
                100L,
                "Coding Contest",
                "description",
                "location",
                null,
                null,
                null,
                100,
                50,
                50,
                "banner",
                "ACTIVE",
                "Type",
                "Criteria",
                "Fanpage",
                1L,
                true,
                null,
                null,
                null
        );

        Page<EventJpaEntity> entityPage = new PageImpl<>(List.of(eventJpa), pageable, 1);

        when(userRepository.findByEmail(email)).thenReturn(user);
        when(storedEventJpaRepository.findStoredEventsByUserId(userId, pageable)).thenReturn(entityPage);
        when(eventDtoMapper.toInfoDto(eventJpa)).thenReturn(eventInfoDto);

        Page<EventInfoDto> result = storedEventQueryService.getStoredEvents(email, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(100L, result.getContent().get(0).id());
        assertEquals("Coding Contest", result.getContent().get(0).name());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found in getStoredEvents")
    void getStoredEvents_whenUserNotFound_shouldThrowResourceNotFoundException() {
        String email = "unknown@university.edu.vn";
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findByEmail(email)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () ->
                storedEventQueryService.getStoredEvents(email, pageable));
    }
}
