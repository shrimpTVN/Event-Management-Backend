package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.dto.semesterpoint.PointTransactionRequestInfoDto;
import com.ddd.application.mapper.PointTransactionDtoMapper;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.model.*;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static com.ddd.application.service.semesterpoint.impl.StudentSemesterPointCalculatorTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointTransactionCommandServiceImplTest {
    @Mock PointTransactionDtoMapper mapper;
    @Mock PointTransactionRepository transactions;
    @Mock StudentSemesterPointRepository snapshots;
    @Mock SemesterPointCategoryPolicy policy;
    @Mock SecurityContextPort security;
    PointTransactionCommandServiceImpl service;
    final List<PointTransaction> ledger = new ArrayList<>();
    final Map<Long,StudentSemesterPoint> stored = new HashMap<>();
    @BeforeEach void setup() {
        var rebuild=new StudentSemesterPointCommandServiceImpl(snapshots,transactions,policy,new StudentSemesterPointCalculator());
        service=new PointTransactionCommandServiceImpl(mapper,transactions,policy,rebuild,security);
        when(security.getCurrentUserId()).thenReturn(7L);
        lenient().when(policy.effectiveCategories(9L)).thenReturn(List.of(category(1,null,20,false),category(2,1L,20,false)));
        lenient().when(transactions.findAllByUserIdsAndSemesterId(List.of(7L),9L)).thenAnswer(i->new ArrayList<>(ledger));
        lenient().when(snapshots.findAllByUserIdsAndSemesterId(List.of(7L),9L)).thenAnswer(i->new ArrayList<>(stored.values()));
        lenient().doAnswer(i->{for(StudentSemesterPoint p:i.<List<StudentSemesterPoint>>getArgument(0))stored.put(p.getPointCategoryId(),p);return null;}).when(snapshots).saveAll(anyList());
        var ids=new AtomicLong();
        lenient().doAnswer(i->{PointTransaction p=i.getArgument(0);if(p.getId()==null)p.setId(ids.incrementAndGet());ledger.removeIf(tx->tx.getId().equals(p.getId()));ledger.add(p);return null;}).when(transactions).save(any());
        lenient().when(transactions.findById(anyLong())).thenAnswer(i->ledger.stream().filter(tx->tx.getId().equals(i.getArgument(0))).findFirst());
        lenient().doAnswer(i->{PointTransaction p=i.getArgument(0);ledger.removeIf(tx->tx.getId().equals(p.getId()));return null;}).when(transactions).delete(any());
        lenient().when(mapper.toDomain(any(PointTransactionRequestInfoDto.class))).thenAnswer(i->{
            PointTransactionRequestInfoDto p=i.getArgument(0);var tx=transaction(p.pointCategoryId(),p.points(),"MANUAL");tx.setDescription(p.description());return tx;
        });
    }
    @Test void addEditAndPhysicallyDeleteUpdateSnapshotsWithoutAccumulationErrors() {
        service.createPointTransactionForStudent(new PointTransactionRequestInfoDto(9L,2L,null,8,"first"));
        service.createPointTransactionForStudent(new PointTransactionRequestInfoDto(9L,2L,null,5,"second"));
        assertEquals(13,stored.get(1L).getTotalPoint());assertEquals(2,ledger.size());
        service.updatePointTransactionForStudent(1L,5,"edited");
        assertEquals(10,stored.get(1L).getTotalPoint());assertEquals(2,ledger.size());assertEquals("edited",ledger.getLast().getDescription());
        service.deletePointTransactionForStudent(2L);
        assertEquals(1,ledger.size());assertEquals(5,stored.get(1L).getTotalPoint());assertEquals(1L,ledger.getFirst().getId());
    }
    @Test void rejectsOtherStudentsAndSystemTransactionsForEditAndDelete() {
        var other=transaction(2,8,"MANUAL");other.setUserId(8L);other.setId(1L);ledger.add(other);
        assertThrows(IllegalArgumentException.class,()->service.updatePointTransactionForStudent(1L,5,"edited"));
        assertThrows(IllegalArgumentException.class,()->service.deletePointTransactionForStudent(1L));
        other.setUserId(7L);other.setType("CHECKOUT_REWARD");
        assertThrows(IllegalArgumentException.class,()->service.updatePointTransactionForStudent(1L,5,"edited"));
        assertThrows(IllegalArgumentException.class,()->service.deletePointTransactionForStudent(1L));
        verify(transactions,never()).save(any());verify(transactions,never()).delete(any());
    }
    @Test void rejectsNonEffectiveCategoryBeforeSavingOrEditing() {
        doThrow(new BusinessException("outside effective tree")).when(policy).requireLeaves(9L,List.of(2L));
        assertThrows(BusinessException.class,()->service.createPointTransactionForStudent(new PointTransactionRequestInfoDto(9L,2L,null,5,"note")));
        var own=transaction(2,8,"MANUAL");own.setId(1L);ledger.add(own);
        assertThrows(BusinessException.class,()->service.updatePointTransactionForStudent(1L,5,"edited"));
        assertEquals(8,own.getPoints());verify(transactions,never()).save(any());
    }
}
