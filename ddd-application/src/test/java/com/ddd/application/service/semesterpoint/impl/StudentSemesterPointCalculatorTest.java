package com.ddd.application.service.semesterpoint.impl;

import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.model.PointCategory;
import com.ddd.domain.model.PointTransaction;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StudentSemesterPointCalculatorTest {
    private final StudentSemesterPointCalculator calculator = new StudentSemesterPointCalculator();
    static PointCategory category(long id, Long parent, int max, boolean maxChild) {
        var category = new PointCategory();
        category.setId(id); category.setParentCategoryId(parent);
        category.setMaximum(max); category.setIsMaxChild(maxChild);
        return category;
    }
    static PointTransaction transaction(long category, int points, String type) {
        var transaction = new PointTransaction();
        transaction.setUserId(7L); transaction.setSemesterId(9L);
        transaction.setPointCategoryId(category); transaction.setPoints(points);
        transaction.setType(type); transaction.setStatus("CONFIRMED");
        return transaction;
    }
    @Test void retainsRawLeafAndCapsContributionAtEveryLevel() {
        var result = calculator.calculate(7L,9L,
                List.of(category(1,null,100,false),category(2,1L,10,false),category(3,2L,20,false)),
                List.of(transaction(3,8,"CHECKOUT_REWARD"),transaction(3,7,"MANUAL")));
        var leaf = result.points().stream().filter(p->p.getPointCategoryId()==3).findFirst().orElseThrow();
        var parent = result.points().stream().filter(p->p.getPointCategoryId()==2).findFirst().orElseThrow();
        var root = result.points().stream().filter(p->p.getPointCategoryId()==1).findFirst().orElseThrow();
        assertEquals(15,leaf.getTotalPoint()); assertEquals(15,parent.getTotalPoint());
        assertEquals(10,root.getTotalPoint()); assertTrue(result.nodeMetadata().get(2L).isAtMaximum());
    }
    @Test void countsExistingTransactionsRegardlessOfStatusAndIsolatesStudentAndSemester() {
        var pending=transaction(1,10,"MANUAL");pending.setStatus("PENDING");
        var revoked=transaction(1,10,"MANUAL");revoked.setStatus("REVOKED");
        var other=transaction(1,10,"MANUAL");other.setUserId(8L);
        var wrongSemester=transaction(1,10,"MANUAL");wrongSemester.setSemesterId(10L);
        var result=calculator.calculate(7L,9L,List.of(category(1,null,10,false)),
                List.of(pending,revoked,other,wrongSemester));
        assertEquals(20,result.points().getFirst().getTotalPoint());
    }
    @Test void choosesMaxChildByRawManualThenSmallerIdRegardlessOfInputOrder() {
        var result=calculator.calculate(7L,9L,List.of(category(1,null,20,true),category(3,1L,10,false),category(2,1L,10,false)),
                List.of(transaction(3,10,"CHECKOUT_REWARD"),transaction(2,100,"MANUAL")));
        assertEquals(2L,result.nodeMetadata().get(1L).selectedChildId());
        assertFalse(result.nodeMetadata().get(3L).contributesToParent());
        var tie=calculator.calculate(7L,9L,List.of(category(1,null,20,true),category(3,1L,10,false),category(2,1L,10,false)),
                List.of(transaction(3,10,"MANUAL"),transaction(2,10,"MANUAL")));
        assertEquals(2L,tie.nodeMetadata().get(1L).selectedChildId());
    }
    @Test void signedAdjustmentsAreOrderIndependentAndZeroMaximumContributesZero() {
        var result=calculator.calculate(7L,9L,List.of(category(1,null,0,false)),
                List.of(transaction(1,-8,"PENALTY_DEDUCTION"),transaction(1,10,"MANUAL")));
        assertEquals(2,result.points().getFirst().getTotalPoint());
        assertEquals(0,result.nodeMetadata().get(1L).completionPercentage());
        assertFalse(result.nodeMetadata().get(1L).isAtMaximum());
    }
    @Test void excludesParentAndNonEffectiveAwardsFromRollup() {
        var result=calculator.calculate(7L,9L,List.of(category(1,null,10,false),category(2,1L,10,false)),
                List.of(transaction(1,5,"MANUAL"),transaction(99,5,"MANUAL"),transaction(2,2,"MANUAL")));
        assertEquals(2,result.points().stream().filter(p->p.getPointCategoryId()==1).findFirst().orElseThrow().getTotalPoint());
    }
    @Test void rejectsCyclesAndIntegerOverflow() {
        assertThrows(BusinessException.class,()->calculator.calculate(7L,9L,
                List.of(category(1,2L,10,false),category(2,1L,10,false)),List.of()));
        assertThrows(BusinessException.class,()->calculator.calculate(7L,9L,List.of(category(1,null,10,false)),
                List.of(transaction(1,Integer.MAX_VALUE,"MANUAL"),transaction(1,1,"MANUAL"))));
    }
    @Test void leafRetainsFifteenRawPointsWhenItsMaximumIsTen() {
        var result=calculator.calculate(7L,9L,List.of(category(1,null,10,false)),
                List.of(transaction(1,8,"CHECKOUT_REWARD"),transaction(1,7,"MANUAL")));
        assertEquals(15,result.points().getFirst().getTotalPoint());
        assertEquals(8,result.points().getFirst().getSystemPointRecord());
        assertEquals(7,result.points().getFirst().getManualPointRecord());
        assertEquals(100,result.nodeMetadata().get(1L).completionPercentage());
    }

}
