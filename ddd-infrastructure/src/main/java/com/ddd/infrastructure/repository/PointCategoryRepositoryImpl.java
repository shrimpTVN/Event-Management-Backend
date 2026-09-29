package com.ddd.infrastructure.repository;

import com.ddd.domain.model.PointCategory;
import com.ddd.domain.repository.PointCategoryRepository;
import com.ddd.infrastructure.entity.PointCategoryJpaEntity;
import com.ddd.infrastructure.mapper.PointCategoryMapper;
import com.ddd.infrastructure.repository.jpaRepository.PointCategoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PointCategoryRepositoryImpl implements PointCategoryRepository {
    private final PointCategoryJpaRepository pointCategoryJpaRepository;
    private final PointCategoryMapper pointCategoryMapper;

    @Override
    public Optional<PointCategory> findById(Long id) {
        return pointCategoryJpaRepository.findById(id)
                .map(pointCategoryMapper::toDomainModel);
    }

    @Override
    public PointCategory save(PointCategory pointCategory) {
        PointCategoryJpaEntity entity = pointCategoryMapper.toJpaEntity(pointCategory);
        if (pointCategory.getParentCategoryId() != null) {
            PointCategoryJpaEntity parent = pointCategoryJpaRepository.findById(pointCategory.getParentCategoryId())
                    .orElse(null);
            entity.setParentCategory(parent);
        } else {
            entity.setParentCategory(null);
        }
        PointCategoryJpaEntity savedEntity = pointCategoryJpaRepository.save(entity);
        return pointCategoryMapper.toDomainModel(savedEntity);
    }

    @Override
    public List<PointCategory> findAll() {
        // Query all point categories sorted by level ascending, then childrenOrder ascending
        Sort sort = Sort.by(Sort.Direction.ASC, "level")
                .and(Sort.by(Sort.Direction.ASC, "childrenOrder"));
        return pointCategoryJpaRepository.findAll(sort)
                .stream()
                .map(pointCategoryMapper::toDomainModel)
                .toList();
    }
}

