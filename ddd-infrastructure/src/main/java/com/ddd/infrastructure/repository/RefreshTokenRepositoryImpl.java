package com.ddd.infrastructure.repository;

import com.ddd.domain.model.RefreshToken;
import com.ddd.domain.repository.RefreshTokenRepository;
import com.ddd.infrastructure.mapper.RefreshTokenMapper;
import com.ddd.infrastructure.repository.jpaRepository.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
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
    @Transactional
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
}
