package com.ddd.application.service.semesterpoint.impl;

import com.ddd.domain.model.*;
import com.ddd.domain.repository.*;
import org.junit.jupiter.api.*;
import java.util.*;
import java.time.LocalDate;
import static com.ddd.application.service.semesterpoint.impl.StudentSemesterPointCalculatorTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real policy, coordinator and calculator/command; repositories retain sequential test state. */
class PointConfigurationRebuildTest {
    final StudentSemesterPointRepository points=mock(StudentSemesterPointRepository.class);
    final PointTransactionRepository transactions=mock(PointTransactionRepository.class);
    final PointCategoryRepository categories=mock(PointCategoryRepository.class);
    final SemesterRepository semesters=mock(SemesterRepository.class);
    final SemesterPointCategoryPolicy policy=new SemesterPointCategoryPolicy(semesters,categories);
    final StudentSemesterPointCommandServiceImpl command=new StudentSemesterPointCommandServiceImpl(points,transactions,policy,new StudentSemesterPointCalculator());
    final PointConfigurationRefresh refresh=new PointConfigurationRefresh(points,policy,command);
    final List<PointCategory> tree=new ArrayList<>();
    final Map<Long,StudentSemesterPoint> stored=new HashMap<>();
    final Semester semester=new Semester();
    @BeforeEach void setup() {
        semester.setEndDate(LocalDate.of(2026,6,1));
        when(semesters.findById(9L)).thenReturn(Optional.of(semester));
        when(categories.findActiveEffectiveAsOf(any())).thenAnswer(i->List.copyOf(tree));
        when(points.findAllScopes()).thenReturn(List.of(new StudentSemesterPointScope(7L,9L)));
        when(points.findAllByUserIdsAndSemesterId(List.of(7L),9L)).thenAnswer(i->new ArrayList<>(stored.values()));
        when(transactions.findAllByUserIdsAndSemesterId(List.of(7L),9L)).thenReturn(List.of(transaction(3,15,"MANUAL"),transaction(4,7,"CHECKOUT_REWARD")));
        doAnswer(i->{for(StudentSemesterPoint p:i.<List<StudentSemesterPoint>>getArgument(0))stored.put(p.getPointCategoryId(),p);return null;}).when(points).saveAll(anyList());
        tree.addAll(List.of(category(1,null,100,false),category(2,1L,100,false),category(3,2L,10,false),category(4,2L,20,false)));
    }
    private void adminUpdateLeaf(int maximum) {
        tree.get(1).setLevel(1);
        when(categories.findById(anyLong())).thenAnswer(i->tree.stream().filter(c->c.getId().equals(i.getArgument(0))).findFirst());
        when(categories.save(any())).thenAnswer(i->i.getArgument(0));
        var mapper=org.mapstruct.factory.Mappers.getMapper(com.ddd.application.mapper.PointCategoryDtoMapper.class);
        var admin=new com.ddd.application.service.pointcategory.impl.PointCategoryServiceImpl(categories,semesters,mapper,refresh);
        admin.updatePointCategory(3L,new com.ddd.application.dto.pointcategory.PointCategoryUpdateDto(
            "Leaf",null,maximum,LocalDate.of(2026,1,1),2L,1,true,false));
    }
    @Test void capChangesKeepRawLeafAndRebuildBothAncestors() {
        command.rebuildSemesterPoints(7L,9L);assertEquals(17,stored.get(1L).getTotalPoint());
        adminUpdateLeaf(5);
        assertEquals(15,stored.get(3L).getTotalPoint());assertEquals(12,stored.get(2L).getTotalPoint());assertEquals(12,stored.get(1L).getTotalPoint());
        adminUpdateLeaf(20);assertEquals(22,stored.get(1L).getTotalPoint());
    }
    @Test void zeroMaximumPreservesRawLeafAndRemovesItsContribution() {
        command.rebuildSemesterPoints(7L,9L);
        var before=refresh.capture();tree.get(2).setMaximum(0);refresh.refresh(before);
        assertEquals(15,stored.get(3L).getTotalPoint());assertEquals(7,stored.get(1L).getTotalPoint());
    }
    @Test void sumMaxReparentAndDeactivateRebuildOldAndNewBranches() {
        var before=refresh.capture();tree.get(1).setIsMaxChild(true);refresh.refresh(before);assertEquals(10,stored.get(1L).getTotalPoint());
        before=refresh.capture();tree.get(2).setParentCategoryId(1L);refresh.refresh(before);
        assertEquals(7,stored.get(2L).getTotalPoint());assertEquals(17,stored.get(1L).getTotalPoint());
        before=refresh.capture();tree.get(1).setActive(false);refresh.refresh(before);
        assertEquals(0,stored.get(2L).getTotalPoint());assertEquals(0,stored.get(4L).getTotalPoint());assertEquals(10,stored.get(1L).getTotalPoint());
    }
    @Test void changingSemesterDateChangesEffectiveVersionAndIncludesLedgerOnlyScope() {
        when(categories.findActiveEffectiveAsOf(any())).thenAnswer(i->i.getArgument(0).equals(LocalDate.of(2026,6,1))?List.copyOf(tree):List.of(category(99,null,20,false)));
        var before=refresh.capture();semester.setEndDate(LocalDate.of(2026,12,1));refresh.refresh(before);
        assertEquals(1,stored.size());assertTrue(stored.containsKey(99L));
    }
    @Test void changingDateApplyRemovesUnreachableBranchAndZerosExistingSnapshots() {
        for(var c:tree)c.setDateApply(LocalDate.of(2026,1,1));
        when(categories.findActiveEffectiveAsOf(any())).thenAnswer(i->tree.stream().filter(c->!c.getDateApply().isAfter(i.getArgument(0))).toList());
        command.rebuildSemesterPoints(7L,9L);assertEquals(17,stored.get(1L).getTotalPoint());
        var before=refresh.capture();tree.getFirst().setDateApply(LocalDate.of(2026,7,1));refresh.refresh(before);
        assertTrue(stored.values().stream().allMatch(p->p.getTotalPoint()==0));
        verify(transactions,never()).save(any());
    }
}
