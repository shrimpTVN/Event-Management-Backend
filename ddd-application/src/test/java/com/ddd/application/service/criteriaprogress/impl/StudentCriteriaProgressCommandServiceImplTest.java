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
    @DisplayName("Should return safely without exception when leaf progress record is not found")
    void markUncompleted_whenProgressNotFound_shouldReturnSafelyWithoutException() {
        Long criteriaId = 10L;
        when(progressRepository.findByUserIdAndCriteriaIdAndAcademicYearId(userId, criteriaId, academicYearId))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.markUncompleted(userId, criteriaId, academicYearId));

        verify(progressRepository, never()).delete(any());
        verify(progressRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should return early without delete or save when leaf progress isCompleted is null")
    void markUncompleted_whenProgressIsCompletedIsNull_shouldReturnEarlyWithoutDeletingOrSaving() {
        Long criteriaId = 10L;
        StudentCriteriaProgress leafProgress = new StudentCriteriaProgress(userId, criteriaId, academicYearId);
        leafProgress.setIsCompleted(null);

        when(progressRepository.findByUserIdAndCriteriaIdAndAcademicYearId(userId, criteriaId, academicYearId))
                .thenReturn(Optional.of(leafProgress));

        service.markUncompleted(userId, criteriaId, academicYearId);

        verify(progressRepository, never()).findAllByUserIdAndAcademicYearId(any(), any());
        verify(criteriaRepository, never()).findAllById(any());
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

    @Test
    @DisplayName("Should track partial occurrences and propagate to parent only when reaching requiredOccurrences")
    void bulkMarkCompleted_whenRequiredOccurrencesIsThree_shouldTrackPartialAndCompleteOnlyOnThird() {
        Long leafId = 402L;
        Long parentId = 4L;

        Criteria leafCriteria = new Criteria();
        leafCriteria.setId(leafId);
        leafCriteria.setParentId(parentId);
        leafCriteria.setRequiredOccurrences(3);

        Criteria parentCriteria = new Criteria();
        parentCriteria.setId(parentId);
        parentCriteria.setRequiredChildren(1);

        StudentCriteriaProgress leafProgress = new StudentCriteriaProgress(userId, leafId, academicYearId);
        StudentCriteriaProgress parentProgress = new StudentCriteriaProgress(userId, parentId, academicYearId);

        when(criteriaRepository.findById(leafId)).thenReturn(Optional.of(leafCriteria));
        when(criteriaRepository.findAllByIsActive(true)).thenReturn(List.of(leafCriteria, parentCriteria));
        when(progressRepository.findByUserIdsAndCriteriaIdAndAcademicYearId(List.of(userId), leafId, academicYearId))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(leafProgress))
                .thenReturn(List.of(leafProgress))
                .thenReturn(List.of(leafProgress));
        when(progressRepository.findAllByUserIdsAndAcademicYearId(List.of(userId), academicYearId))
                .thenReturn(List.of(leafProgress, parentProgress));

        // 1st occurrence -> 1/3, not completed, parent not updated
        service.bulkMarkCompleted(List.of(userId), leafId, academicYearId);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StudentCriteriaProgress>> firstCaptor = ArgumentCaptor.forClass(List.class);
        verify(progressRepository, times(1)).saveAll(firstCaptor.capture());
        StudentCriteriaProgress afterFirst = firstCaptor.getValue().get(0);
        assertEquals(1, afterFirst.getCompletedOccurrences());
        assertFalse(afterFirst.getIsCompleted());
        leafProgress.setCompletedOccurrences(1);
        leafProgress.setIsCompleted(false);

        // 2nd occurrence -> 2/3, not completed, parent still not updated
        service.bulkMarkCompleted(List.of(userId), leafId, academicYearId);
        assertEquals(2, leafProgress.getCompletedOccurrences());
        assertFalse(leafProgress.getIsCompleted());
        assertEquals(0, parentProgress.getCompletedChildrenCount());

        // 3rd occurrence -> 3/3, completed! Parent incremented +1 and marked completed
        service.bulkMarkCompleted(List.of(userId), leafId, academicYearId);
        assertEquals(3, leafProgress.getCompletedOccurrences());
        assertTrue(leafProgress.getIsCompleted());
        assertEquals(1, parentProgress.getCompletedChildrenCount());
        assertTrue(parentProgress.getIsCompleted());

        // 4th occurrence -> 4/3, stays completed, parent NOT incremented a second time
        service.bulkMarkCompleted(List.of(userId), leafId, academicYearId);
        assertEquals(4, leafProgress.getCompletedOccurrences());
        assertTrue(leafProgress.getIsCompleted());
        assertEquals(1, parentProgress.getCompletedChildrenCount());
    }

    @Test
    @DisplayName("Should keep leaf completed and not decrement parent when occurrences drop from 4 to 3")
    void markUncompleted_whenOccurrencesDropFromFourToThree_shouldKeepCompletedAndNotDecrementParent() {
        Long leafId = 402L;
        Criteria leafCriteria = new Criteria();
        leafCriteria.setId(leafId);
        leafCriteria.setRequiredOccurrences(3);

        StudentCriteriaProgress leafProgress = new StudentCriteriaProgress(userId, leafId, academicYearId);
        leafProgress.setCompletedOccurrences(4);
        leafProgress.setIsCompleted(true);
        leafProgress.setCompletedAt(Instant.now());

        when(progressRepository.findByUserIdAndCriteriaIdAndAcademicYearId(userId, leafId, academicYearId))
                .thenReturn(Optional.of(leafProgress));
        when(criteriaRepository.findById(leafId)).thenReturn(Optional.of(leafCriteria));

        service.markUncompleted(userId, leafId, academicYearId);

        assertEquals(3, leafProgress.getCompletedOccurrences());
        assertTrue(leafProgress.getIsCompleted());
        verify(progressRepository).save(leafProgress);
        verify(progressRepository, never()).delete(any());
        verify(progressRepository, never()).findAllByUserIdAndAcademicYearId(any(), any());
    }

    @Test
    @DisplayName("Should mark leaf uncompleted, keep row, and decrement parent when dropping from 3 to 2")
    void markUncompleted_whenOccurrencesDropFromThreeToTwo_shouldMarkUncompletedKeepRowAndDecrementParent() {
        Long leafId = 402L;
        Long parentId = 4L;

        Criteria leafCriteria = new Criteria();
        leafCriteria.setId(leafId);
        leafCriteria.setParentId(parentId);
        leafCriteria.setRequiredOccurrences(3);

        Criteria parentCriteria = new Criteria();
        parentCriteria.setId(parentId);
        parentCriteria.setRequiredChildren(1);

        StudentCriteriaProgress leafProgress = new StudentCriteriaProgress(userId, leafId, academicYearId);
        leafProgress.setCompletedOccurrences(3);
        leafProgress.setIsCompleted(true);
        leafProgress.setCompletedAt(Instant.now());

        StudentCriteriaProgress parentProgress = new StudentCriteriaProgress(userId, parentId, academicYearId);
        parentProgress.setCompletedChildrenCount(1);
        parentProgress.setIsCompleted(true);

        when(progressRepository.findByUserIdAndCriteriaIdAndAcademicYearId(userId, leafId, academicYearId))
                .thenReturn(Optional.of(leafProgress));
        when(criteriaRepository.findById(leafId)).thenReturn(Optional.of(leafCriteria));
        when(progressRepository.findAllByUserIdAndAcademicYearId(userId, academicYearId))
                .thenReturn(List.of(leafProgress, parentProgress));
        when(criteriaRepository.findAllById(anyList()))
                .thenReturn(List.of(leafCriteria, parentCriteria));

        service.markUncompleted(userId, leafId, academicYearId);

        assertEquals(2, leafProgress.getCompletedOccurrences());
        assertFalse(leafProgress.getIsCompleted());
        assertNull(leafProgress.getCompletedAt());
        assertEquals(0, parentProgress.getCompletedChildrenCount());
        assertFalse(parentProgress.getIsCompleted());
        verify(progressRepository).save(leafProgress);
        verify(progressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should decrement occurrences from 2 to 1 without decrementing parent")
    void markUncompleted_whenOccurrencesDropFromTwoToOne_shouldDecrementOccurrencesAndNotDecrementParent() {
        Long leafId = 402L;
        Criteria leafCriteria = new Criteria();
        leafCriteria.setId(leafId);
        leafCriteria.setRequiredOccurrences(3);

        StudentCriteriaProgress leafProgress = new StudentCriteriaProgress(userId, leafId, academicYearId);
        leafProgress.setCompletedOccurrences(2);
        leafProgress.setIsCompleted(false);

        when(progressRepository.findByUserIdAndCriteriaIdAndAcademicYearId(userId, leafId, academicYearId))
                .thenReturn(Optional.of(leafProgress));
        when(criteriaRepository.findById(leafId)).thenReturn(Optional.of(leafCriteria));

        service.markUncompleted(userId, leafId, academicYearId);

        assertEquals(1, leafProgress.getCompletedOccurrences());
        assertFalse(leafProgress.getIsCompleted());
        verify(progressRepository).save(leafProgress);
        verify(progressRepository, never()).delete(any());
        verify(progressRepository, never()).findAllByUserIdAndAcademicYearId(any(), any());
    }
}
