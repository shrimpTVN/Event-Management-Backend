package com.ddd.infrastructure.repository;

import com.ddd.domain.model.FanpageMemberInvitation;
import com.ddd.domain.repository.FanpageMemberInvitationRepository;
import com.ddd.infrastructure.entity.FanpageJpaEntity;
import com.ddd.infrastructure.entity.FanpageMemberInvitationJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.FanpageMemberInvitationMapper;
import com.ddd.infrastructure.repository.jpaRepository.FanpageMemberInvitationJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FanpageMemberInvitationRepositoryImpl implements FanpageMemberInvitationRepository {
    private final FanpageMemberInvitationJpaRepository fanpageMemberInvitationJpaRepository;
    private final FanpageMemberInvitationMapper fanpageMemberInvitationMapper;
    private final EntityManager em;

    @Override
    public Optional<FanpageMemberInvitation> findById(Long id) {
        return fanpageMemberInvitationJpaRepository.findById(id)
                .map(fanpageMemberInvitationMapper::toDomain);
    }

    @Override
    public FanpageMemberInvitation save(FanpageMemberInvitation invitation) {
        FanpageMemberInvitationJpaEntity jpaEntity;
        if (invitation.getId() != null) {
            jpaEntity = fanpageMemberInvitationJpaRepository.findById(invitation.getId())
                    .orElseGet(() -> fanpageMemberInvitationMapper.toJpaEntity(invitation));
            fanpageMemberInvitationMapper.updateJpaEntityFromDomain(invitation, jpaEntity);
            assignReferences(jpaEntity, invitation);
        } else {
            jpaEntity = fanpageMemberInvitationMapper.toJpaEntity(invitation);
            assignReferences(jpaEntity, invitation);
        }

        FanpageMemberInvitationJpaEntity saved = fanpageMemberInvitationJpaRepository.save(jpaEntity);
        return fanpageMemberInvitationMapper.toDomain(saved);
    }

    private void assignReferences(FanpageMemberInvitationJpaEntity jpaEntity, FanpageMemberInvitation invitation) {
        if (invitation.getUserId() != null) {
            jpaEntity.setUser(em.getReference(UserJpaEntity.class, invitation.getUserId()));
        }
        if (invitation.getFanpageId() != null) {
            jpaEntity.setFanpage(em.getReference(FanpageJpaEntity.class, invitation.getFanpageId()));
        }
    }
}
