package com.ddd.application.service.criteriaprogress.impl;

import com.ddd.application.dto.criteria.*;
import com.ddd.application.service.criteria.CriteriaService;
import com.ddd.domain.enums.CriteriaScopeEnum;
import com.ddd.infrastructure.entity.*;
import com.ddd.infrastructure.repository.jpaRepository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CriteriaServerProjectionTest {
    @Mock CriteriaService criteriaService;
    @Mock StudentCriteriaTransactionJpaRepository transactions;
    @Mock UserJpaRepository users;
    @Mock AcademicYearJpaRepository years;
    @InjectMocks StudentCriteriaProgressQueryServiceImpl service;

    private void setup(List<StudentCriteriaTransactionJpaEntity> records, int required) {
        UserJpaEntity user = new UserJpaEntity(); user.setId(7L);
        AcademicYearJpaEntity year = new AcademicYearJpaEntity(); year.setId(9L); year.setCode("2026-2027");
        when(users.findByEmail("s")).thenReturn(user);
        when(years.findById(9L)).thenReturn(Optional.of(year));
        var leaf = new CriteriaTreeNodeDto(2L,"Leaf",null,"UNIVERSITY",2,1,null,required,List.of());
        var root = new CriteriaTreeNodeDto(1L,"Root",null,"UNIVERSITY",1,1,null,1,List.of(leaf));
        when(criteriaService.getCriteriaHierarchy(CriteriaScopeEnum.UNIVERSITY)).thenReturn(List.of(root));
        when(transactions.findByUserIdAndAcademicYearId(7L,9L)).thenReturn(records);
    }
    private StudentCriteriaTransactionJpaEntity tx(long id, Long eventId, String source) {
        var tx = new StudentCriteriaTransactionJpaEntity(); tx.setId(id); tx.setCompletionSource(source);
        var category = new CriteriaJpaEntity(); category.setId(2L); tx.setCriteria(category); tx.setNote("note");
        if (eventId != null) { var event = new EventJpaEntity(); event.setId(eventId); event.setName("Event"); tx.setEvent(event); }
        return tx;
    }
    @Test void combinesManualAndSystemOccurrencesAndRollsUp() {
        setup(List.of(tx(1,10L,"EVENT_CHECKOUT"),tx(2,10L,"EVENT_CHECKOUT"),tx(3,11L,"EVENT_CHECKOUT"),tx(4,null,"MANUAL")),3);
        var result=service.getProgress("s",9L,CriteriaScopeEnum.UNIVERSITY);
        var leaf=result.criterias().get(0).children().get(0);
        assertEquals(3,leaf.completedOccurrences()); assertTrue(leaf.isCompleted());
        assertTrue(leaf.isSelfEvaluated()); assertTrue(leaf.isSystemRecorded()); assertFalse(leaf.isSystemCompleted());
        assertEquals(4L,leaf.manualTransactionId()); assertEquals(1,result.completedMainCriteria());
        assertTrue(result.isEligible());
    }
    @Test void rollsUpAndReturnsSummaryAndSingleNodeConsistently() {
        setup(List.of(tx(1,10L,"EVENT_CHECKOUT"),tx(2,11L,"EVENT_CHECKOUT"),tx(3,12L,"EVENT_CHECKOUT"),tx(4,13L,"EVENT_CHECKOUT")),3);
        var result=service.getProgress("s",9L,CriteriaScopeEnum.UNIVERSITY);
        assertEquals(100,result.completionPercentage()); assertTrue(result.isEligible());
        assertEquals(1,result.criterias().get(0).completedChildrenCount());
        var leaf=result.criterias().get(0).children().get(0);
        assertEquals(4,leaf.completedOccurrences()); assertTrue(leaf.isSystemCompleted());
        assertEquals(leaf,service.getCriteriaProgress("s",2L,9L,CriteriaScopeEnum.UNIVERSITY));
    }
    @Test void manualCompletesSingleOccurrenceOnly() {
        setup(List.of(tx(1,null,"MANUAL")),1);
        var leaf=service.getProgress("s",9L,CriteriaScopeEnum.UNIVERSITY).criterias().get(0).children().get(0);
        assertTrue(leaf.isCompleted()); assertFalse(leaf.isSystemCompleted()); assertEquals("MANUAL",leaf.completionSource());
    }
    @Test void emptyConfigurationHasZeroPercentageAndIsNotEligible() {
        setup(List.of(),1);
        when(criteriaService.getCriteriaHierarchy(CriteriaScopeEnum.UNIVERSITY)).thenReturn(List.of());
        var result=service.getProgress("s",9L,CriteriaScopeEnum.UNIVERSITY);
        assertEquals(0,result.completionPercentage()); assertFalse(result.isEligible()); assertTrue(result.criterias().isEmpty());
    }
    @Test void ignoresInactiveTransactions() {
        var inactive = tx(1,10L,"EVENT_CHECKOUT"); inactive.setIsActive(false);
        setup(List.of(inactive),1);
        var result = service.getProgress("s",9L,CriteriaScopeEnum.UNIVERSITY);
        assertEquals(0,result.completedMainCriteria());
        assertEquals(0,result.criterias().get(0).children().get(0).completedOccurrences());
    }
    @Test void exposesStandardsBelowLevelZeroWrapperAndPreservesSingleNodeLookup() {
        setup(List.of(),1);
        var standard = new CriteriaTreeNodeDto(2L,"Standard",null,"UNIVERSITY",1,1,null,1,List.of());
        var wrapper = new CriteriaTreeNodeDto(1L,"Title",null,"UNIVERSITY",0,1,5,1,List.of(standard));
        when(criteriaService.getCriteriaHierarchy(CriteriaScopeEnum.UNIVERSITY)).thenReturn(List.of(wrapper));
        var result = service.getProgress("s",9L,CriteriaScopeEnum.UNIVERSITY);
        assertEquals(2L,result.criterias().get(0).id());
        assertEquals(1L,service.getCriteriaProgress("s",1L,9L,CriteriaScopeEnum.UNIVERSITY).id());
    }

    @Test void threeManualRecordsCompleteThresholdAndDeletingOneUncompletesAncestors() {
        var records = new ArrayList<>(List.of(tx(1,null,"MANUAL"),tx(2,null,"MANUAL"),tx(3,null,"MANUAL")));
        setup(records,3);
        assertTrue(service.getProgress("s",9L,CriteriaScopeEnum.UNIVERSITY).isEligible());
        records.remove(2);
        var result=service.getProgress("s",9L,CriteriaScopeEnum.UNIVERSITY);
        assertFalse(result.isEligible());
        assertFalse(result.criterias().get(0).isCompleted());
        assertEquals(2,result.criterias().get(0).children().get(0).completedOccurrences());
    }
    @Test void changingOccurrenceThresholdImmediatelyChangesReadResultInBothDirections() {
        setup(List.of(tx(1,null,"MANUAL"),tx(2,null,"MANUAL"),tx(3,10L,"EVENT_CHECKOUT")),3);
        assertTrue(service.getProgress("s",9L,CriteriaScopeEnum.UNIVERSITY).isEligible());
        var leaf = new CriteriaTreeNodeDto(2L,"Leaf",null,"UNIVERSITY",2,1,null,4,List.of());
        var parent = new CriteriaTreeNodeDto(1L,"Root",null,"UNIVERSITY",1,1,1,1,List.of(leaf));
        when(criteriaService.getCriteriaHierarchy(CriteriaScopeEnum.UNIVERSITY)).thenReturn(List.of(parent));
        assertFalse(service.getProgress("s",9L,CriteriaScopeEnum.UNIVERSITY).isEligible());
        leaf = new CriteriaTreeNodeDto(2L,"Leaf",null,"UNIVERSITY",2,1,null,2,List.of());
        parent = new CriteriaTreeNodeDto(1L,"Root",null,"UNIVERSITY",1,1,1,1,List.of(leaf));
        when(criteriaService.getCriteriaHierarchy(CriteriaScopeEnum.UNIVERSITY)).thenReturn(List.of(parent));
        assertTrue(service.getProgress("s",9L,CriteriaScopeEnum.UNIVERSITY).isEligible());
    }
    @Test void nullParentThresholdDefaultsToOneAndAdminChangingItReevaluatesParent() {
        setup(List.of(tx(1,null,"MANUAL")),1);
        var achieved = new CriteriaTreeNodeDto(2L,"Leaf",null,"UNIVERSITY",2,1,null,1,List.of());
        var other = new CriteriaTreeNodeDto(3L,"Other",null,"UNIVERSITY",2,2,null,1,List.of());
        var parent = new CriteriaTreeNodeDto(1L,"Root",null,"UNIVERSITY",1,1,null,1,List.of(achieved,other));
        when(criteriaService.getCriteriaHierarchy(CriteriaScopeEnum.UNIVERSITY)).thenReturn(List.of(parent));
        var result=service.getProgress("s",9L,CriteriaScopeEnum.UNIVERSITY);
        assertTrue(result.isEligible()); assertEquals(1,result.criterias().get(0).requiredChildren());
        parent = new CriteriaTreeNodeDto(1L,"Root",null,"UNIVERSITY",1,1,2,1,List.of(achieved,other));
        when(criteriaService.getCriteriaHierarchy(CriteriaScopeEnum.UNIVERSITY)).thenReturn(List.of(parent));
        assertFalse(service.getProgress("s",9L,CriteriaScopeEnum.UNIVERSITY).isEligible());
    }

}
