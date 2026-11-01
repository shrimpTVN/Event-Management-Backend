package com.ddd.infrastructure.repository;

import com.ddd.domain.enums.FollowingStatusEnum;
import com.ddd.domain.model.Following;
import com.ddd.domain.repository.FollowingRepository;
import com.ddd.infrastructure.entity.FanpageJpaEntity;
import com.ddd.infrastructure.entity.FollowingJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.FollowingMapper;
import com.ddd.infrastructure.repository.jpaRepository.FollowingJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FollowingRepositoryImpl implements FollowingRepository {

    private final FollowingJpaRepository followingJpaRepository;
    private final FollowingMapper followingMapper;
    private final EntityManager entityManager;

    @Override
    public Optional<Following> findByFanpageIdAndUserId(Long fanpageId, Long userId) {
        return followingJpaRepository.findByFanpage_IdAndUser_Id(fanpageId, userId)
                .map(followingMapper::toDomain);
    }

    @Override
    public Following save(Following domainModel) {
        FollowingJpaEntity entity = followingMapper.toJpaEntity(domainModel);

        if (domainModel.getId() != null) {
            entity.setId(domainModel.getId());
        }

        FanpageJpaEntity fanpageRef = entityManager.getReference(FanpageJpaEntity.class, domainModel.getFanpageId());
        UserJpaEntity userRef = entityManager.getReference(UserJpaEntity.class, domainModel.getUserId());
        entity.setFanpage(fanpageRef);
        entity.setUser(userRef);

        FollowingJpaEntity saved = followingJpaRepository.save(entity);
        return followingMapper.toDomain(saved);
    }

    @Override
    public long countByFanpageIdAndFolStatus(Long fanpageId, FollowingStatusEnum folStatus) {
        return followingJpaRepository.countByFanpage_IdAndFolStatusAndIsActive(
                fanpageId,
                folStatus,
                true
        );
    }

    @Override
    public boolean existsByFanpageIdAndUserIdAndFolStatus(Long fanpageId, Long userId, FollowingStatusEnum folStatus) {
        return followingJpaRepository.existsByFanpage_IdAndUser_IdAndFolStatusAndIsActive(
                fanpageId,
                userId,
                folStatus,
                true
        );
    }
}
