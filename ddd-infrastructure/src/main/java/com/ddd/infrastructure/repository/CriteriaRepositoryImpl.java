package com.ddd.infrastructure.repository;

import com.ddd.domain.model.Criteria;
import com.ddd.domain.repository.CriteriaRepository;
import com.ddd.infrastructure.entity.CriteriaJpaEntity;
import com.ddd.infrastructure.mapper.CriteriaMapper;
import com.ddd.infrastructure.repository.jpaRepository.CriteriaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.stream;

@Component
@RequiredArgsConstructor
public class CriteriaRepositoryImpl implements CriteriaRepository {
    private final CriteriaJpaRepository criteriaJpaRepository;
    private final CriteriaMapper criteriaMapper;

    private Sort getDefaultSort() {
        return Sort.by(Sort.Direction.ASC, "level")
                .and(Sort.by(Sort.Direction.ASC, "childrenOrder"));
    }

    @Override
    public Criteria save(Criteria criteria) {
        CriteriaJpaEntity criteriaJpaEntity = criteriaMapper.toEntity(criteria);
        if (criteria.getParentId() != null) {
            CriteriaJpaEntity parent = criteriaJpaRepository.findById(criteria.getParentId())
                    .orElse(null);
            criteriaJpaEntity.setParent(parent);
        } else {
            criteriaJpaEntity.setParent(null);
        }
        CriteriaJpaEntity savedEntity = criteriaJpaRepository.save(criteriaJpaEntity);
        return criteriaMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Criteria> findById(Long id) {
        return criteriaJpaRepository.findById(id)
                .map(criteriaMapper::toDomain);
    }

    @Override
    public List<Criteria> findAll(boolean isActive) {
        return criteriaJpaRepository.findAllByIsActive(isActive, getDefaultSort())
                .stream()
                .map(criteriaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Criteria> findByParentId(Long parentId) {
        Sort sort = getDefaultSort();
        List<CriteriaJpaEntity> entities;
        if (parentId == null || parentId == 0L) {
            entities = criteriaJpaRepository.findByParentIsNull(sort);
        } else {
            entities = criteriaJpaRepository.findByParentId(parentId, sort);
        }
        return entities.stream()
                .map(criteriaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Criteria> findByLevel(Integer level) {
        return criteriaJpaRepository.findByLevel(level, getDefaultSort())
                .stream()
                .map(criteriaMapper::toDomain)
                .toList();
    }

    @Override
    public void delete(Criteria criteria) {
        CriteriaJpaEntity criteriaJpaEntity = criteriaMapper.toEntity(criteria);
        criteriaJpaEntity.setIsActive(false);
        criteriaJpaRepository.save(criteriaJpaEntity);
    }

    @Override
    public boolean hasChildren(Long criteriaId) {
        return criteriaJpaRepository.existsByParentIdAndIsActive(criteriaId, true);
    }

    @Override
    public List<Criteria> findAllByIsActive(boolean b) {
        return criteriaJpaRepository.findAllByIsActive(b, getDefaultSort())
                .stream()
                .map(criteriaMapper::toDomain)
                .toList();
    }
}
