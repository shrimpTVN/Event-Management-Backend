package com.ddd.application.service.semesterpoint.impl;

import com.ddd.domain.enums.PointTransactionTypeEnum;
import com.ddd.domain.model.PointCategory;
import com.ddd.domain.model.Semester;
import com.ddd.domain.model.StudentSemesterPoint;
import com.ddd.domain.repository.PointCategoryRepository;
import com.ddd.domain.repository.SemesterRepository;
import com.ddd.domain.repository.StudentSemesterPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentSemesterPointCommandServiceImplTest {

    @Mock
    private StudentSemesterPointRepository studentSemesterPointRepository;

    @Mock
    private PointCategoryRepository pointCategoryRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @InjectMocks
    private StudentSemesterPointCommandServiceImpl service;

    private final Long userId = 100L;
    private final Long semesterId = 2024L;
    private final LocalDate semesterEndDate = LocalDate.of(2026, 12, 31);

    @BeforeEach
    void setUpSemester() {
        Semester semester = new Semester();
        semester.setId(semesterId);
        semester.setEndDate(semesterEndDate);
        lenient().when(semesterRepository.findById(semesterId)).thenReturn(Optional.of(semester));
    }

    @Test
    @DisplayName("Should update leaf point and sum ancestors when isMaxChild is false")
    void recordPointAndPropagate_whenManualPoint_shouldUpdateLeafAndSumAncestors() {
        Long leafId = 10L;
        Long parentId = 2L;
        Long rootId = 1L;

        PointCategory leaf = new PointCategory();
        leaf.setId(leafId);
        leaf.setParentCategoryId(parentId);
        leaf.setMaximum(10);
        leaf.setIsMaxChild(false);

        PointCategory parent = new PointCategory();
        parent.setId(parentId);
        parent.setParentCategoryId(rootId);
        parent.setMaximum(20);
        parent.setIsMaxChild(false);

        PointCategory root = new PointCategory();
        root.setId(rootId);
        root.setParentCategoryId(null);
        root.setMaximum(50);
        root.setIsMaxChild(false);

        when(pointCategoryRepository.findActiveEffectiveAsOf(semesterEndDate)).thenReturn(List.of(leaf, parent, root));
        when(studentSemesterPointRepository.findAllByUserIdsAndSemesterId(List.of(userId), semesterId))
                .thenReturn(new ArrayList<>());

        service.recordPointAndPropagate(userId, semesterId, leafId, 5, PointTransactionTypeEnum.MANUAL);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StudentSemesterPoint>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentSemesterPointRepository).saveAll(captor.capture());

        List<StudentSemesterPoint> saved = captor.getValue();
        assertEquals(3, saved.size());

        StudentSemesterPoint savedLeaf = saved.stream().filter(p -> p.getPointCategoryId().equals(leafId)).findFirst().orElseThrow();
        assertEquals(5, savedLeaf.getManualPointRecord());
        assertEquals(0, savedLeaf.getSystemPointRecord());
        assertEquals(5, savedLeaf.getTotalPoint());

        StudentSemesterPoint savedParent = saved.stream().filter(p -> p.getPointCategoryId().equals(parentId)).findFirst().orElseThrow();
        assertEquals(5, savedParent.getManualPointRecord());
        assertEquals(0, savedParent.getSystemPointRecord());
        assertEquals(5, savedParent.getTotalPoint());

        StudentSemesterPoint savedRoot = saved.stream().filter(p -> p.getPointCategoryId().equals(rootId)).findFirst().orElseThrow();
        assertEquals(5, savedRoot.getManualPointRecord());
        assertEquals(0, savedRoot.getSystemPointRecord());
        assertEquals(5, savedRoot.getTotalPoint());
    }

    @Test
    @DisplayName("Should take maximum child point when parent has isMaxChild true")
    void recordPointAndPropagate_whenIsMaxChildIsTrue_shouldTakeMaxChildPoints() {
        Long leafAId = 10L;
        Long leafBId = 11L;
        Long parentId = 2L;

        PointCategory leafA = new PointCategory();
        leafA.setId(leafAId);
        leafA.setParentCategoryId(parentId);
        leafA.setMaximum(10);
        leafA.setIsMaxChild(false);

        PointCategory leafB = new PointCategory();
        leafB.setId(leafBId);
        leafB.setParentCategoryId(parentId);
        leafB.setMaximum(15);
        leafB.setIsMaxChild(false);

        PointCategory parent = new PointCategory();
        parent.setId(parentId);
        parent.setParentCategoryId(null);
        parent.setMaximum(15);
        parent.setIsMaxChild(true); // Takes MAX of children

        // Leaf A already has 8 points
        StudentSemesterPoint existingLeafA = new StudentSemesterPoint(userId, semesterId, leafAId, 8);
        existingLeafA.setSystemPointRecord(8);
        existingLeafA.setManualPointRecord(0);

        when(pointCategoryRepository.findActiveEffectiveAsOf(semesterEndDate)).thenReturn(List.of(leafA, leafB, parent));
        when(studentSemesterPointRepository.findAllByUserIdsAndSemesterId(List.of(userId), semesterId))
                .thenReturn(new ArrayList<>(List.of(existingLeafA)));

        // Award 12 points to Leaf B via CHECKOUT_REWARD
        service.recordPointAndPropagate(userId, semesterId, leafBId, 12, PointTransactionTypeEnum.CHECKOUT_REWARD);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StudentSemesterPoint>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentSemesterPointRepository).saveAll(captor.capture());

        List<StudentSemesterPoint> saved = captor.getValue();
        assertEquals(2, saved.size()); // leafB and parent

        StudentSemesterPoint savedLeafB = saved.stream().filter(p -> p.getPointCategoryId().equals(leafBId)).findFirst().orElseThrow();
        assertEquals(12, savedLeafB.getSystemPointRecord());
        assertEquals(0, savedLeafB.getManualPointRecord());
        assertEquals(12, savedLeafB.getTotalPoint());

        StudentSemesterPoint savedParent = saved.stream().filter(p -> p.getPointCategoryId().equals(parentId)).findFirst().orElseThrow();
        // Since Leaf B (12) > Leaf A (8), parent takes max (Leaf B's 12 points)
        assertEquals(12, savedParent.getTotalPoint());
        assertEquals(12, savedParent.getSystemPointRecord());
        assertEquals(0, savedParent.getManualPointRecord());
    }

    @Test
    @DisplayName("Should batch process multiple students and propagate points up to ancestors")
    void bulkRecordPointsAndPropagate_shouldProcessMultipleStudentsAndMultipleCategories() {
        Long user1 = 101L;
        Long user2 = 102L;
        Long leafId = 10L;
        Long parentId = 2L;

        PointCategory leaf = new PointCategory();
        leaf.setId(leafId);
        leaf.setParentCategoryId(parentId);

        PointCategory parent = new PointCategory();
        parent.setId(parentId);
        parent.setParentCategoryId(null);
        parent.setIsMaxChild(false);

        when(pointCategoryRepository.findActiveEffectiveAsOf(semesterEndDate)).thenReturn(List.of(leaf, parent));
        when(studentSemesterPointRepository.findAllByUserIdsAndSemesterId(List.of(user1, user2), semesterId))
                .thenReturn(new ArrayList<>());

        service.bulkRecordPointsAndPropagate(List.of(user1, user2), semesterId, Map.of(leafId, 5), PointTransactionTypeEnum.CHECKOUT_REWARD);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StudentSemesterPoint>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentSemesterPointRepository).saveAll(captor.capture());

        List<StudentSemesterPoint> saved = captor.getValue();
        // 2 students * 2 categories (leaf + parent) = 4 records
        assertEquals(4, saved.size());

        List<StudentSemesterPoint> user1Points = saved.stream().filter(p -> p.getUserId().equals(user1)).toList();
        assertEquals(2, user1Points.size());
        assertTrue(user1Points.stream().allMatch(p -> p.getTotalPoint() == 5 && p.getSystemPointRecord() == 5));

        List<StudentSemesterPoint> user2Points = saved.stream().filter(p -> p.getUserId().equals(user2)).toList();
        assertEquals(2, user2Points.size());
        assertTrue(user2Points.stream().allMatch(p -> p.getTotalPoint() == 5 && p.getSystemPointRecord() == 5));
    }

    @Test
    @DisplayName("Should retain raw total on leaf when exceeding maximum, cap contribution to parent, and handle deduction")
    void recordPointAndPropagate_whenLeafExceedsMaximum_shouldNotOverflowParentAndSupportDeduction() {
        Long leafId = 1045L;
        Long parentId = 2L;

        PointCategory leaf = new PointCategory();
        leaf.setId(leafId);
        leaf.setParentCategoryId(parentId);
        leaf.setMaximum(2);
        leaf.setIsMaxChild(false);

        PointCategory parent = new PointCategory();
        parent.setId(parentId);
        parent.setParentCategoryId(null);
        parent.setMaximum(20);
        parent.setIsMaxChild(false);

        StudentSemesterPoint existingLeaf = new StudentSemesterPoint(userId, semesterId, leafId, 2);
        existingLeaf.setSystemPointRecord(2);
        existingLeaf.setManualPointRecord(0);

        StudentSemesterPoint existingParent = new StudentSemesterPoint(userId, semesterId, parentId, 2);
        existingParent.setSystemPointRecord(2);
        existingParent.setManualPointRecord(0);

        when(pointCategoryRepository.findActiveEffectiveAsOf(semesterEndDate)).thenReturn(List.of(leaf, parent));
        when(studentSemesterPointRepository.findAllByUserIdsAndSemesterId(List.of(userId), semesterId))
                .thenReturn(new ArrayList<>(List.of(existingLeaf, existingParent)));

        // Award another +2 on leaf (raw becomes 4, max is 2)
        service.recordPointAndPropagate(userId, semesterId, leafId, 2, PointTransactionTypeEnum.CHECKOUT_REWARD);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StudentSemesterPoint>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentSemesterPointRepository).saveAll(captor.capture());

        List<StudentSemesterPoint> saved = captor.getValue();
        StudentSemesterPoint savedLeaf = saved.stream().filter(p -> p.getPointCategoryId().equals(leafId)).findFirst().orElseThrow();
        assertEquals(4, savedLeaf.getTotalPoint());
        assertEquals(4, savedLeaf.getSystemPointRecord());

        StudentSemesterPoint savedParent = saved.stream().filter(p -> p.getPointCategoryId().equals(parentId)).findFirst().orElseThrow();
        assertEquals(2, savedParent.getTotalPoint());
        assertEquals(2, savedParent.getSystemPointRecord());

        // Now deduct -2 from leaf (raw becomes 2), parent remains 2
        service.recordPointAndPropagate(userId, semesterId, leafId, -2, PointTransactionTypeEnum.CHECKOUT_REWARD);
        assertEquals(2, savedLeaf.getTotalPoint());
        assertEquals(2, savedParent.getTotalPoint());
    }

    @Test
    @DisplayName("Should clamp sum rollup to parent maximum when children sum exceeds parent maximum")
    void recordPointAndPropagate_whenChildrenSumExceedsParentMax_shouldClampParentTotal() {
        Long leafAId = 10L;
        Long leafBId = 11L;
        Long parentId = 1L;

        PointCategory leafA = new PointCategory();
        leafA.setId(leafAId);
        leafA.setParentCategoryId(parentId);
        leafA.setMaximum(12);
        leafA.setIsMaxChild(false);

        PointCategory leafB = new PointCategory();
        leafB.setId(leafBId);
        leafB.setParentCategoryId(parentId);
        leafB.setMaximum(12);
        leafB.setIsMaxChild(false);

        PointCategory parent = new PointCategory();
        parent.setId(parentId);
        parent.setParentCategoryId(null);
        parent.setMaximum(20);
        parent.setIsMaxChild(false);

        StudentSemesterPoint existingLeafA = new StudentSemesterPoint(userId, semesterId, leafAId, 12);
        existingLeafA.setSystemPointRecord(12);
        existingLeafA.setManualPointRecord(0);

        when(pointCategoryRepository.findActiveEffectiveAsOf(semesterEndDate)).thenReturn(List.of(leafA, leafB, parent));
        when(studentSemesterPointRepository.findAllByUserIdsAndSemesterId(List.of(userId), semesterId))
                .thenReturn(new ArrayList<>(List.of(existingLeafA)));

        service.recordPointAndPropagate(userId, semesterId, leafBId, 10, PointTransactionTypeEnum.MANUAL);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StudentSemesterPoint>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentSemesterPointRepository).saveAll(captor.capture());

        StudentSemesterPoint savedParent = captor.getValue().stream()
                .filter(p -> p.getPointCategoryId().equals(parentId))
                .findFirst()
                .orElseThrow();
        assertEquals(20, savedParent.getTotalPoint());
        assertEquals(12, savedParent.getSystemPointRecord());
        assertEquals(10, savedParent.getManualPointRecord());
    }

    @Test
    @DisplayName("Should compare effective totals and break ties deterministically when isMaxChild is true")
    void recordPointAndPropagate_whenIsMaxChildTieOrOverflow_shouldCompareEffectiveAndBreakTies() {
        Long leafAId = 10L;
        Long leafBId = 11L;
        Long parentId = 2L;

        PointCategory leafA = new PointCategory();
        leafA.setId(leafAId);
        leafA.setParentCategoryId(parentId);
        leafA.setMaximum(5);
        leafA.setIsMaxChild(false);

        PointCategory leafB = new PointCategory();
        leafB.setId(leafBId);
        leafB.setParentCategoryId(parentId);
        leafB.setMaximum(8);
        leafB.setIsMaxChild(false);

        PointCategory parent = new PointCategory();
        parent.setId(parentId);
        parent.setParentCategoryId(null);
        parent.setMaximum(6);
        parent.setIsMaxChild(true);

        // Leaf A has raw 10 but maximum is 5 -> effective = 5
        StudentSemesterPoint existingLeafA = new StudentSemesterPoint(userId, semesterId, leafAId, 10);
        existingLeafA.setSystemPointRecord(10);
        existingLeafA.setManualPointRecord(0);

        StudentSemesterPoint existingLeafB = new StudentSemesterPoint(userId, semesterId, leafBId, 0);
        existingLeafB.setSystemPointRecord(0);
        existingLeafB.setManualPointRecord(0);

        StudentSemesterPoint existingParent = new StudentSemesterPoint(userId, semesterId, parentId, 5);
        existingParent.setSystemPointRecord(5);
        existingParent.setManualPointRecord(0);

        when(pointCategoryRepository.findActiveEffectiveAsOf(semesterEndDate)).thenReturn(List.of(leafA, leafB, parent));
        when(studentSemesterPointRepository.findAllByUserIdsAndSemesterId(List.of(userId), semesterId))
                .thenReturn(new ArrayList<>(List.of(existingLeafA, existingLeafB, existingParent)));

        // Leaf B gets 5 manual -> effective = 5 (tie on effectiveTotal with Leaf A, Leaf B wins on higher manualPointRecord)
        service.recordPointAndPropagate(userId, semesterId, leafBId, 5, PointTransactionTypeEnum.MANUAL);

        assertEquals(5, existingParent.getTotalPoint());
        assertEquals(0, existingParent.getSystemPointRecord());
        assertEquals(5, existingParent.getManualPointRecord());

        // Leaf B gets +3 manual (total 8, effective 8 > 5) -> clamped to parent.maximum (6)
        service.recordPointAndPropagate(userId, semesterId, leafBId, 3, PointTransactionTypeEnum.MANUAL);
        assertEquals(6, existingParent.getTotalPoint());
        assertEquals(0, existingParent.getSystemPointRecord());
        assertEquals(6, existingParent.getManualPointRecord());
    }
}
