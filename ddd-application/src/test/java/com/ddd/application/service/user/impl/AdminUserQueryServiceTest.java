package com.ddd.application.service.user.impl;

import com.ddd.application.dto.user.StaffProfileDetailsDto;
import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.application.dto.user.StudentProfileDetailsDto;
import com.ddd.application.dto.user.UserInfoDto;
import com.ddd.application.mapper.StaffProfileDtoMapper;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.infrastructure.entity.*;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserQueryServiceTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private StaffProfileDtoMapper staffProfileDtoMapper;

    @InjectMocks
    private AdminUserQueryServiceImpl adminUserQueryService;

    @Test
    @DisplayName("getAllUsers should return empty page when no users found without issuing roles query")
    void getAllUsers_whenEmpty_shouldReturnEmptyPage() {
        Page<UserJpaEntity> emptyPage = Page.empty();
        when(userJpaRepository.findAllAdminUsersByIsActive(eq(true), any(Pageable.class))).thenReturn(emptyPage);

        Page<UserInfoDto> result = adminUserQueryService.getAllUsers(true, 0, 10, "createdAt", "desc");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userJpaRepository, never()).findUsersWithRolesByIdIn(anyCollection());
    }

    @Test
    @DisplayName("getAllUsers should map user, student profile, staff profile, and batch-fetched roles")
    void getAllUsers_withProfilesAndRoles_shouldMapCorrectly() {
        UserJpaEntity user = new UserJpaEntity();
        user.setId(10L);
        user.setEmail("admin@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setGender("MALE");
        user.setIsActive(true);

        SchoolJpaEntity school = new SchoolJpaEntity();
        school.setName("School of CS");
        MajorJpaEntity major = new MajorJpaEntity();
        major.setName("Software Engineering");
        major.setCode("SE01");
        major.setSchool(school);

        StudentProfileJpaEntity studentProfile = new StudentProfileJpaEntity();
        studentProfile.setStudentId("B2100001");
        studentProfile.setKNumber(47);
        studentProfile.setMajor(major);
        user.setStudentProfile(studentProfile);

        FanpageAdminProfileJpaEntity staffProfile = new FanpageAdminProfileJpaEntity();
        staffProfile.setStaffId("CB001");
        user.setFanpageAdminProfile(staffProfile);

        Page<UserJpaEntity> page = new PageImpl<>(List.of(user));
        when(userJpaRepository.findAllAdminUsersByIsActive(eq(true), any(Pageable.class))).thenReturn(page);

        RoleJpaEntity role = new RoleJpaEntity();
        role.setName("ROLE_ADMIN");
        UserJpaEntity userWithRoles = new UserJpaEntity();
        userWithRoles.setId(10L);
        userWithRoles.setRoles(Set.of(role));
        when(userJpaRepository.findUsersWithRolesByIdIn(List.of(10L))).thenReturn(List.of(userWithRoles));

        StaffProfileDetailsDto staffDetails = new StaffProfileDetailsDto("CB001", "School of CS", "Lecturer", LocalDate.of(1985, 5, 20), "0912345678");
        when(staffProfileDtoMapper.toStaffProfileDetailsDto(staffProfile)).thenReturn(staffDetails);

        Page<UserInfoDto> result = adminUserQueryService.getAllUsers(true, 0, 10, "createdAt", "desc");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        UserInfoDto dto = result.getContent().get(0);
        assertEquals(10L, dto.userId());
        assertEquals("admin@example.com", dto.email());
        assertTrue(dto.roles().contains("ROLE_ADMIN"));
        assertEquals("ROLE_ADMIN", dto.role());

        assertNotNull(dto.studentProfile());
        assertEquals("B2100001", dto.studentProfile().studentId());
        assertEquals(47, dto.studentProfile().kNumber());
        assertEquals("School of CS", dto.studentProfile().schoolName());
        assertEquals("Software Engineering", dto.studentProfile().majorName());
        assertEquals("SE01", dto.studentProfile().majorCode());

        assertNotNull(dto.staffProfile());
        assertEquals("CB001", dto.staffProfile().staffId());
    }

    @Test
    @DisplayName("getStaffProfileByUserId should return profile summary when user exists")
    void getStaffProfileByUserId_whenUserExists_shouldReturnSummary() {
        UserJpaEntity user = new UserJpaEntity();
        user.setId(10L);
        FanpageAdminProfileJpaEntity staffProfile = new FanpageAdminProfileJpaEntity();
        user.setFanpageAdminProfile(staffProfile);

        StaffProfileDetailsDto staffDto = new StaffProfileDetailsDto("CB001", "School of CS", "Lecturer", LocalDate.of(1985, 5, 20), "0912345678");
        StaffProfileSummaryDto summary = new StaffProfileSummaryDto(10L, "John", "Doe", "test@example.com", "MALE", null, Set.of("FANPAGE_ADMIN"), staffDto);

        when(userJpaRepository.findAdminProfileById(10L)).thenReturn(Optional.of(user));
        when(staffProfileDtoMapper.toStaffProfileSummaryDto(user, staffProfile)).thenReturn(summary);

        StaffProfileSummaryDto result = adminUserQueryService.getStaffProfileByUserId(10L);

        assertNotNull(result);
        assertEquals(10L, result.userId());
        assertEquals("CB001", result.staffProfile().staffId());
    }

    @Test
    @DisplayName("getStaffProfileByUserId should throw ResourceNotFoundException when user not found")
    void getStaffProfileByUserId_whenUserNotFound_shouldThrow404() {
        when(userJpaRepository.findAdminProfileById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminUserQueryService.getStaffProfileByUserId(99L));
    }

    @Test
    @DisplayName("getAllUsers with role/keyword or null isActive should query via Specification findAdminUsers")
    void getAllUsers_withRoleAndKeyword_shouldUseSpecificationQuery() {
        UserJpaEntity user = new UserJpaEntity();
        user.setId(21L);
        user.setEmail("sv@student.ctu.edu.vn");
        user.setIsActive(true);
        Page<UserJpaEntity> page = new PageImpl<>(List.of(user));

        when(userJpaRepository.findAdminUsers(any(), any(Pageable.class))).thenReturn(page);
        when(userJpaRepository.findUsersWithRolesByIdIn(List.of(21L))).thenReturn(List.of(user));

        Page<UserInfoDto> result = adminUserQueryService.getAllUsers(null, "STUDENT", "B23", 0, 20, "createdAt", "desc");

        assertEquals(1, result.getTotalElements());
        assertEquals(21L, result.getContent().get(0).userId());
        verify(userJpaRepository).findAdminUsers(any(), any(Pageable.class));
        verify(userJpaRepository, never()).findAllAdminUsersByIsActive(anyBoolean(), any(Pageable.class));
    }
}
