package com.ddd.application.service.user.impl;

import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.application.dto.user.UserInfoDto;
import com.ddd.application.mapper.StaffProfileDtoMapper;
import com.ddd.application.mapper.StudentProfileDtoMapper;
import com.ddd.application.mapper.UserDtoMapper;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.infrastructure.entity.FanpageAdminProfileJpaEntity;
import com.ddd.infrastructure.entity.MajorJpaEntity;
import com.ddd.infrastructure.entity.RoleJpaEntity;
import com.ddd.infrastructure.entity.SchoolJpaEntity;
import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
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
import java.util.Set;

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

    @Test
    void getCurrentUserProfileByEmail_shouldReturnStudentAndStaffProfilesWithSchoolAndMajorIds() {
        RoleJpaEntity studentRole = new RoleJpaEntity();
        studentRole.setName("STUDENT");
        SchoolJpaEntity school = new SchoolJpaEntity();
        school.setId(2L);
        school.setName("Trường CNTT&TT");
        MajorJpaEntity major = new MajorJpaEntity();
        major.setId(20L);
        major.setName("Kỹ thuật phần mềm");
        major.setCode("7480103");
        major.setSchool(school);

        StudentProfileJpaEntity studentProfile = new StudentProfileJpaEntity();
        studentProfile.setStudentId("B2303827");
        studentProfile.setKNumber(49);
        studentProfile.setMajor(major);

        FanpageAdminProfileJpaEntity staffProfile = new FanpageAdminProfileJpaEntity();
        staffProfile.setStaffId("CB123");
        staffProfile.setOrgName("CLB Tin học");

        UserJpaEntity user = new UserJpaEntity();
        user.setId(9L);
        user.setEmail("loc@student.ctu.edu.vn");
        user.setFirstName("Lộc");
        user.setLastName("Đoàn");
        user.setGender("MALE");
        user.setIsActive(true);
        user.setRoles(Set.of(studentRole));
        user.setStudentProfile(studentProfile);
        user.setFanpageAdminProfile(staffProfile);

        when(userJpaRepository.findDetailedByEmail("loc@student.ctu.edu.vn")).thenReturn(Optional.of(user));

        UserInfoDto result = userQueryService.getCurrentUserProfileByEmail("loc@student.ctu.edu.vn");

        assertEquals(9L, result.userId());
        assertNotNull(result.studentProfile());
        assertEquals("B2303827", result.studentProfile().studentId());
        assertEquals(2L, result.studentProfile().schoolId());
        assertEquals(20L, result.studentProfile().majorId());
        assertNotNull(result.staffProfile());
        assertEquals("CB123", result.staffProfile().staffId());
    }

    @Test
    void getCurrentUserProfileByEmail_shouldSucceedWithout404_whenAdminHasNoSubProfiles() {
        RoleJpaEntity adminRole = new RoleJpaEntity();
        adminRole.setName("ADMIN");

        UserJpaEntity adminUser = new UserJpaEntity();
        adminUser.setId(1L);
        adminUser.setEmail("admin@ctu.edu.vn");
        adminUser.setFirstName("Quản");
        adminUser.setLastName("Trị");
        adminUser.setIsActive(true);
        adminUser.setRoles(Set.of(adminRole));

        when(userJpaRepository.findDetailedByEmail("admin@ctu.edu.vn")).thenReturn(Optional.of(adminUser));

        UserInfoDto result = userQueryService.getCurrentUserProfileByEmail("admin@ctu.edu.vn");

        assertEquals(1L, result.userId());
        assertTrue(result.roles().contains("ADMIN"));
        assertNull(result.studentProfile());
        assertNull(result.staffProfile());
    }
}
