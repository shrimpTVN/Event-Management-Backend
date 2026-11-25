package com.ddd.application.service.event.cache;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationCacheServiceTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void evictsOnlyAfterSuccessfulCommit(boolean commit) {
        RecommendationCacheService cache = new RecommendationCacheService();
        cache.put(1L, List.of());
        TransactionSynchronizationManager.initSynchronization();
        try {
            cache.evictAfterCommit(1L);
            assertNotNull(cache.get(1L));
            var callbacks = TransactionSynchronizationManager.getSynchronizations();
            if (commit) {
                callbacks.forEach(TransactionSynchronization::afterCommit);
                assertNull(cache.get(1L));
            } else {
                callbacks.forEach(callback -> callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
                assertNotNull(cache.get(1L));
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
