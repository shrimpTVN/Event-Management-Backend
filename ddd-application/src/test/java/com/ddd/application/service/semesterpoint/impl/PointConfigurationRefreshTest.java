package com.ddd.application.service.semesterpoint.impl;

import com.ddd.domain.model.*;
import com.ddd.domain.repository.StudentSemesterPointRepository;
import com.ddd.application.service.semesterpoint.StudentSemesterPointCommandService;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;

class PointConfigurationRefreshTest {
    @Test void changingEffectiveConfigurationRebuildsScopesEvenWithoutSnapshots() {
        var points=mock(StudentSemesterPointRepository.class);
        var policy=mock(SemesterPointCategoryPolicy.class);
        var command=mock(StudentSemesterPointCommandService.class);
        when(points.findAllScopes()).thenReturn(List.of(new StudentSemesterPointScope(7L,9L),new StudentSemesterPointScope(8L,9L)));
        var leaf=new PointCategory();leaf.setId(1L);leaf.setMaximum(10);leaf.setIsMaxChild(false);
        when(policy.effectiveCategories(9L)).thenReturn(List.of(leaf));
        var refresh=new PointConfigurationRefresh(points,policy,command);
        var before=refresh.capture();
        leaf.setMaximum(5);
        refresh.refresh(before);
        verify(command).rebuildSemesterPoints(List.of(7L,8L),9L);
        var unchanged=refresh.capture();
        refresh.refresh(unchanged);
        verifyNoMoreInteractions(command);
    }
}
