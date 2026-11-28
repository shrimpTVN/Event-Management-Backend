package com.ddd.infrastructure.repository;

import com.ddd.domain.model.Otp;
import com.ddd.domain.repository.OtpRepository;
import com.ddd.infrastructure.entity.OtpJpaEntity;
import com.ddd.infrastructure.mapper.OtpMapper;
import com.ddd.infrastructure.repository.jpaRepository.OtpJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OtpRepositoryImpl implements OtpRepository {

    private final OtpJpaRepository otpJpaRepository;
    private final OtpMapper otpMapper;

    @Override
    public Otp save(Otp otp) {
        OtpJpaEntity entity = otpMapper.toEntity(otp);
        OtpJpaEntity savedEntity = otpJpaRepository.save(entity);
        return otpMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Otp> findLatestValidOtp(String email) {
        return otpJpaRepository.findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(email)
                .map(otpMapper::toDomain);
    }

    @Override
    public Optional<Otp> findLatestOtp(String email) {
        return otpJpaRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .map(otpMapper::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markOtpUsed(Long otpId) {
        if (otpId != null) {
            otpJpaRepository.markOtpUsed(otpId);
        }
    }

}
