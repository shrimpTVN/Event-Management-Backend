package com.ddd.application.service.criteriaprogress.impl;

import com.ddd.application.dto.criteria.CriteriaProgressItemDto;
import com.ddd.application.dto.criteria.CriteriaTreeNodeDto;
import com.ddd.application.dto.criteria.StudentCriteriaProgressDto;
import com.ddd.application.service.criteria.CriteriaService;
import com.ddd.domain.enums.CriteriaScopeEnum;
import com.ddd.infrastructure.entity.AcademicYearJpaEntity;
import com.ddd.infrastructure.entity.CriteriaJpaEntity;
import com.ddd.infrastructure.entity.StudentCriteriaProgressJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.AcademicYearJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.StudentCriteriaTransactionJpaRepository;
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
    private StudentCriteriaTransactionJpaRepository transactionJpaRepository;

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

        var first = new com.ddd.infrastructure.entity.StudentCriteriaTransactionJpaEntity();
        first.setId(1L); first.setCriteria(leafCriteriaEntity); first.setCompletionSource("SYSTEM");
        var second = new com.ddd.infrastructure.entity.StudentCriteriaTransactionJpaEntity();
        second.setId(2L); second.setCriteria(leafCriteriaEntity); second.setCompletionSource("SYSTEM");
        when(userJpaRepository.findByEmail(email)).thenReturn(user);
        when(academicYearJpaRepository.findById(academicYearId)).thenReturn(Optional.of(academicYear));
        when(criteriaService.getCriteriaHierarchy(null)).thenReturn(List.of(root));
        when(transactionJpaRepository.findByUserIdAndAcademicYearId(100L, academicYearId))
                .thenReturn(List.of(first, second));

        StudentCriteriaProgressDto result = queryService.getProgress(email, academicYearId);

        assertEquals(1, result.criterias().size());
        CriteriaProgressItemDto leafItem = result.criterias().get(0).children().get(0);
        assertEquals(3, leafItem.requiredOccurrences());
        assertEquals(2, leafItem.completedOccurrences());
        assertFalse(leafItem.isCompleted());
    }

    @Test
    @DisplayName("Should delegate scope filter to criteriaService.getCriteriaTree")
    void getProgress_whenScopeProvided_shouldDelegateScopeToCriteriaService() {
        String email = "student@ctu.edu.vn";
        Long academicYearId = 1L;

        UserJpaEntity user = new UserJpaEntity();
        user.setId(100L);
        user.setEmail(email);

        AcademicYearJpaEntity academicYear = new AcademicYearJpaEntity();
        academicYear.setId(academicYearId);
        academicYear.setCode("2026-2027");

        CriteriaTreeNodeDto schoolNode = new CriteriaTreeNodeDto(
                100L, "SV5T School", null, "FACULTY", 1, 1, 1, 1, List.of());

        when(userJpaRepository.findByEmail(email)).thenReturn(user);
        when(academicYearJpaRepository.findById(academicYearId)).thenReturn(Optional.of(academicYear));
        when(criteriaService.getCriteriaHierarchy(CriteriaScopeEnum.FACULTY)).thenReturn(List.of(schoolNode));
        when(transactionJpaRepository.findByUserIdAndAcademicYearId(100L, academicYearId))
                .thenReturn(List.of());

        StudentCriteriaProgressDto result = queryService.getProgress(email, academicYearId, CriteriaScopeEnum.FACULTY);

        assertEquals(1, result.criterias().size());
        assertEquals("FACULTY", result.criterias().get(0).scope());
    }

    @Test
    @DisplayName("Should filter getCriteriaProgress by scope")
    void getCriteriaProgress_whenScopeProvided_shouldDelegateScopeToCriteriaService() {
        String email = "student@ctu.edu.vn";
        Long academicYearId = 1L;
        Long criteriaId = 100L;

        UserJpaEntity user = new UserJpaEntity();
        user.setId(100L);
        user.setEmail(email);

        AcademicYearJpaEntity academicYear = new AcademicYearJpaEntity();
        academicYear.setId(academicYearId);
        academicYear.setCode("2026-2027");

        CriteriaTreeNodeDto schoolNode = new CriteriaTreeNodeDto(
                criteriaId, "SV5T School", null, "FACULTY", 1, 1, 1, 1, List.of());

        when(userJpaRepository.findByEmail(email)).thenReturn(user);
        when(academicYearJpaRepository.findById(academicYearId)).thenReturn(Optional.of(academicYear));
        when(criteriaService.getCriteriaHierarchy(CriteriaScopeEnum.FACULTY)).thenReturn(List.of(schoolNode));
        when(transactionJpaRepository.findByUserIdAndAcademicYearId(100L, academicYearId))
                .thenReturn(List.of());

        CriteriaProgressItemDto result = queryService.getCriteriaProgress(
                email, criteriaId, academicYearId, CriteriaScopeEnum.FACULTY);

        assertEquals(criteriaId, result.id());
        assertEquals("FACULTY", result.scope());
    }
}
