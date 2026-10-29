package com.ddd.infrastructure.scheduler;

import com.ddd.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to purge expired refresh tokens from the database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    // Runs every day at 2:00 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanExpiredTokens() {
        log.info("Running scheduled refresh token cleanup...");
        refreshTokenRepository.deleteExpiredTokens();
    }
}
