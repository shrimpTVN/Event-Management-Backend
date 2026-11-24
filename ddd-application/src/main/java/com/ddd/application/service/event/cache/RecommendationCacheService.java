package com.ddd.application.service.event.cache;

import com.ddd.application.dto.recommendation.EventRecommendationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
public class RecommendationCacheService {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    private static final int MAX_CACHE_SIZE = 5000;

    private record CacheEntry(List<EventRecommendationDto> data, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();


    public List<EventRecommendationDto> get(Long userId) {
        if (userId == null) {
            return null;
        }

        CacheEntry entry = cache.get(userId);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(userId);
            log.debug("Recommendation cache expired for userId: {}", userId);
            return null;
        }

        log.debug("Recommendation cache hit for userId: {}", userId);
        return entry.data();
    }

  
    public void put(Long userId, List<EventRecommendationDto> recommendations) {
        if (userId == null || recommendations == null) {
            return;
        }

        // Prevent memory exhaustion under extreme loads
        if (cache.size() >= MAX_CACHE_SIZE) {
            cleanExpiredEntries();
            if (cache.size() >= MAX_CACHE_SIZE) {
                cache.clear();
            }
        }

        Instant expiresAt = Instant.now().plus(DEFAULT_TTL);
        cache.put(userId, new CacheEntry(recommendations, expiresAt));
        log.debug("Cached {} recommendations for userId: {} (TTL 30m)", recommendations.size(), userId);
    }

   
    public void evict(Long userId) {
        if (userId != null) {
            cache.remove(userId);
            log.info("Evicted recommendation cache for userId: {}", userId);
        }
    }

   
    public void evictAll() {
        cache.clear();
        log.info("Evicted all recommendation caches.");
    }

    private void cleanExpiredEntries() {
        cache.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}

