package com.ddd.infrastructure.repository;

import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.School;
import com.ddd.domain.repository.SchoolRepository;
import com.ddd.infrastructure.entity.SchoolJpaEntity;
import com.ddd.infrastructure.mapper.SchoolMapper;
import com.ddd.infrastructure.repository.jpaRepository.SchoolJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.print.Pageable;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SchoolRepositoryImpl implements SchoolRepository {

    private final SchoolJpaRepository schoolJpaRepository;
    private final SchoolMapper schoolMapper;

    @Override
    public List<School> findAll(){
        return schoolJpaRepository.findAll()
                .stream()
                .map(schoolMapper::toDomainModel)
                .toList();
    }

    @Override
    public Optional<School> findById(Long id){
        return schoolJpaRepository.findById(id)
                .map(schoolMapper::toDomainModel);
    }

    @Override
    public School save(School school){
        SchoolJpaEntity schoolJpaEntity = schoolMapper.toJpaEntity(school);
        return schoolMapper.toDomainModel(schoolJpaRepository.save(schoolJpaEntity));
    }

    @Override
    public void deleteSchool(Long id){
        SchoolJpaEntity schoolJpaEntity = schoolJpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + id));
        schoolJpaEntity.setIsActive(false);
        schoolJpaRepository.save(schoolJpaEntity);
    }

    @Override
    public School updateSchool(Long id, School school) {
        SchoolJpaEntity schoolJpaEntity = schoolJpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + id));

        schoolMapper.updateJpaEntity(school, schoolJpaEntity);

        return schoolMapper.toDomainModel(schoolJpaRepository.save(schoolJpaEntity));
    }


}
