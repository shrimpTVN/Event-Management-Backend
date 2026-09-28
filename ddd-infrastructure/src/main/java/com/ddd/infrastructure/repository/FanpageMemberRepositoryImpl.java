package com.ddd.infrastructure.repository;

import com.ddd.domain.enums.FanpageRoleEnum;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.FanpageMember;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.infrastructure.entity.FanpageJpaEntity;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntityId;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.FanpageMemberMapper;
import com.ddd.infrastructure.repository.jpaRepository.FanpageMemberJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FanpageMemberRepositoryImpl implements FanpageMemberRepository {
    private final FanpageMemberJpaRepository fanpageMemberJpaRepository;
    private final FanpageMemberMapper fanpageMemberMapper;
    private final EntityManager entityManager;

    public Optional<FanpageMember> findById(Long fanpageId, Long userId) {

        FanpageMemberJpaEntityId id = new FanpageMemberJpaEntityId(fanpageId, userId);

        Optional<FanpageMemberJpaEntity> entity = fanpageMemberJpaRepository.findById(id);


        return entity.map(fanpageMemberMapper::toDomain);
    }


    @Override
    public FanpageMember save(FanpageMember domainModel) {
        FanpageMemberJpaEntity fanpageMemberJpaEntity = fanpageMemberMapper.toJpaEntity(domainModel);

        FanpageMemberJpaEntityId id = new FanpageMemberJpaEntityId(domainModel.getFanpageId(), domainModel.getUserId());
        fanpageMemberJpaEntity.setId(id);

        // getReference() creates a proxy object without executing a database SELECT query, making it highly performant.
        FanpageJpaEntity fanpageRef = entityManager.getReference(FanpageJpaEntity.class, domainModel.getFanpageId());
        UserJpaEntity userRef = entityManager.getReference(UserJpaEntity.class, domainModel.getUserId());
        fanpageMemberJpaEntity.setFanpage(fanpageRef);
        fanpageMemberJpaEntity.setUser(userRef);


        FanpageMemberJpaEntity savedEntity = fanpageMemberJpaRepository.save(fanpageMemberJpaEntity);

        return fanpageMemberMapper.toDomain(savedEntity);
    }

    @Override
    public boolean isAlreadyHasFanpage(Long userId) {
        return fanpageMemberJpaRepository.existsByUserIdAndRoleAndIsActive(userId, FanpageRoleEnum.ADMIN.name(), true);
    }
}
