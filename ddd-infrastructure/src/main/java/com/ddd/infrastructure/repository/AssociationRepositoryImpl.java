package com.ddd.infrastructure.repository;

import com.ddd.domain.model.Association;
import com.ddd.domain.repository.AssociationRepository;
import com.ddd.infrastructure.entity.AssociationJpaEntity;
import com.ddd.infrastructure.mapper.AssociationMapper;
import com.ddd.infrastructure.repository.jpaRepository.AssociationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AssociationRepositoryImpl implements AssociationRepository {
    private final AssociationJpaRepository jpaRepository;
    private final AssociationMapper mapper;

    @Override
    public List<Association> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Association findById(Long id) {
        AssociationJpaEntity entity = jpaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Association not found"));
        return mapper.toDomain(entity);
    }

    @Override
    public Association saveAssociation(Association association) {
        AssociationJpaEntity entity = mapper.toEntity(association);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteAssociation(Long id) {
        AssociationJpaEntity entity = jpaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Association not found"));
        entity.setIsActive(false);
        jpaRepository.save(entity);
    }
}
