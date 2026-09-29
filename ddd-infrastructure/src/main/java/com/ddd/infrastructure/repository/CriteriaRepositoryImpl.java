package com.ddd.infrastructure.repository;

import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Criteria;
import com.ddd.domain.repository.CriteriaRepository;
import com.ddd.infrastructure.entity.CriteriaJpaEntity;
import com.ddd.infrastructure.mapper.CriteriaMapper;
import com.ddd.infrastructure.repository.jpaRepository.CriteriaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CriteriaRepositoryImpl implements CriteriaRepository {
    private final CriteriaJpaRepository criteriaJpaRepository;
    private final CriteriaMapper criteriaMapper;

    @Override
    public Criteria save(Criteria criteria) {
        CriteriaJpaEntity criteriaJpaEntity = criteriaMapper.toEntity(criteria);
        CriteriaJpaEntity savedEntity = criteriaJpaRepository.save(criteriaJpaEntity);
        return criteriaMapper.toDomain(savedEntity);
    }

    @Override
    public Criteria findById(Long id) {
        CriteriaJpaEntity entity = criteriaJpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Criteria not found with id: " + id));
        return criteriaMapper.toDomain(entity);
    }

    @Override
    public void delete(Criteria criteria) {
        CriteriaJpaEntity criteriaJpaEntity = criteriaMapper.toEntity(criteria);
        criteriaJpaEntity.setIsActive(false);
        criteriaJpaRepository.save(criteriaJpaEntity);
    }
}
