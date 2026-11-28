package com.ddd.application.service.registration.impl;

import com.ddd.application.mapper.RegistrationDtoMapper;
import com.ddd.application.service.event.cache.RecommendationCacheService;
import com.ddd.application.service.criteriaprogress.StudentCriteriaTransactionCommandService;
import com.ddd.application.service.semesterpoint.StudentSemesterPointCommandService;
import com.ddd.application.service.semesterpoint.impl.SemesterPointCategoryPolicy;
import com.ddd.domain.model.*;
import com.ddd.domain.enums.RegistrationStatusEnum;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.repository.*;
import com.ddd.domain.model.port.security.SecurityContextPort;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationPointAwardTest {
    @Mock RegistrationRepository registrations;
    @Mock UserRepository users;
    @Mock EventRepository events;
    @Mock FanpageMemberRepository members;
    @Mock EventPointRepository eventPoints;
    @Mock PointTransactionRepository transactions;
    @Mock StudentSemesterPointCommandService rebuild;
    @Mock StudentCriteriaTransactionCommandService criteria;
    @Mock SecurityContextPort security;
    @Mock ApplicationEventPublisher publisher;
    @Mock StudentProfileRepository profiles;
    @Mock RegistrationDtoMapper mapper;
    @Mock RecommendationCacheService cache;
    @Mock SemesterPointCategoryPolicy categoryPolicy;
    @InjectMocks RegistrationCommandServiceImpl service;
    @BeforeEach void setup() {
        var event=new Event();event.setId(1L);event.setFanpageId(20L);event.setSemesterId(9L);
        event.setStatus("PUBLISHED");event.setActive(true);event.setDateHappen(Instant.now().minusSeconds(60));
        when(security.getCurrentUserId()).thenReturn(99L);
        when(events.findByIdForUpdate(1L)).thenReturn(event);
        when(members.isActiveMember(20L,99L)).thenReturn(true);
        var registration=new Registration();registration.setUserId(7L);registration.setStatus(RegistrationStatusEnum.COMPLETED);
        when(registrations.findAllByEvent(1L)).thenReturn(List.of(registration));
        var award=new EventPoint();award.setPointCategoryId(2L);award.setPoint(3);
        when(eventPoints.findByEventId(1L)).thenReturn(List.of(award));
    }
    @Test void savesLedgerBeforeRebuildingAndDoesNotAwardTwice() {
        service.fanpageMemberMarkEventAsCompleted(1L);
        service.fanpageMemberMarkEventAsCompleted(1L);
        var order=inOrder(categoryPolicy,transactions,rebuild);
        order.verify(categoryPolicy).requireLeaves(9L,List.of(2L));
        order.verify(transactions).saveAll(argThat(list->list.size()==1 && list.getFirst().getUserId()==7L && list.getFirst().getPoints()==3));
        order.verify(rebuild).rebuildSemesterPoints(List.of(7L),9L);
        verify(transactions,times(1)).saveAll(anyList());
    }
    @Test void rejectsInvalidAwardCategoryBeforeSavingPoints() {
        doThrow(new BusinessException("parent or non-effective category")).when(categoryPolicy).requireLeaves(9L,List.of(2L));
        assertThrows(BusinessException.class,()->service.fanpageMemberMarkEventAsCompleted(1L));
        verify(transactions,never()).saveAll(anyList());verifyNoInteractions(rebuild);
    }
}
