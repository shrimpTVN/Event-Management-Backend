package com.ddd.application.service.criteriaprogress.impl;
import com.ddd.application.service.criteriaprogress.StudentCriteriaProgressCommandService;
import com.ddd.domain.repository.*;
import com.ddd.domain.model.StudentCriteriaTransaction;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.infrastructure.repository.jpaRepository.StudentCriteriaTransactionJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentCriteriaTransactionCommandServiceImplTest {
 @Mock StudentCriteriaTransactionRepository criteriaTransactionRepository;
 @Mock CriteriaRepository criteriaRepository;
 @Mock AcademicYearRepository academicYearRepository;
 @Mock EventRepository eventRepository;
 @Mock SemesterRepository semesterRepository;
 @Mock StudentCriteriaProgressCommandService criteriaProgressCommandService;
 @Mock SecurityContextPort securityContextPort;
 @Mock CriteriaProgressLock progressLock;
 @Mock StudentCriteriaTransactionJpaRepository transactionJpaRepository;
 @InjectMocks StudentCriteriaTransactionCommandServiceImpl service;
 @Test void propagatesRebuildFailureSoOuterTransactionCanRollbackDeletion() {
  when(securityContextPort.getCurrentUserId()).thenReturn(7L);
  var tx = new StudentCriteriaTransaction(7L,2L,9L,null,"MANUAL","note");
  when(criteriaTransactionRepository.findById(1L)).thenReturn(Optional.of(tx));
  doThrow(new IllegalStateException("rebuild failed")).when(criteriaProgressCommandService).markUncompleted(7L,2L,9L);
  assertThrows(IllegalStateException.class,()->service.deleteTransaction(1L));
  var order = inOrder(progressLock,criteriaTransactionRepository);
  order.verify(criteriaTransactionRepository).findById(1L);
  order.verify(progressLock).acquire(7L,9L);
  order.verify(criteriaTransactionRepository).findById(1L);
  order.verify(criteriaTransactionRepository).delete(tx);
 }
 @Test void updatesOnlyNoteWithoutCreatingAnotherOccurrence() {
  when(securityContextPort.getCurrentUserId()).thenReturn(7L);
  var tx = new StudentCriteriaTransaction(7L,2L,9L,null,"MANUAL","old");
  tx.setId(1L);
  when(criteriaTransactionRepository.findById(1L)).thenReturn(Optional.of(tx));
  service.updateTransactionNote(1L,"new reason");
  assertEquals(1L,tx.getId()); assertEquals("new reason",tx.getNote());
  assertEquals(2L,tx.getCriteriaId()); assertEquals(9L,tx.getAcademicYearId());
  verify(criteriaTransactionRepository).save(tx);
  verify(criteriaProgressCommandService).markCompleted(7L,2L,9L);
 }
 @Test void cannotEditAnotherStudentsManualOrOwnSystemRecord() {
  when(securityContextPort.getCurrentUserId()).thenReturn(7L);
  var other = new StudentCriteriaTransaction(8L,2L,9L,null,"MANUAL","old");
  when(criteriaTransactionRepository.findById(1L)).thenReturn(Optional.of(other));
  assertThrows(IllegalArgumentException.class,()->service.updateTransactionNote(1L,"new"));
  var system = new StudentCriteriaTransaction(7L,2L,9L,10L,"EVENT_CHECKOUT","old");
  when(criteriaTransactionRepository.findById(1L)).thenReturn(Optional.of(system));
  assertThrows(IllegalArgumentException.class,()->service.updateTransactionNote(1L,"new"));
  verify(criteriaTransactionRepository,never()).save(any());
 }

}
