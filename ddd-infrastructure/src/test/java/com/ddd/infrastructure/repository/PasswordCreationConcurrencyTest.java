package com.ddd.infrastructure.repository;

import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.UserMapper;
import com.ddd.infrastructure.repository.jpaRepository.RoleJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordCreationConcurrencyTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleJpaRepository roleJpaRepository;

    @InjectMocks
    private UserRepositoryImpl userRepository;

    @Test
    @DisplayName("concurrent password creation should ensure only one caller can set password")
    void concurrentPasswordCreation_shouldEnsureSingleWinner() throws Exception {
        Long userId = 100L;
        UserJpaEntity sharedUser = new UserJpaEntity();
        sharedUser.setId(userId);
        sharedUser.setPassword(null);

        Semaphore dbRowLock = new Semaphore(1);

        // When locked, DB returns the current committed state of the entity
        when(userJpaRepository.findByIdForUpdate(userId)).thenAnswer(invocation -> {
            dbRowLock.acquire();
            UserJpaEntity snapshot = new UserJpaEntity();
            snapshot.setId(sharedUser.getId());
            snapshot.setPassword(sharedUser.getPassword());
            return Optional.of(snapshot);
        });

        when(userJpaRepository.save(any(UserJpaEntity.class))).thenAnswer(invocation -> {
            UserJpaEntity entityToSave = invocation.getArgument(0);
            sharedUser.setPassword(entityToSave.getPassword());
            return entityToSave;
        });

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean result = userRepository.setPasswordIfAbsent(userId, "encodedPassword_" + threadIndex);
                    if (result) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    if (dbRowLock.availablePermits() == 0) {
                        dbRowLock.release();
                    }
                    doneLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly one thread must win, and 9 threads must fail (receive false)
        assertEquals(1, successCount.get(), "Only one thread must successfully set the password");
        assertEquals(numThreads - 1, failureCount.get(), "All other threads must be rejected");
        assertNotNull(sharedUser.getPassword(), "Stored password must not be null");
        assertTrue(sharedUser.getPassword().startsWith("encodedPassword_"));
    }
}
