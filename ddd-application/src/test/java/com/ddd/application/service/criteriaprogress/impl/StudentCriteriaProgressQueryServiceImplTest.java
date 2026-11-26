package com.ddd.application.service.criteriaprogress.impl;

import com.ddd.application.dto.criteria.CriteriaProgressItemDto;
import com.ddd.application.dto.criteria.CriteriaTreeNodeDto;
import com.ddd.application.dto.criteria.StudentCriteriaProgressDto;
import com.ddd.application.service.criteria.CriteriaService;
import com.ddd.infrastructure.entity.AcademicYearJpaEntity;
import com.ddd.infrastructure.entity.CriteriaJpaEntity;
import com.ddd.infrastructure.entity.StudentCriteriaProgressJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.AcademicYearJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.StudentCriteriaProgressJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentCriteriaProgressQueryServiceImplTest {

    @Mock
    private CriteriaService criteriaService;

    @Mock
    private StudentCriteriaProgressJpaRepository progressJpaRepository;

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private AcademicYearJpaRepository academicYearJpaRepository;

    @InjectMocks
    private StudentCriteriaProgressQueryServiceImpl queryService;

    @Test
    @DisplayName("Should include requiredOccurrences and completedOccurrences on leaf progress item")
    void getProgress_shouldIncludeRequiredAndCompletedOccurrencesForLeafNode() {
        String email = "student@ctu.edu.vn";
        Long academicYearId = 1L;

        UserJpaEntity user = new UserJpaEntity();
        user.setId(100L);
        user.setEmail(email);

        AcademicYearJpaEntity academicYear = new AcademicYearJpaEntity();
        academicYear.setId(academicYearId);
        academicYear.setCode("2026-2027");

        CriteriaTreeNodeDto leaf = new CriteriaTreeNodeDto(
                402L, "Hoạt động tình nguyện", null, "UNIVERSITY", 2, 1, null, 3, List.of());
        CriteriaTreeNodeDto root = new CriteriaTreeNodeDto(
                4L, "Hội nhập tốt", null, "UNIVERSITY", 1, 1, 1, 1, List.of(leaf));

        CriteriaJpaEntity leafCriteriaEntity = new CriteriaJpaEntity();
        leafCriteriaEntity.setId(402L);

        StudentCriteriaProgressJpaEntity leafProgress = new StudentCriteriaProgressJpaEntity();
        leafProgress.setCriteria(leafCriteriaEntity);
        leafProgress.setCompletedOccurrences(2);
        leafProgress.setIsCompleted(false);

        when(userJpaRepository.findByEmail(email)).thenReturn(user);
        when(academicYearJpaRepository.findById(academicYearId)).thenReturn(Optional.of(academicYear));
        when(criteriaService.getCriteriaTree()).thenReturn(List.of(root));
        when(progressJpaRepository.findAllByUserIdAndAcademicYearId(100L, academicYearId))
                .thenReturn(List.of(leafProgress));

        StudentCriteriaProgressDto result = queryService.getProgress(email, academicYearId);

        assertEquals(1, result.criterias().size());
        CriteriaProgressItemDto leafItem = result.criterias().get(0).children().get(0);
        assertEquals(3, leafItem.requiredOccurrences());
        assertEquals(2, leafItem.completedOccurrences());
        assertFalse(leafItem.isCompleted());
    }
}
