package com.ddd.application.service.criteriaprogress.impl;
import com.ddd.application.dto.criteria.CriteriaTreeNodeDto;
import com.ddd.application.service.criteria.CriteriaService;
import com.ddd.domain.model.StudentCriteriaProgress;
import com.ddd.domain.repository.StudentCriteriaProgressRepository;
import com.ddd.infrastructure.repository.jpaRepository.StudentCriteriaTransactionJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentCriteriaProgressCommandServiceImplTest {
 @Mock CriteriaService criteriaService;
 @Mock StudentCriteriaProgressRepository progressRepository;
 @Mock StudentCriteriaTransactionJpaRepository transactions;
 @Mock CriteriaProgressLock progressLock;
 @InjectMocks StudentCriteriaProgressCommandServiceImpl service;
 @Captor ArgumentCaptor<List<StudentCriteriaProgress>> saved;
 @Test void rebuildsStaleCompletionFromRemainingTransactionsUnderLock() {
  var stale = new StudentCriteriaProgress(7L,2L,9L); stale.setIsCompleted(true); stale.setCompletedOccurrences(8);
  var leaf = new CriteriaTreeNodeDto(2L,"Leaf",null,"UNIVERSITY",1,1,null,3,List.of());
  when(criteriaService.getCriteriaHierarchy(null)).thenReturn(List.of(leaf));
  when(transactions.findByUserIdAndAcademicYearId(7L,9L)).thenReturn(List.of());
  when(progressRepository.findAllByUserIdAndAcademicYearId(7L,9L)).thenReturn(List.of(stale));
  service.markUncompleted(7L,2L,9L);
  var order = inOrder(progressLock,transactions,progressRepository);
  order.verify(progressLock).acquire(7L,9L);
  order.verify(transactions).findByUserIdAndAcademicYearId(7L,9L);
  verify(progressRepository).saveAll(saved.capture());
  assertFalse(saved.getValue().get(0).getIsCompleted()); assertEquals(0,saved.getValue().get(0).getCompletedOccurrences());
 }
 @Test void locksDistinctStudentsInStableOrder() {
  when(criteriaService.getCriteriaHierarchy(null)).thenReturn(List.of());
  service.bulkMarkCompleted(List.of(8L,7L,8L),2L,9L);
  var order = inOrder(progressLock);order.verify(progressLock).acquire(7L,9L);order.verify(progressLock).acquire(8L,9L);
  verify(progressLock,times(2)).acquire(anyLong(),eq(9L));
 }
}
