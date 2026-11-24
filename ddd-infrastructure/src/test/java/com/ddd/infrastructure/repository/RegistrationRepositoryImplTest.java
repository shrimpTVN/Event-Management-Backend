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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationRepositoryImplTest {
    @Mock private RegistrationJpaRepository registrationJpaRepository;
    @Mock private RegistrationMapper registrationMapper;
    @Mock private EntityManager entityManager;

    @InjectMocks private RegistrationRepositoryImpl repository;

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
