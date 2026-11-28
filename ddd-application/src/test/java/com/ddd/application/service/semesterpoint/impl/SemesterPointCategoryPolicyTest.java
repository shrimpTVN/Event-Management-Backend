package com.ddd.application.service.semesterpoint.impl;

import com.ddd.domain.model.Semester;
import com.ddd.domain.repository.*;
import com.ddd.domain.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static com.ddd.application.service.semesterpoint.impl.StudentSemesterPointCalculatorTest.category;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SemesterPointCategoryPolicyTest {
    @Test void acceptsOnlyActiveLeavesInSemestersEffectiveTree() {
        var semesters=mock(SemesterRepository.class);var categories=mock(PointCategoryRepository.class);
        var semester=new Semester();semester.setEndDate(LocalDate.of(2026,12,31));
        when(semesters.findById(9L)).thenReturn(Optional.of(semester));
        var inactive=category(3,1L,10,false);inactive.setActive(false);
        when(categories.findActiveEffectiveAsOf(semester.getEndDate())).thenReturn(List.of(category(1,null,20,false),category(2,1L,10,false),inactive));
        var policy=new SemesterPointCategoryPolicy(semesters,categories);
        assertDoesNotThrow(()->policy.requireLeaves(9L,List.of(2L)));
        assertThrows(BusinessException.class,()->policy.requireLeaves(9L,List.of(1L)));
        assertThrows(BusinessException.class,()->policy.requireLeaves(9L,List.of(3L)));
        assertThrows(BusinessException.class,()->policy.requireLeaves(9L,List.of(99L)));
    }
}
