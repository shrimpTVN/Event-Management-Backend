package com.ddd.domain.repository;

import com.ddd.domain.model.RefreshToken;
import java.util.Optional;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken refreshToken);
    Optional<RefreshToken> findByToken(String token);
    void revokeAllByUserId(Long userId);
    void deleteByToken(String token);
    void deleteExpiredTokens();
}
