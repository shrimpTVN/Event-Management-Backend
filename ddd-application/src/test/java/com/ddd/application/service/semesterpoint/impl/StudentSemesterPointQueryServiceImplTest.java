package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.dto.semesterpoint.StudentSemesterPointFlattenedItemDto;
import com.ddd.application.dto.semesterpoint.StudentSemesterPointGroupedTreeDto;
import com.ddd.application.mapper.StudentSemesterPointDtoMapper;
import com.ddd.domain.exception.AuthException;
import com.ddd.domain.model.PointCategory;
import com.ddd.domain.model.StudentSemesterPoint;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.PointCategoryRepository;
import com.ddd.domain.repository.StudentSemesterPointRepository;
import com.ddd.infrastructure.repository.jpaRepository.StudentSemesterPointJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentSemesterPointQueryServiceImplTest {

    @Mock
    private StudentSemesterPointJpaRepository studentSemesterPointJpaRepository;

    @Mock
    private StudentSemesterPointDtoMapper studentSemesterPointDtoMapper;

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private StudentSemesterPointRepository studentSemesterPointRepository;

    @Mock
    private PointCategoryRepository pointCategoryRepository;

    @Mock
    private SecurityContextPort securityContextPort;

    @InjectMocks
    private StudentSemesterPointQueryServiceImpl queryService;

    private final Long userId = 100L;
    private final Long semesterId = 2024L;

    @Test
    @DisplayName("Should return grouped tree with attached student points and fallback to 0 for unrecorded categories")
    void getSemesterPointTreeForCurrentStudent_shouldReturnGroupedTreeWithAttachedPoints() {
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);

        PointCategory root = new PointCategory();
        root.setId(1L);
        root.setName("I. Học tập");
        root.setMaximum(20);
        root.setLevel(1);
        root.setParentCategoryId(0L);
        root.setChildrenOrder(1);
        root.setIsMaxChild(false);
        root.setActive(true);
        root.setDateApply(LocalDate.of(2024, 1, 1));

        PointCategory child1 = new PointCategory();
        child1.setId(10L);
        child1.setName("Đi học đúng giờ");
        child1.setMaximum(10);
        child1.setLevel(2);
        child1.setParentCategoryId(1L);
        child1.setChildrenOrder(1);
        child1.setIsMaxChild(false);
        child1.setActive(true);

        PointCategory child2 = new PointCategory();
        child2.setId(11L);
        child2.setName("Tham gia NCKH");
        child2.setMaximum(15);
        child2.setLevel(2);
        child2.setParentCategoryId(1L);
        child2.setChildrenOrder(2);
        child2.setIsMaxChild(false);
        child2.setActive(true);

        when(pointCategoryRepository.findAllActive()).thenReturn(List.of(root, child1, child2));

        StudentSemesterPoint rootPoint = new StudentSemesterPoint();
        rootPoint.setId(501L);
        rootPoint.setPointCategoryId(1L);
        rootPoint.setUserId(userId);
        rootPoint.setSemesterId(semesterId);
        rootPoint.setTotalPoint(12);
        rootPoint.setSystemPointRecord(8);
        rootPoint.setManualPointRecord(4);

        StudentSemesterPoint child1Point = new StudentSemesterPoint();
        child1Point.setId(502L);
        child1Point.setPointCategoryId(10L);
        child1Point.setUserId(userId);
        child1Point.setSemesterId(semesterId);
        child1Point.setTotalPoint(8);
        child1Point.setSystemPointRecord(8);
        child1Point.setManualPointRecord(0);

        when(studentSemesterPointRepository.findAllByUserIdAndSemesterId(userId, semesterId))
                .thenReturn(List.of(rootPoint, child1Point));

        List<StudentSemesterPointGroupedTreeDto> tree = queryService.getSemesterPointTreeForCurrentStudent(semesterId);

        assertNotNull(tree);
        assertEquals(1, tree.size());

        StudentSemesterPointGroupedTreeDto rootDto = tree.get(0);
        assertEquals(1L, rootDto.id());
        assertEquals("I. Học tập", rootDto.name());
        assertEquals(20, rootDto.maximum());
        assertEquals(501L, rootDto.studentSemesterPointId());
        assertEquals(12, rootDto.totalPoint());
        assertEquals(8, rootDto.systemPointRecord());
        assertEquals(4, rootDto.manualPointRecord());

        assertEquals(2, rootDto.items().size());

        StudentSemesterPointFlattenedItemDto item1 = rootDto.items().get(0);
        assertEquals(10L, item1.id());
        assertEquals(10, item1.maximum());
        assertEquals(502L, item1.studentSemesterPointId());
        assertEquals(8, item1.totalPoint());
        assertEquals(8, item1.systemPointRecord());
        assertEquals(0, item1.manualPointRecord());
        assertTrue(item1.isLeaf());

        StudentSemesterPointFlattenedItemDto item2 = rootDto.items().get(1);
        assertEquals(11L, item2.id());
        assertEquals(15, item2.maximum());
        assertNull(item2.studentSemesterPointId());
        assertEquals(0, item2.totalPoint());
        assertEquals(0, item2.systemPointRecord());
        assertEquals(0, item2.manualPointRecord());
        assertTrue(item2.isLeaf());
    }

    @Test
    @DisplayName("Should throw AuthException when current user is not authenticated")
    void getSemesterPointTreeForCurrentStudent_shouldThrowAuthException_whenNotAuthenticated() {
        when(securityContextPort.getCurrentUserId()).thenReturn(null);

        assertThrows(AuthException.class, () ->
                queryService.getSemesterPointTreeForCurrentStudent(semesterId)
        );
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when parameters are null")
    void getSemesterPointTreeForStudent_shouldThrowIllegalArgumentException_whenParamsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                queryService.getSemesterPointTreeForStudent(null, semesterId)
        );
        assertThrows(IllegalArgumentException.class, () ->
                queryService.getSemesterPointTreeForStudent(userId, null)
        );
    }
}
