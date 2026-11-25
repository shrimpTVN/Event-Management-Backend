package com.ddd.infrastructure.repository;

import com.ddd.domain.model.Registration;
import com.ddd.infrastructure.entity.RegistrationId;
import com.ddd.infrastructure.entity.RegistrationJpaEntity;
import com.ddd.infrastructure.mapper.RegistrationMapper;
import com.ddd.infrastructure.repository.jpaRepository.RegistrationJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationRepositoryImplTest {
    @Mock private RegistrationJpaRepository registrationJpaRepository;
    @Mock private RegistrationMapper registrationMapper;
    @Mock private EntityManager entityManager;

    @InjectMocks private RegistrationRepositoryImpl repository;

    @Test
    void countsOnlyActiveRegistrationsForTheRequestedEvent() {
        when(registrationJpaRepository.countActiveRegistrationsByEventId(3L)).thenReturn(2L);
        when(registrationJpaRepository.countActiveRegistrationsByEventId(4L)).thenReturn(0L);

        assertEquals(2L, repository.countActiveRegistrationsByEventId(3L));
        assertEquals(0L, repository.countActiveRegistrationsByEventId(4L));
        verify(registrationJpaRepository).countActiveRegistrationsByEventId(4L);
    }

    @Test
    void countsOnlyActiveRegistrationsForTheRequestedEventAndGender() {
        when(registrationJpaRepository.countActiveRegistrationsByEventIdAndGender(3L, "MALE")).thenReturn(5L);
        when(registrationJpaRepository.countActiveRegistrationsByEventIdAndGender(3L, "FEMALE")).thenReturn(0L);

        assertEquals(5L, repository.countActiveRegistrationsByEventIdAndGender(3L, "MALE"));
        assertEquals(0L, repository.countActiveRegistrationsByEventIdAndGender(3L, "FEMALE"));
        verify(registrationJpaRepository).countActiveRegistrationsByEventIdAndGender(3L, "FEMALE");
    }

    @Test
    void save_whenAlreadyExists_shouldUpdateWithoutFetchingNewReferences() {
        Registration reg = new Registration();
        reg.setUserId(1L);
        reg.setEventId(2L);
        RegistrationJpaEntity existing = new RegistrationJpaEntity();
        existing.setId(new RegistrationId(1L, 2L));

        when(registrationJpaRepository.findById(new RegistrationId(1L, 2L)))
                .thenReturn(Optional.of(existing));

        repository.save(reg);

        verify(registrationJpaRepository).findById(new RegistrationId(1L, 2L));
        verify(registrationMapper).updateEntityFromDomain(reg, existing);
        verify(registrationJpaRepository).save(existing);
        verifyNoInteractions(entityManager);
    }
}
