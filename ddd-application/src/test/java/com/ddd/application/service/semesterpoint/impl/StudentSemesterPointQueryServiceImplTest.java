package com.ddd.application.service.semesterpoint.impl;

import com.ddd.domain.model.*;
import com.ddd.domain.repository.*;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.*;
import com.ddd.domain.exception.AuthException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static com.ddd.application.service.semesterpoint.impl.StudentSemesterPointCalculatorTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentSemesterPointQueryServiceImplTest {
    @Mock UserJpaRepository users;
    @Mock StudentSemesterPointRepository snapshots;
    @Mock PointTransactionRepository transactions;
    @Mock SemesterPointCategoryPolicy policy;
    @Spy StudentSemesterPointCalculator calculator=new StudentSemesterPointCalculator();
    @Mock SecurityContextPort security;
    @Mock RegistrationJpaRepository registrations;
    @InjectMocks StudentSemesterPointQueryServiceImpl service;
    @BeforeEach void setup() {
        lenient().when(security.getCurrentUserId()).thenReturn(7L);
        var user=new UserJpaEntity();user.setId(7L);
        lenient().when(users.findByEmail("s")).thenReturn(user);
        lenient().when(policy.effectiveCategories(9L)).thenReturn(List.of(category(1,null,20,false),category(2,1L,10,false)));
        lenient().when(transactions.findAllByUserIdsAndSemesterId(List.of(7L),9L)).thenReturn(List.of());
    }
    @Test void treeAndFlatReturnSameRawLeafAndServerMetadata() {
        when(transactions.findAllByUserIdsAndSemesterId(List.of(7L),9L)).thenReturn(List.of(transaction(2,8,"CHECKOUT_REWARD"),transaction(2,7,"MANUAL")));
        var tree=service.getSemesterPointTreeForCurrentStudent(9L);
        var leaf=tree.getFirst().items().getFirst();
        assertEquals(15,leaf.totalPoint());assertEquals(8,leaf.systemPointRecord());assertTrue(leaf.isAtMaximum());
        assertEquals(100,leaf.completionPercentage());assertEquals(10,tree.getFirst().totalPoint());
        var flat=service.getSemesterPointsForStudent("s",9L);
        assertEquals(15,flat.stream().filter(p->p.pointCategoryId()==2L).findFirst().orElseThrow().totalPoint());
        verify(snapshots,never()).saveAll(any());
    }
    @Test void newStudentAndEmptyConfigurationHaveSafeZeroTotals() {
        assertEquals(0,service.getSemesterPointTreeForCurrentStudent(9L).getFirst().totalPoint());
        when(policy.effectiveCategories(9L)).thenReturn(List.of());
        var result=service.getSummaryForCurrentStudent(9L);
        assertEquals(0,result.totalPoint());assertEquals("Kém",result.classificationLabel());assertEquals(35,result.pointsNeeded());
    }
    @ParameterizedTest
    @CsvSource({"34,Kém,1","35,Yếu,15","49,Yếu,1","50,Trung Bình,30","64,Trung Bình,16","65,Trung Bình,15","79,Trung Bình,1","80,Tốt,10","89,Tốt,1","90,Xuất sắc,0","120,Xuất sắc,0"})
    void summaryUsesBackendRankBoundaries(int score,String label,int needed) {
        when(policy.effectiveCategories(9L)).thenReturn(List.of(category(1,null,200,false)));
        when(transactions.findAllByUserIdsAndSemesterId(List.of(7L),9L)).thenReturn(List.of(transaction(1,score,"MANUAL")));
        when(registrations.countByUserIdAndEventSemesterIdAndStatusIn(eq(7L),eq(9L),any())).thenReturn(3L);
        var result=service.getSummaryForCurrentStudent(9L);
        assertEquals(score,result.totalPoint());assertEquals(100,result.maximum());assertEquals(label,result.classificationLabel());
        assertEquals(needed,result.pointsNeeded());assertEquals(3,result.activitiesCount());
    }
    @Test void adminMaximumChangesAreVisibleOnNextReadEvenForExistingSnapshots() {
        var leaf=category(2,1L,10,false);var root=category(1,null,20,false);
        when(policy.effectiveCategories(9L)).thenReturn(List.of(root,leaf));
        when(transactions.findAllByUserIdsAndSemesterId(List.of(7L),9L)).thenReturn(List.of(transaction(2,10,"MANUAL")));
        assertEquals(10,service.getSemesterPointTreeForCurrentStudent(9L).getFirst().totalPoint());
        leaf.setMaximum(5);
        assertEquals(5,service.getSemesterPointTreeForCurrentStudent(9L).getFirst().totalPoint());
    }
    @Test void rejectsUnauthenticatedRead() {
        when(security.getCurrentUserId()).thenReturn(null);
        assertThrows(AuthException.class,()->service.getSemesterPointTreeForCurrentStudent(9L));
    }
    @Test void summaryCountUsesSelectedSemesterAndExactActivityStatuses() {
        service.getSummaryForCurrentStudent(9L);
        verify(registrations).countByUserIdAndEventSemesterIdAndStatusIn(7L,9L,
            List.of(com.ddd.domain.enums.RegistrationStatusEnum.COMPLETED,com.ddd.domain.enums.RegistrationStatusEnum.CONFIRMED,com.ddd.domain.enums.RegistrationStatusEnum.RECORDED));
        var parsed=new org.springframework.data.repository.query.parser.PartTree("countByUserIdAndEventSemesterIdAndStatusIn",com.ddd.infrastructure.entity.RegistrationJpaEntity.class);
        assertEquals(List.of("user.id","event.semester.id","status"),parsed.stream().flatMap(or->or.stream()).map(part->part.getProperty().toDotPath()).toList());
    }
}
