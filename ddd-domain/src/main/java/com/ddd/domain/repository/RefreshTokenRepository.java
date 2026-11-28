package com.ddd.domain.repository;

import com.ddd.domain.model.RefreshToken;
import java.util.Optional;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken refreshToken);
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByTokenForUpdate(String token);
    boolean revokeTokenIfActive(String token);
    void revokeAllByUserId(Long userId);
    void deleteByToken(String token);
    void deleteExpiredTokens();
    void deleteExpiredByUserId(Long userId);
}
