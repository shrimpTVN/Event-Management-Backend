package com.ddd.application.service.criteriaprogress.impl;

import com.ddd.domain.model.Criteria;
import com.ddd.domain.model.StudentCriteriaProgress;
import com.ddd.domain.repository.CriteriaRepository;
import com.ddd.domain.repository.StudentCriteriaProgressRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentCriteriaProgressCommandServiceImplTest {

    @Mock
    private CriteriaRepository criteriaRepository;

    @Mock
    private StudentCriteriaProgressRepository progressRepository;

    @InjectMocks
    private StudentCriteriaProgressCommandServiceImpl service;

    private final Long userId = 100L;
    private final Long academicYearId = 2024L;

    @Test
    @DisplayName("Should throw IllegalArgumentException when leaf progress record is not found")
    void markUncompleted_whenProgressNotFound_shouldThrowIllegalArgumentException() {
        Long criteriaId = 10L;
        when(progressRepository.findByUserIdAndCriteriaIdAndAcademicYearId(userId, criteriaId, academicYearId))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.markUncompleted(userId, criteriaId, academicYearId)
        );

        assertTrue(exception.getMessage().contains("No progress found for user: " + userId));
        verify(progressRepository, never()).delete(any());
        verify(progressRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should return early without delete or save when leaf progress is already uncompleted")
    void markUncompleted_whenAlreadyUncompleted_shouldReturnEarlyWithoutDeletingOrSaving() {
        Long criteriaId = 10L;
        StudentCriteriaProgress leafProgress = new StudentCriteriaProgress(userId, criteriaId, academicYearId);
        leafProgress.setIsCompleted(false);

        when(progressRepository.findByUserIdAndCriteriaIdAndAcademicYearId(userId, criteriaId, academicYearId))
                .thenReturn(Optional.of(leafProgress));

        service.markUncompleted(userId, criteriaId, academicYearId);

        verify(progressRepository, never()).findAllByUserIdAndAcademicYearId(any(), any());
        verify(criteriaRepository, never()).findAllById(any());
        verify(progressRepository, never()).delete(any());
        verify(progressRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should delete leaf progress and mark parent uncompleted when parent count falls below requiredChildren")
    void markUncompleted_whenParentFallsBelowRequiredChildren_shouldMarkParentUncompletedAndDeleteLeaf() {
        Long leafCriteriaId = 10L;
        Long parentCriteriaId = 2L;

        Criteria leafCriteria = new Criteria();
        leafCriteria.setId(leafCriteriaId);
        leafCriteria.setParentId(parentCriteriaId);

        Criteria parentCriteria = new Criteria();
        parentCriteria.setId(parentCriteriaId);
        parentCriteria.setRequiredChildren(2);

        StudentCriteriaProgress leafProgress = new StudentCriteriaProgress(userId, leafCriteriaId, academicYearId);
        leafProgress.setIsCompleted(true);
        leafProgress.setCompletedAt(Instant.now());

        StudentCriteriaProgress parentProgress = new StudentCriteriaProgress(userId, parentCriteriaId, academicYearId);
        parentProgress.setIsCompleted(true);
        parentProgress.setCompletedAt(Instant.now());
        parentProgress.setCompletedChildrenCount(2);

        when(progressRepository.findByUserIdAndCriteriaIdAndAcademicYearId(userId, leafCriteriaId, academicYearId))
                .thenReturn(Optional.of(leafProgress));
        when(progressRepository.findAllByUserIdAndAcademicYearId(userId, academicYearId))
                .thenReturn(List.of(leafProgress, parentProgress));
        when(criteriaRepository.findAllById(anyList()))
                .thenReturn(List.of(leafCriteria, parentCriteria));

        service.markUncompleted(userId, leafCriteriaId, academicYearId);

        verify(progressRepository).delete(leafProgress);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StudentCriteriaProgress>> captor = ArgumentCaptor.forClass(List.class);
        verify(progressRepository).saveAll(captor.capture());

        List<StudentCriteriaProgress> savedList = captor.getValue();
        assertEquals(1, savedList.size());
        StudentCriteriaProgress updatedParent = savedList.get(0);
        assertEquals(parentCriteriaId, updatedParent.getCriteriaId());
        assertEquals(1, updatedParent.getCompletedChildrenCount());
        assertFalse(updatedParent.getIsCompleted());
        assertNull(updatedParent.getCompletedAt());
    }

    @Test
    @DisplayName("Should decrement parent count and stop propagation when parent still meets requiredChildren")
    void markUncompleted_whenParentStillMeetsRequiredChildren_shouldDecrementCountAndStopPropagation() {
        Long leafCriteriaId = 10L;
        Long parentCriteriaId = 2L;
        Instant parentCompletedAt = Instant.now().minusSeconds(3600);

        Criteria leafCriteria = new Criteria();
        leafCriteria.setId(leafCriteriaId);
        leafCriteria.setParentId(parentCriteriaId);

        Criteria parentCriteria = new Criteria();
        parentCriteria.setId(parentCriteriaId);
        parentCriteria.setRequiredChildren(1);

        StudentCriteriaProgress leafProgress = new StudentCriteriaProgress(userId, leafCriteriaId, academicYearId);
        leafProgress.setIsCompleted(true);

        StudentCriteriaProgress parentProgress = new StudentCriteriaProgress(userId, parentCriteriaId, academicYearId);
        parentProgress.setIsCompleted(true);
        parentProgress.setCompletedAt(parentCompletedAt);
        parentProgress.setCompletedChildrenCount(2);

        when(progressRepository.findByUserIdAndCriteriaIdAndAcademicYearId(userId, leafCriteriaId, academicYearId))
                .thenReturn(Optional.of(leafProgress));
        when(progressRepository.findAllByUserIdAndAcademicYearId(userId, academicYearId))
                .thenReturn(List.of(leafProgress, parentProgress));
        when(criteriaRepository.findAllById(anyList()))
                .thenReturn(List.of(leafCriteria, parentCriteria));

        service.markUncompleted(userId, leafCriteriaId, academicYearId);

        verify(progressRepository).delete(leafProgress);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StudentCriteriaProgress>> captor = ArgumentCaptor.forClass(List.class);
        verify(progressRepository).saveAll(captor.capture());

        List<StudentCriteriaProgress> savedList = captor.getValue();
        assertEquals(1, savedList.size());
        StudentCriteriaProgress updatedParent = savedList.get(0);
        assertEquals(parentCriteriaId, updatedParent.getCriteriaId());
        assertEquals(1, updatedParent.getCompletedChildrenCount());
        assertTrue(updatedParent.getIsCompleted());
        assertEquals(parentCompletedAt, updatedParent.getCompletedAt());
    }

    @Test
    @DisplayName("Should propagate uncompleted status up multiple levels when parent and grandparent fall below thresholds")
    void markUncompleted_multiLevelPropagation_shouldCascadeToGrandparentWhenBothFallBelowThreshold() {
        Long leafId = 10L;
        Long parentId = 2L;
        Long grandparentId = 1L;

        Criteria leafCriteria = new Criteria();
        leafCriteria.setId(leafId);
        leafCriteria.setParentId(parentId);

        Criteria parentCriteria = new Criteria();
        parentCriteria.setId(parentId);
        parentCriteria.setParentId(grandparentId);
        parentCriteria.setRequiredChildren(1);

        Criteria grandparentCriteria = new Criteria();
        grandparentCriteria.setId(grandparentId);
        grandparentCriteria.setParentId(null);
        grandparentCriteria.setRequiredChildren(1);

        StudentCriteriaProgress leafProgress = new StudentCriteriaProgress(userId, leafId, academicYearId);
        leafProgress.setIsCompleted(true);

        StudentCriteriaProgress parentProgress = new StudentCriteriaProgress(userId, parentId, academicYearId);
        parentProgress.setIsCompleted(true);
        parentProgress.setCompletedAt(Instant.now());
        parentProgress.setCompletedChildrenCount(1);

        StudentCriteriaProgress grandparentProgress = new StudentCriteriaProgress(userId, grandparentId, academicYearId);
        grandparentProgress.setIsCompleted(true);
        grandparentProgress.setCompletedAt(Instant.now());
        grandparentProgress.setCompletedChildrenCount(1);

        when(progressRepository.findByUserIdAndCriteriaIdAndAcademicYearId(userId, leafId, academicYearId))
                .thenReturn(Optional.of(leafProgress));
        when(progressRepository.findAllByUserIdAndAcademicYearId(userId, academicYearId))
                .thenReturn(List.of(leafProgress, parentProgress, grandparentProgress));
        when(criteriaRepository.findAllById(anyList()))
                .thenReturn(List.of(leafCriteria, parentCriteria, grandparentCriteria));

        service.markUncompleted(userId, leafId, academicYearId);

        verify(progressRepository).delete(leafProgress);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StudentCriteriaProgress>> captor = ArgumentCaptor.forClass(List.class);
        verify(progressRepository).saveAll(captor.capture());

        List<StudentCriteriaProgress> savedList = captor.getValue();
        assertEquals(2, savedList.size());

        StudentCriteriaProgress updatedParent = savedList.stream()
                .filter(p -> p.getCriteriaId().equals(parentId))
                .findFirst()
                .orElseThrow();
        assertEquals(0, updatedParent.getCompletedChildrenCount());
        assertFalse(updatedParent.getIsCompleted());
        assertNull(updatedParent.getCompletedAt());

        StudentCriteriaProgress updatedGrandparent = savedList.stream()
                .filter(p -> p.getCriteriaId().equals(grandparentId))
                .findFirst()
                .orElseThrow();
        assertEquals(0, updatedGrandparent.getCompletedChildrenCount());
        assertFalse(updatedGrandparent.getIsCompleted());
        assertNull(updatedGrandparent.getCompletedAt());
    }

    @Test
    @DisplayName("Should stop propagation and delete leaf when parent progress is not found in database")
    void markUncompleted_whenParentProgressNotFound_shouldStopPropagationAndDeleteLeaf() {
        Long leafId = 10L;
        Long parentId = 2L;

        Criteria leafCriteria = new Criteria();
        leafCriteria.setId(leafId);
        leafCriteria.setParentId(parentId);

        Criteria parentCriteria = new Criteria();
        parentCriteria.setId(parentId);
        parentCriteria.setRequiredChildren(1);

        StudentCriteriaProgress leafProgress = new StudentCriteriaProgress(userId, leafId, academicYearId);
        leafProgress.setIsCompleted(true);

        when(progressRepository.findByUserIdAndCriteriaIdAndAcademicYearId(userId, leafId, academicYearId))
                .thenReturn(Optional.of(leafProgress));
        when(progressRepository.findAllByUserIdAndAcademicYearId(userId, academicYearId))
                .thenReturn(List.of(leafProgress));
        when(criteriaRepository.findAllById(anyList()))
                .thenReturn(List.of(leafCriteria, parentCriteria));

        service.markUncompleted(userId, leafId, academicYearId);

        verify(progressRepository).delete(leafProgress);
        verify(progressRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should default requiredChildren to 1 when parent criteria requiredChildren is null")
    void markUncompleted_whenParentCriteriaHasNullRequiredChildren_shouldDefaultToOne() {
        Long leafId = 10L;
        Long parentId = 2L;

        Criteria leafCriteria = new Criteria();
        leafCriteria.setId(leafId);
        leafCriteria.setParentId(parentId);

        Criteria parentCriteria = new Criteria();
        parentCriteria.setId(parentId);
        parentCriteria.setRequiredChildren(null);

        StudentCriteriaProgress leafProgress = new StudentCriteriaProgress(userId, leafId, academicYearId);
        leafProgress.setIsCompleted(true);

        StudentCriteriaProgress parentProgress = new StudentCriteriaProgress(userId, parentId, academicYearId);
        parentProgress.setIsCompleted(true);
        parentProgress.setCompletedAt(Instant.now());
        parentProgress.setCompletedChildrenCount(1);

        when(progressRepository.findByUserIdAndCriteriaIdAndAcademicYearId(userId, leafId, academicYearId))
                .thenReturn(Optional.of(leafProgress));
        when(progressRepository.findAllByUserIdAndAcademicYearId(userId, academicYearId))
                .thenReturn(List.of(leafProgress, parentProgress));
        when(criteriaRepository.findAllById(anyList()))
                .thenReturn(List.of(leafCriteria, parentCriteria));

        service.markUncompleted(userId, leafId, academicYearId);

        verify(progressRepository).delete(leafProgress);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StudentCriteriaProgress>> captor = ArgumentCaptor.forClass(List.class);
        verify(progressRepository).saveAll(captor.capture());

        List<StudentCriteriaProgress> savedList = captor.getValue();
        assertEquals(1, savedList.size());
        StudentCriteriaProgress updatedParent = savedList.get(0);
        assertEquals(0, updatedParent.getCompletedChildrenCount());
        assertFalse(updatedParent.getIsCompleted());
        assertNull(updatedParent.getCompletedAt());
    }
}
