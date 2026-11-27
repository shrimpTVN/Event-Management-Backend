package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.fanpage.res.ManagedFanpageResponseDto;
import com.ddd.api.dto.user.res.StaffProfileDetailsResponseDto;
import com.ddd.api.dto.user.res.StaffProfileResponseDto;
import com.ddd.api.dto.user.res.StudentProfileDetailsResponseDto;
import com.ddd.api.dto.user.res.UserInfoResponseDto;
import com.ddd.api.mapper.FanpageApiMapper;
import com.ddd.api.mapper.StaffProfileApiMapper;
import com.ddd.api.mapper.UserInfoApiMapper;
import com.ddd.application.dto.fanpage.ManagedFanpageInfoDto;
import com.ddd.application.dto.user.StaffProfileDetailsDto;
import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.application.dto.user.StudentProfileDetailsDto;
import com.ddd.application.dto.user.UserInfoDto;
import com.ddd.application.service.fanpage.FanpageQueryService;
import com.ddd.application.service.user.AdminUserQueryService;
import com.ddd.application.service.user.UserQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private AdminUserQueryService adminUserQueryService;

    @Mock
    private UserInfoApiMapper userInfoApiMapper;

    @Mock
    private StaffProfileApiMapper staffProfileApiMapper;

    @Mock
    private FanpageQueryService fanpageQueryService;

    @Mock
    private FanpageApiMapper fanpageApiMapper;

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

        BaseResponse<Page<UserInfoResponseDto>> response = userController.getAllUsers(true, null, null, 0, 10, "createdAt", "desc");

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

    @Test
    @DisplayName("getUserProfile should return UserInfoResponseDto with studentProfile and fanpageAdminProfile")
    void getUserProfile_shouldReturnUserInfoResponseDto() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("dual@ctu.edu.vn");

        StudentProfileDetailsDto studentDto = new StudentProfileDetailsDto("B2303827", 49, "School of CS", "SE", "7480103", null, 1L, 10L);
        StaffProfileDetailsDto staffDto = new StaffProfileDetailsDto("CB001", "CLB AI", "Admin", LocalDate.of(2000, 1, 1), "0912345678");
        UserInfoDto summary = new UserInfoDto(
                5L, "dual@ctu.edu.vn", Set.of("STUDENT", "FANPAGE_ADMIN"), "STUDENT",
                "Loc", "Doan", "MALE", true, null, "https://cdn/avt.png", studentDto, staffDto
        );

        StudentProfileDetailsResponseDto studentRes = new StudentProfileDetailsResponseDto("B2303827", 49, "School of CS", "SE", "7480103", null, 1L, 10L);
        StaffProfileDetailsResponseDto staffRes = new StaffProfileDetailsResponseDto("CB001", "CLB AI", "Admin", LocalDate.of(2000, 1, 1), "0912345678");
        UserInfoResponseDto expected = new UserInfoResponseDto(
                5L, "dual@ctu.edu.vn", Set.of("STUDENT", "FANPAGE_ADMIN"), "STUDENT",
                "Loc", "Doan", "MALE", true, null, "https://cdn/avt.png", studentRes, staffRes
        );

        when(userQueryService.getCurrentUserProfileByEmail("dual@ctu.edu.vn")).thenReturn(summary);
        when(userInfoApiMapper.toUserInfoResponseDto(summary)).thenReturn(expected);

        BaseResponse<UserInfoResponseDto> response = userController.getUserProfile(auth);

        assertNotNull(response.data());
        assertEquals(5L, response.data().userId());
        assertEquals(10L, response.data().studentProfile().majorId());
        assertEquals("CB001", response.data().fanpageAdminProfile().staffId());
    }

    @Test
    @DisplayName("getAdminUserFanpages should return managed fanpages for target user")
    void getAdminUserFanpages_shouldReturnManagedFanpages() {
        ManagedFanpageInfoDto info = new ManagedFanpageInfoDto(7L, "CLB AI", "Desc", "https://cdn/fp.png", "CLB AI", "ADMIN");
        ManagedFanpageResponseDto dto = new ManagedFanpageResponseDto(7L, "CLB AI", "Desc", "https://cdn/fp.png", "CLB AI", "ADMIN");

        when(fanpageQueryService.getManagedFanpagesByUserId(10L)).thenReturn(List.of(info));
        when(fanpageApiMapper.toResponseDto(info)).thenReturn(dto);

        BaseResponse<List<ManagedFanpageResponseDto>> response = userController.getAdminUserFanpages(10L);

        assertNotNull(response.data());
        assertEquals(1, response.data().size());
        assertEquals(7L, response.data().get(0).fanpageId());
        assertEquals("ADMIN", response.data().get(0).role());
    }

    @Test
    @DisplayName("getAllUsers should forward nullable isActive, role, and keyword filters")
    void getAllUsers_withRoleAndKeyword_shouldDelegateToService() {
        when(adminUserQueryService.getAllUsers(null, "FANPAGE_ADMIN", "ngoc", 0, 20, "createdAt", "desc"))
                .thenReturn(Page.empty());

        BaseResponse<Page<UserInfoResponseDto>> response = userController.getAllUsers(
                null, "FANPAGE_ADMIN", "ngoc", 0, 20, "createdAt", "desc"
        );

        assertNotNull(response.data());
        verify(adminUserQueryService).getAllUsers(null, "FANPAGE_ADMIN", "ngoc", 0, 20, "createdAt", "desc");
    }
}
