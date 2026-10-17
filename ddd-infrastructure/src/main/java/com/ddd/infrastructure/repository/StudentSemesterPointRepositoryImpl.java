package com.ddd.infrastructure.repository;

import com.ddd.domain.repository.StudentSemesterPointRepository;
import com.ddd.infrastructure.repository.jpaRepository.StudentSemesterJpaRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StudentSemesterPointRepositoryImpl implements StudentSemesterPointRepository {
    private final StudentSemesterJpaRepository jpaRepository;

}
