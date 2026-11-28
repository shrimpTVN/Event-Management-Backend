package com.ddd.application.service.semesterpoint.impl;

import com.ddd.domain.model.*;
import com.ddd.domain.repository.*;
import com.ddd.application.service.semesterpoint.StudentSemesterPointCommandService;
import org.junit.jupiter.api.*;
import java.util.*;
import static com.ddd.application.service.semesterpoint.impl.StudentSemesterPointCalculatorTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SemesterPointReconciliationServiceTest {
    final StudentSemesterPointRepository snapshots=mock(StudentSemesterPointRepository.class);
    final PointTransactionRepository transactions=mock(PointTransactionRepository.class);
    final SemesterPointCategoryPolicy policy=mock(SemesterPointCategoryPolicy.class);
    final StudentSemesterPointCommandService command=mock(StudentSemesterPointCommandService.class);
    final SemesterPointReconciliationService service=new SemesterPointReconciliationService(snapshots,transactions,policy,new StudentSemesterPointCalculator(),command);
    @BeforeEach void setup() {
        when(snapshots.findAllScopes()).thenReturn(List.of(new StudentSemesterPointScope(7L,9L)));
        when(policy.effectiveCategories(9L)).thenReturn(List.of(category(1,null,10,false)));
    }
    @Test void previewReportsRawDiffAndInvalidTransactionsWithoutWrites() {
        var valid=transaction(1,15,"MANUAL");valid.setId(42L);
        var invalid=transaction(99,2,"MANUAL");invalid.setId(43L);
        when(transactions.findAllByUserIdsAndSemesterId(List.of(7L),9L)).thenReturn(List.of(valid,invalid));
        when(snapshots.findAllByUserIdAndSemesterId(7L,9L)).thenReturn(List.of(new StudentSemesterPoint(7L,9L,1L,10)));
        var report=service.preview();
        assertFalse(report.applied());assertEquals(15,report.scopes().getFirst().changes().getFirst().afterTotal());
        assertEquals(43L,report.scopes().getFirst().issues().getFirst().transactionId());
        verifyNoInteractions(command);verify(snapshots,never()).saveAll(anyList());verify(transactions,never()).save(any());
    }
    @Test void refusesMissingLedgerEvenForRemovedCategoryWithAnotherValidLedger() {
        when(transactions.findAllByUserIdsAndSemesterId(List.of(7L),9L)).thenReturn(List.of(transaction(1,5,"MANUAL")));
        when(snapshots.findAllByUserIdAndSemesterId(7L,9L)).thenReturn(List.of(new StudentSemesterPoint(7L,9L,99L,8)));
        assertTrue(service.preview().scopes().getFirst().missingLedger());
        assertThrows(RuntimeException.class,()->service.repair(false));verifyNoInteractions(command);
        assertTrue(service.repair(true).applied());verify(command).rebuildSemesterPoints(List.of(7L),9L);
    }
    @Test void repeatedRepairOnlyRebuildsSnapshotsAndKeepsManualIds() {
        var first=transaction(1,8,"MANUAL");first.setId(41L);
        var second=transaction(1,5,"MANUAL");second.setId(42L);
        when(transactions.findAllByUserIdsAndSemesterId(List.of(7L),9L)).thenReturn(List.of(first,second));
        var stored=new ArrayList<StudentSemesterPoint>();
        when(snapshots.findAllByUserIdAndSemesterId(7L,9L)).thenAnswer(i->List.copyOf(stored));
        doAnswer(i->{stored.clear();var point=new StudentSemesterPoint(7L,9L,1L,13);point.setManualPointRecord(13);stored.add(point);return null;})
            .when(command).rebuildSemesterPoints(List.of(7L),9L);
        service.repair(false);assertTrue(service.repair(false).scopes().getFirst().changes().isEmpty());
        assertEquals(41L,first.getId());assertEquals(42L,second.getId());verify(transactions,never()).save(any());
    }
    @Test void nullExceptionMessageStillBlocksRepair() {
        when(policy.effectiveCategories(9L)).thenThrow(new IllegalStateException());
        assertEquals("IllegalStateException",service.preview().scopes().getFirst().error());
        assertThrows(RuntimeException.class,()->service.repair(true));verifyNoInteractions(command);
    }
}
