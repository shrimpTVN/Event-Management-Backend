package com.ddd.infrastructure.repository;

import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.infrastructure.mapper.EventMapper;
import com.ddd.infrastructure.repository.jpaRepository.EventJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventRepositoryImplTest {
    @Mock private EventJpaRepository eventJpaRepository;
    @Mock private EventMapper eventMapper;
    @Mock private EntityManager entityManager;
    @Mock private Query timeoutQuery;
    @InjectMocks private EventRepositoryImpl repository;

    @Test
    void missingEventRaisesResourceNotFoundAfterConfiguringTimeout() {
        when(entityManager.createNativeQuery("select set_config('lock_timeout', '3s', true)"))
                .thenReturn(timeoutQuery);
        when(eventJpaRepository.findByIdForUpdate(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> repository.findByIdForUpdate(5L));

        var calls = inOrder(timeoutQuery, eventJpaRepository);
        calls.verify(timeoutQuery).getSingleResult();
        calls.verify(eventJpaRepository).findByIdForUpdate(5L);
        verifyNoInteractions(eventMapper);
    }
}
