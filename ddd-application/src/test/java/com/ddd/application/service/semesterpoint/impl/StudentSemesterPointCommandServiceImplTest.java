package com.ddd.application.service.semesterpoint.impl;

import com.ddd.domain.model.*;
import com.ddd.domain.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.*;
import static com.ddd.application.service.semesterpoint.impl.StudentSemesterPointCalculatorTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentSemesterPointCommandServiceImplTest {
    @Mock StudentSemesterPointRepository points;
    @Mock PointTransactionRepository transactions;
    @Mock SemesterPointCategoryPolicy categoryPolicy;
    @Spy StudentSemesterPointCalculator calculator = new StudentSemesterPointCalculator();
    @InjectMocks StudentSemesterPointCommandServiceImpl service;
    final Map<Long,StudentSemesterPoint> stored = new HashMap<>();
    final List<PointTransaction> ledger = new ArrayList<>();

    @BeforeEach void setup() {
        when(categoryPolicy.effectiveCategories(9L)).thenReturn(List.of(category(1,null,100,false),category(2,1L,10,false),category(3,2L,20,false)));
        when(transactions.findAllByUserIdsAndSemesterId(List.of(7L),9L)).thenAnswer(i->new ArrayList<>(ledger));
        when(points.findAllByUserIdsAndSemesterId(List.of(7L),9L)).thenAnswer(i->new ArrayList<>(stored.values()));
        doAnswer(i->{
            for (StudentSemesterPoint point : i.<List<StudentSemesterPoint>>getArgument(0)) {
                if(point.getId()==null) point.setId(point.getPointCategoryId()+100);
                stored.put(point.getPointCategoryId(),point);
            }
            return null;
        }).when(points).saveAll(anyList());
    }
    @Test void rebuildingTwiceKeepsIdsAuditAndRawPointsStable() {
        ledger.add(transaction(3,15,"MANUAL"));
        service.rebuildSemesterPoints(7L,9L);
        var leaf=stored.get(3L);
        Instant created=leaf.getCreatedAt(); leaf.setFinalizedAt(Instant.EPOCH);
        service.rebuildSemesterPoints(7L,9L);
        assertEquals(3,stored.size());assertEquals(103L,stored.get(3L).getId());
        assertEquals(created,stored.get(3L).getCreatedAt());assertEquals(Instant.EPOCH,stored.get(3L).getFinalizedAt());
        assertEquals(15,stored.get(2L).getTotalPoint());assertEquals(10,stored.get(1L).getTotalPoint());
    }
    @Test void deletingOneOfTwoTransactionsRecomputesAllAncestors() {
        ledger.add(transaction(3,8,"MANUAL"));ledger.add(transaction(3,7,"MANUAL"));
        service.rebuildSemesterPoints(7L,9L);
        ledger.removeFirst();service.rebuildSemesterPoints(7L,9L);
        assertEquals(7,stored.get(3L).getTotalPoint());assertEquals(7,stored.get(2L).getTotalPoint());assertEquals(7,stored.get(1L).getTotalPoint());
    }
    @Test void rebuildRepairsOldSnapshotsAndZerosRemovedCategories() {
        var old = new StudentSemesterPoint(7L,9L,3L,999);old.setId(77L);stored.put(3L,old);
        var stale=new StudentSemesterPoint(7L,9L,99L,20);stored.put(99L,stale);
        ledger.add(transaction(3,5,"MANUAL"));service.rebuildSemesterPoints(7L,9L);
        assertEquals(5,stored.get(3L).getTotalPoint());assertEquals(77L,stored.get(3L).getId());assertEquals(0,stored.get(99L).getTotalPoint());
    }
    @Test void maxChildSwitchesWinnerAfterDeletion() {
        when(categoryPolicy.effectiveCategories(9L)).thenReturn(List.of(category(1,null,100,true),category(2,1L,10,false),category(3,1L,20,false)));
        ledger.add(transaction(2,8,"MANUAL"));ledger.add(transaction(3,7,"CHECKOUT_REWARD"));
        service.rebuildSemesterPoints(7L,9L);assertEquals(8,stored.get(1L).getTotalPoint());
        ledger.removeFirst();service.rebuildSemesterPoints(7L,9L);assertEquals(7,stored.get(1L).getTotalPoint());assertEquals(7,stored.get(1L).getSystemPointRecord());
    }
}
