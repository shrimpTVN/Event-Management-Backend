package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.fanpage.FanpageInfoDto;
import com.ddd.application.dto.fanpage.FanpageMemberDto;
import com.ddd.application.dto.fanpage.FanpageSearchCriteriaDto;
import com.ddd.application.dto.fanpage.ManagedFanpageInfoDto;
import com.ddd.application.dto.fanpage.UpcomingEventReminderDto;
import com.ddd.application.mapper.FanpageDtoMapper;
import com.ddd.application.mapper.FanpageMemberDtoMapper;
import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.domain.enums.FanpageRoleEnum;
import com.ddd.domain.enums.FollowingStatusEnum;
import com.ddd.domain.enums.RegistrationStatusEnum;
import com.ddd.domain.exception.AuthException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Fanpage;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.FanpageRepository;
import com.ddd.domain.repository.FollowingRepository;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.entity.FanpageAdminProfileJpaEntity;
import com.ddd.infrastructure.entity.FanpageJpaEntity;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntityId;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.EventJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.FanpageJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.FanpageMemberJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.FollowingJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.RegistrationJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FanpageQueryServiceImplTest {

    @Mock private FanpageMemberJpaRepository fanpageMemberJpaRepository;
    @Mock private FanpageMemberDtoMapper fanpageMemberDtoMapper;
    @Mock private FanpageJpaRepository fanpageJpaRepository;
    @Mock private FanpageDtoMapper fanpageDtoMapper;
    @Mock private FanpageRepository fanpageRepository;
    @Mock private FanpageMemberRepository fanpageMemberRepository;
    @Mock private FollowingRepository followingRepository;
    @Mock private FollowingJpaRepository followingJpaRepository;
    @Mock private SecurityContextPort securityContextPort;
    @Mock private EventJpaRepository eventJpaRepository;
    @Mock private RegistrationJpaRepository registrationJpaRepository;

    @InjectMocks private FanpageQueryServiceImpl fanpageQueryService;

    @Test
    void getMembersOfFanpage_whenAdmin_shouldFetchEvenIfFanpageInactive() {
        when(securityContextPort.hasRole("ADMIN")).thenReturn(true);
        Fanpage fanpage = new Fanpage();
        fanpage.setId(99L);
        fanpage.setActive(false);
        when(fanpageRepository.findById(99L)).thenReturn(Optional.of(fanpage));

        FanpageMemberJpaEntity memberEntity = new FanpageMemberJpaEntity();
        when(fanpageMemberJpaRepository.findAllByFanpageId(99L)).thenReturn(List.of(memberEntity));
        FanpageMemberDto memberDto = new FanpageMemberDto(1L, "admin@test.com", "Admin", "User", null, "ADMIN", "ST01", "0901234567");
        when(fanpageMemberDtoMapper.toDto(memberEntity)).thenReturn(memberDto);

        List<FanpageMemberDto> result = fanpageQueryService.getMembersOfFanpage(99L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ST01", result.get(0).staffId());
        assertEquals("0901234567", result.get(0).phoneNumber());
        verify(fanpageRepository, never()).findByIdAndIsActive(anyLong(), anyBoolean());
    }

    @Test
    void getUpcomingReminder_whenMemberHasUpcomingEvent_returnsReminderWithActiveCount() {
        when(securityContextPort.getCurrentUserId()).thenReturn(10L);
        when(securityContextPort.hasRole("ADMIN")).thenReturn(false);

        Fanpage fanpage = new Fanpage();
        fanpage.setId(5L);
        fanpage.setActive(true);
        when(fanpageRepository.findByIdAndIsActive(5L, true)).thenReturn(Optional.of(fanpage));
        when(fanpageMemberRepository.isActiveMember(5L, 10L)).thenReturn(true);

        Instant happenAt = Instant.now().plus(2, ChronoUnit.DAYS);
        EventJpaEntity event = new EventJpaEntity();
        event.setId(42L);
        event.setName("Tech Workshop");
        event.setDateHappen(happenAt);

        when(eventJpaRepository.findFirstByFanpage_IdAndIsActiveTrueAndStatusAndDateHappenAfterOrderByDateHappenAsc(
                eq(5L), eq(EventStatusEnum.PUBLISHED), any(Instant.class)))
                .thenReturn(Optional.of(event));
        when(registrationJpaRepository.countActiveRegistrationsByEventId(42L, RegistrationStatusEnum.CANCELLED))
                .thenReturn(120L);

        UpcomingEventReminderDto result = fanpageQueryService.getUpcomingReminder(5L);

        assertNotNull(result);
        assertEquals(42L, result.id());
        assertEquals("Tech Workshop", result.title());
        assertEquals(happenAt, result.dateHappen());
        assertEquals(120L, result.registrationsCount());
    }

    @Test
    void getUpcomingReminder_whenNoUpcomingEvent_returnsNull() {
        when(securityContextPort.getCurrentUserId()).thenReturn(10L);
        when(securityContextPort.hasRole("ADMIN")).thenReturn(false);

        Fanpage fanpage = new Fanpage();
        fanpage.setId(5L);
        when(fanpageRepository.findByIdAndIsActive(5L, true)).thenReturn(Optional.of(fanpage));
        when(fanpageMemberRepository.isActiveMember(5L, 10L)).thenReturn(true);
        when(eventJpaRepository.findFirstByFanpage_IdAndIsActiveTrueAndStatusAndDateHappenAfterOrderByDateHappenAsc(
                eq(5L), eq(EventStatusEnum.PUBLISHED), any(Instant.class)))
                .thenReturn(Optional.empty());

        UpcomingEventReminderDto result = fanpageQueryService.getUpcomingReminder(5L);

        assertNull(result);
        verify(registrationJpaRepository, never()).countActiveRegistrationsByEventId(anyLong(), any());
    }

    @Test
    void getUpcomingReminder_whenNotMemberAndNotAdmin_throwsAuthException() {
        when(securityContextPort.getCurrentUserId()).thenReturn(10L);
        when(securityContextPort.hasRole("ADMIN")).thenReturn(false);

        Fanpage fanpage = new Fanpage();
        fanpage.setId(5L);
        when(fanpageRepository.findByIdAndIsActive(5L, true)).thenReturn(Optional.of(fanpage));
        when(fanpageMemberRepository.isActiveMember(5L, 10L)).thenReturn(false);

        assertThrows(AuthException.class, () -> fanpageQueryService.getUpcomingReminder(5L));
    }

    @Test
    void getUpcomingReminder_whenFanpageMissing_throwsResourceNotFound() {
        when(securityContextPort.getCurrentUserId()).thenReturn(10L);
        when(securityContextPort.hasRole("ADMIN")).thenReturn(false);
        when(fanpageRepository.findByIdAndIsActive(999L, true)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> fanpageQueryService.getUpcomingReminder(999L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchAndFilterFanpagesForAdmin_batchesFollowerEventsAndRepresentativeCounts() {
        FanpageJpaEntity fp1 = new FanpageJpaEntity();
        fp1.setId(1L);
        fp1.setName("CLB AI");
        FanpageJpaEntity fp2 = new FanpageJpaEntity();
        fp2.setId(2L);
        fp2.setName("CLB An ninh");

        when(fanpageJpaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fp1, fp2)));
        when(fanpageDtoMapper.toInfoDto(fp1)).thenReturn(new FanpageInfoDto(1L, "CLB AI", null, null, null, null, null, null, null, null, null, "ACTIVE", true));
        when(fanpageDtoMapper.toInfoDto(fp2)).thenReturn(new FanpageInfoDto(2L, "CLB An ninh", null, null, null, null, null, null, null, null, null, "ACTIVE", true));

        when(followingJpaRepository.countFollowersByFanpageIds(List.of(1L, 2L), FollowingStatusEnum.FOLLOWING))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 120L}));
        when(eventJpaRepository.countActiveEventsByFanpageIds(List.of(1L, 2L)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 8L}, new Object[]{2L, 3L}));

        UserJpaEntity adminUser = new UserJpaEntity();
        adminUser.setId(401L);
        adminUser.setFirstName("Minh");
        adminUser.setLastName("Nguyễn");
        adminUser.setEmail("minh@ctu.edu.vn");
        FanpageAdminProfileJpaEntity staffProfile = new FanpageAdminProfileJpaEntity();
        staffProfile.setStaffId("CB401");
        staffProfile.setPhoneNumber("0909999999");
        adminUser.setFanpageAdminProfile(staffProfile);

        FanpageMemberJpaEntity adminMember = new FanpageMemberJpaEntity();
        adminMember.setId(new FanpageMemberJpaEntityId(1L, 401L));
        adminMember.setUser(adminUser);
        when(fanpageMemberJpaRepository.findActiveMembersByFanpageIdsAndRole(List.of(1L, 2L), FanpageRoleEnum.ADMIN.name()))
                .thenReturn(List.of(adminMember));

        Page<FanpageInfoDto> result = fanpageQueryService.searchAndFilterFanpagesForAdmin(
                new FanpageSearchCriteriaDto(null, null, null, null, 0, 20, "createdAt", "desc")
        );

        assertEquals(2, result.getContent().size());
        FanpageInfoDto first = result.getContent().get(0);
        assertEquals(120L, first.followerCount());
        assertEquals(8L, first.eventsCount());
        assertEquals(401L, first.representativeUserId());
        assertEquals("Minh Nguyễn", first.representativeName());
        assertEquals("CB401", first.representativeCode());
        assertEquals("0909999999", first.representativePhone());

        FanpageInfoDto second = result.getContent().get(1);
        assertEquals(0L, second.followerCount());
        assertEquals(3L, second.eventsCount());
        assertNull(second.representativeName());

        verify(followingJpaRepository, times(1)).countFollowersByFanpageIds(any(), any());
        verify(eventJpaRepository, times(1)).countActiveEventsByFanpageIds(any());
        verify(fanpageMemberJpaRepository, times(1)).findActiveMembersByFanpageIdsAndRole(any(), any());
    }

    @Test
    void getManagedFanpagesByUserId_returnsActiveMembershipsInSingleQuery() {
        FanpageMemberJpaEntity membership = new FanpageMemberJpaEntity();
        ManagedFanpageInfoDto mapped = new ManagedFanpageInfoDto(11L, "Đoàn Khoa", null, null, "Đoàn Khoa", "ADMIN");
        when(fanpageMemberJpaRepository.findAllByUserIdAndIsActive(77L, true)).thenReturn(List.of(membership));
        when(fanpageDtoMapper.toManagedInfoDto(membership)).thenReturn(mapped);

        List<ManagedFanpageInfoDto> result = fanpageQueryService.getManagedFanpagesByUserId(77L);

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).fanpageId());
    }
}
