package com.ddd.infrastructure.repository;

import com.ddd.domain.model.RefreshToken;
import com.ddd.domain.repository.RefreshTokenRepository;
import com.ddd.infrastructure.mapper.RefreshTokenMapper;
import com.ddd.infrastructure.repository.jpaRepository.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final RefreshTokenMapper refreshTokenMapper;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        var entity = refreshTokenMapper.toEntity(refreshToken);
        return refreshTokenMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<RefreshToken> findByToken(String hashedToken) {
        return jpaRepository.findByToken(hashedToken)
                .map(refreshTokenMapper::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByTokenForUpdate(String hashedToken) {
        return jpaRepository.findByTokenForUpdate(hashedToken)
                .map(refreshTokenMapper::toDomain);
    }

    @Override
    @Transactional
    public boolean revokeTokenIfActive(String hashedToken) {
        return jpaRepository.revokeTokenIfActive(hashedToken) > 0;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllByUserId(Long userId) {
        jpaRepository.revokeAllByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteByToken(String hashedToken) {
        jpaRepository.deleteByToken(hashedToken);
    }

    @Override
    @Transactional
    public void deleteExpiredTokens() {
        jpaRepository.deleteAllExpiredBefore(Instant.now());
    }

    @Override
    @Transactional
    public void deleteExpiredByUserId(Long userId) {
        if (userId != null) {
            jpaRepository.deleteExpiredByUserIdBefore(userId, Instant.now());
        }
    }
}
