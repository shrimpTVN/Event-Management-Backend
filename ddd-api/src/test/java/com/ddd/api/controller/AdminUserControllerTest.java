package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.user.res.StaffProfileDetailsResponseDto;
import com.ddd.api.dto.user.res.StaffProfileResponseDto;
import com.ddd.api.dto.user.res.StudentProfileDetailsResponseDto;
import com.ddd.api.dto.user.res.UserInfoResponseDto;
import com.ddd.api.mapper.StaffProfileApiMapper;
import com.ddd.api.mapper.UserInfoApiMapper;
import com.ddd.application.dto.user.StaffProfileDetailsDto;
import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.application.dto.user.StudentProfileDetailsDto;
import com.ddd.application.dto.user.UserInfoDto;
import com.ddd.application.service.user.AdminUserQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserQueryService adminUserQueryService;

    @Mock
    private UserInfoApiMapper userInfoApiMapper;

    @Mock
    private StaffProfileApiMapper staffProfileApiMapper;

    @Mock
    private com.ddd.application.service.fanpage.FanpageRegistrationEligibilityService fanpageRegistrationEligibilityService;

    @InjectMocks
    private UserController userController;

    @Test
    @DisplayName("getAllUsers should return paginated UserInfoResponseDto with student and staff profiles")
    void getAllUsers_shouldReturnAdminUsersWithProfiles() {
        StudentProfileDetailsDto studentDto = new StudentProfileDetailsDto("B2100001", 47, "School of CS", "Software Engineering", "SE01", null);
        StaffProfileDetailsDto staffDto = new StaffProfileDetailsDto("CB001", "School of CS", "Lecturer", LocalDate.of(1985, 5, 20), "0912345678");
        UserInfoDto summary = new UserInfoDto(
                10L, "test@example.com", Set.of("STUDENT", "FANPAGE_ADMIN"), "STUDENT",
                "John", "Doe", "MALE", true, null, null, studentDto, staffDto
        );
        Page<UserInfoDto> page = new PageImpl<>(List.of(summary));

        StudentProfileDetailsResponseDto studentRes = new StudentProfileDetailsResponseDto("B2100001", 47, "School of CS", "Software Engineering", "SE01", null);
        StaffProfileDetailsResponseDto staffRes = new StaffProfileDetailsResponseDto("CB001", "School of CS", "Lecturer", LocalDate.of(1985, 5, 20), "0912345678");
        UserInfoResponseDto res = new UserInfoResponseDto(
                10L, "test@example.com", Set.of("STUDENT", "FANPAGE_ADMIN"), "STUDENT",
                "John", "Doe", "MALE", true, null, null, studentRes, staffRes
        );

        when(adminUserQueryService.getAllUsers(true, 0, 10, "createdAt", "desc")).thenReturn(page);
        when(userInfoApiMapper.toUserInfoResponseDto(summary)).thenReturn(res);

        BaseResponse<Page<UserInfoResponseDto>> response = userController.getAllUsers(true, 0, 10, "createdAt", "desc");

        assertNotNull(response);
        assertNotNull(response.data());
        assertEquals(1, response.data().getTotalElements());
        UserInfoResponseDto item = response.data().getContent().get(0);
        assertEquals(10L, item.userId());
        assertNotNull(item.studentProfile());
        assertEquals("B2100001", item.studentProfile().studentId());
        assertNotNull(item.staffProfile());
        assertEquals("CB001", item.staffProfile().staffId());
        assertEquals("CB001", item.fanpageAdminProfile().staffId());
    }

    @Test
    @DisplayName("getAdminStaffProfileByUserId should return staff profile for user id")
    void getAdminStaffProfileByUserId_shouldReturnStaffProfile() {
        StaffProfileDetailsDto staffDto = new StaffProfileDetailsDto("CB001", "School of CS", "Lecturer", LocalDate.of(1985, 5, 20), "0912345678");
        StaffProfileSummaryDto summary = new StaffProfileSummaryDto(
                10L, "John", "Doe", "test@example.com", "MALE", null, Set.of("FANPAGE_ADMIN"), staffDto
        );

        StaffProfileDetailsResponseDto staffRes = new StaffProfileDetailsResponseDto("CB001", "School of CS", "Lecturer", LocalDate.of(1985, 5, 20), "0912345678");
        StaffProfileResponseDto res = new StaffProfileResponseDto(
                10L, "John", "Doe", "test@example.com", "MALE", null, Set.of("FANPAGE_ADMIN"), staffRes
        );

        when(adminUserQueryService.getStaffProfileByUserId(10L)).thenReturn(summary);
        when(staffProfileApiMapper.toStaffProfileResponseDto(summary)).thenReturn(res);

        BaseResponse<StaffProfileResponseDto> response = userController.getAdminStaffProfileByUserId(10L);

        assertNotNull(response);
        assertNotNull(response.data());
        assertEquals(10L, response.data().userId());
        assertNotNull(response.data().staffProfile());
        assertEquals("CB001", response.data().staffProfile().staffId());
    }
}
