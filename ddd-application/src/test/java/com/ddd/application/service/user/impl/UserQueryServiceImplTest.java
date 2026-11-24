package com.ddd.application.service.user.impl;

import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.application.mapper.StaffProfileDtoMapper;
import com.ddd.application.mapper.StudentProfileDtoMapper;
import com.ddd.application.mapper.UserDtoMapper;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.infrastructure.entity.FanpageAdminProfileJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.FanpageAdminProfileJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.StudentProfileJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceImplTest {

    @Mock
    private StudentProfileJpaRepository studentProfileJpaRepository;

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private FanpageAdminProfileJpaRepository fanpageAdminProfileJpaRepository;

    @Mock
    private StudentProfileDtoMapper studentProfileDtoMapper;

    @Mock
    private UserDtoMapper userDtoMapper;

    @Mock
    private SecurityContextPort securityContextPort;

    private final StaffProfileDtoMapper staffProfileDtoMapper = Mappers.getMapper(StaffProfileDtoMapper.class);

    private UserQueryServiceImpl userQueryService;

    @BeforeEach
    void setUp() {
        userQueryService = new UserQueryServiceImpl(
                studentProfileJpaRepository,
                userJpaRepository,
                fanpageAdminProfileJpaRepository,
                studentProfileDtoMapper,
                staffProfileDtoMapper,
                userDtoMapper,
                securityContextPort
        );
    }

    @Test
    void getStaffProfile_shouldReturnProfile_whenAuthenticatedAndHasStaffProfile() {
        when(securityContextPort.getCurrentUserId()).thenReturn(1L);

        UserJpaEntity user = new UserJpaEntity();
        user.setId(1L);
        user.setEmail("admin@example.com");
        when(userJpaRepository.findById(1L)).thenReturn(Optional.of(user));

        FanpageAdminProfileJpaEntity staffProfile = new FanpageAdminProfileJpaEntity();
        staffProfile.setStaffId("STAFF01");
        staffProfile.setTitle("Admin");
        when(fanpageAdminProfileJpaRepository.findByUserId(1L)).thenReturn(Optional.of(staffProfile));

        StaffProfileSummaryDto result = userQueryService.getStaffProfile();

        assertNotNull(result);
        assertEquals(1L, result.userId());
        assertNotNull(result.staffProfile());
        assertEquals("STAFF01", result.staffProfile().staffId());
    }

    @Test
    void getStaffProfile_shouldReturnNullStaffProfile_whenUserHasNoStaffProfile() {
        when(securityContextPort.getCurrentUserId()).thenReturn(2L);

        UserJpaEntity user = new UserJpaEntity();
        user.setId(2L);
        user.setEmail("user@example.com");
        when(userJpaRepository.findById(2L)).thenReturn(Optional.of(user));
        when(fanpageAdminProfileJpaRepository.findByUserId(2L)).thenReturn(Optional.empty());

        StaffProfileSummaryDto result = userQueryService.getStaffProfile();

        assertNotNull(result);
        assertEquals(2L, result.userId());
        assertNull(result.staffProfile());
    }

    @Test
    void getStaffProfile_shouldThrowUnauthorized_whenNotAuthenticated() {
        when(securityContextPort.getCurrentUserId()).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> userQueryService.getStaffProfile());
    }
}
