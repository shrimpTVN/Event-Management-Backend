package com.ddd.infrastructure.repository;

import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.StudentProfile;
import com.ddd.domain.repository.StudentProfileRepository;
import com.ddd.infrastructure.entity.AssociationJpaEntity;
import com.ddd.infrastructure.entity.MajorJpaEntity;
import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.StudentProfileMapper;
import com.ddd.infrastructure.repository.jpaRepository.AssociationJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.MajorJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.StudentProfileJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentProfileRepositoryImpl implements StudentProfileRepository {
    private final StudentProfileJpaRepository studentProfileJpaRepository;
    private final StudentProfileMapper studentProfileMapper;
    private final UserJpaRepository userJpaRepository;
    private final AssociationJpaRepository associationJpaRepository;
    private final MajorJpaRepository majorJpaRepository;

    @Override
    public boolean existsByStudentId(String studentId) {
        return studentProfileJpaRepository.existsByStudentId(studentId);
    }

    @Override
    public void save(StudentProfile studentProfile, Long userId) {
        AssociationJpaEntity associationJpaEntity = associationJpaRepository.findById(studentProfile.getAssociationId())
                .orElseThrow(() -> new ResourceNotFoundException("Association not found with id: " + studentProfile.getAssociationId()));
        MajorJpaEntity majorJpaEntity = majorJpaRepository.findById(studentProfile.getMajorId())
                .orElseThrow(() -> new ResourceNotFoundException("Major not found with id: " + studentProfile.getMajorId()));
        UserJpaEntity userJpaEntity = userJpaRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        StudentProfileJpaEntity studentProfileJpaEntity = studentProfileMapper.toEntity(studentProfile);
        studentProfileJpaEntity.setAssociation(associationJpaEntity);
        studentProfileJpaEntity.setMajor(majorJpaEntity);
        studentProfileJpaEntity.setUser(userJpaEntity);

        studentProfileJpaRepository.save(studentProfileJpaEntity);
    }
}
