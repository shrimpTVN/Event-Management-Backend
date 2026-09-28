package com.ddd.infrastructure.repository;

import com.ddd.domain.enums.FanpageStatusEnum;
import com.ddd.domain.model.Fanpage;
import com.ddd.domain.repository.FanpageRepository;
import com.ddd.infrastructure.entity.FanpageJpaEntity;
import com.ddd.infrastructure.mapper.FanpageMapper;
import com.ddd.infrastructure.repository.jpaRepository.FanpageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FanpageRepositoryImpl implements FanpageRepository {
    private final FanpageJpaRepository fanpageJpaRepository;
    private final FanpageMapper fanpageMapper;

    @Override
    public boolean existsByName(String name) {
        return fanpageJpaRepository.existsByName(name);
    }

    @Override
    public Fanpage save(Fanpage fanpage) {
        FanpageJpaEntity fanpageJpaEntity = fanpageMapper.toEntity(fanpage);
        return fanpageMapper.toDomain( fanpageJpaRepository.save(fanpageJpaEntity));
    }

    @Override
    public Optional<Fanpage> findById(Long fanpageId) {
        return fanpageJpaRepository.findById(fanpageId).map(fanpageMapper::toDomain);
    }
}
