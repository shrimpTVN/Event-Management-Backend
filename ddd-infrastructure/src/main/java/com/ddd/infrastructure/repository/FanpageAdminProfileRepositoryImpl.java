package com.ddd.infrastructure.repository;

import com.ddd.domain.model.FanpageAdminProfile;
import com.ddd.domain.repository.FanpageAdminProfileRepository;
import com.ddd.infrastructure.mapper.FanpageAdminProfileMapper;
import com.ddd.infrastructure.repository.jpaRepository.FanpageAdminProfileJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Component
@RequiredArgsConstructor
public class FanpageAdminProfileRepositoryImpl implements FanpageAdminProfileRepository {
    private final FanpageAdminProfileJpaRepository fanpageAdminProfileJpaRepository;
    private final FanpageAdminProfileMapper fanpageAdminProfileMapper;
    private final UserJpaRepository userJpaRepository;

    @Override
    public void createFanpageAdminProfile(FanpageAdminProfile fanpageAdminProfile, Long userId) {
        var fanpageAdminProfileJpaEntity = fanpageAdminProfileMapper.toJpaEntity(fanpageAdminProfile);
        var userJpaEntity = userJpaRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        fanpageAdminProfileJpaEntity.setUser(userJpaEntity);

        fanpageAdminProfileJpaRepository.save(fanpageAdminProfileJpaEntity);
    }

    @Override
    public boolean existsByStaffId(String s) {
        return fanpageAdminProfileJpaRepository.existsByStaffId(s);
    }


}
