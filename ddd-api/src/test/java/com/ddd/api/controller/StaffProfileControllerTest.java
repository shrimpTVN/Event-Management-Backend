package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.user.req.StaffProfileCreateRequestDto;
import com.ddd.api.dto.user.req.StaffProfileDetailsCreateRequestDto;
import com.ddd.api.dto.user.res.StaffProfileDetailsResponseDto;
import com.ddd.api.dto.user.res.StaffProfileResponseDto;
import com.ddd.api.mapper.StaffProfileApiMapper;
import com.ddd.api.mapper.StudentProfileApiMapper;
import com.ddd.api.mapper.UserInfoApiMapper;
import com.ddd.api.mapper.UserApiMapper;
import com.ddd.application.dto.user.StaffProfileCreateDto;
import com.ddd.application.dto.user.StaffProfileDetailsCreateDto;
import com.ddd.application.dto.user.StaffProfileDetailsDto;
import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.application.service.fanpage.FanpageRegistrationEligibilityService;
import com.ddd.application.service.user.AccountSecurityQueryService;
import com.ddd.application.service.user.AdminUserQueryService;
import com.ddd.application.service.user.UserCommandService;
import com.ddd.application.service.user.UserQueryService;
import com.ddd.application.service.userprofile.StaffProfileCreationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffProfileControllerTest {

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private AdminUserQueryService adminUserQueryService;

    @Mock
    private UserCommandService userCommandService;

    @Mock
    private AccountSecurityQueryService accountSecurityQueryService;

    @Mock
    private StaffProfileCreationService staffProfileCreationService;

    @Mock
    private FanpageRegistrationEligibilityService fanpageRegistrationEligibilityService;

    @Mock
    private StudentProfileApiMapper studentProfileApiMapper;

    @Mock
    private StaffProfileApiMapper staffProfileApiMapper;

    @Mock
    private UserInfoApiMapper userInfoApiMapper;

    @Mock
    private UserApiMapper userApiMapper;

    @InjectMocks
    private UserController userController;

    @Test
    @DisplayName("createStaffProfile should return 201 with created staff profile")
    void createStaffProfile_shouldReturnCreatedProfile() {
        StaffProfileDetailsCreateRequestDto detailsReq = new StaffProfileDetailsCreateRequestDto(
                "CB01234", "School of CS", "Lecturer", LocalDate.of(1990, 1, 1), "0901234567");
        StaffProfileCreateRequestDto req = new StaffProfileCreateRequestDto("An", "Nguyen", "MALE", detailsReq);

        StaffProfileDetailsCreateDto detailsDto = new StaffProfileDetailsCreateDto(
                "CB01234", "School of CS", "Lecturer", LocalDate.of(1990, 1, 1), "0901234567");
        StaffProfileCreateDto createDto = new StaffProfileCreateDto("An", "Nguyen", "MALE", detailsDto);

        StaffProfileDetailsDto detailsSummary = new StaffProfileDetailsDto(
                "CB01234", "School of CS", "Lecturer", LocalDate.of(1990, 1, 1), "0901234567");
        StaffProfileSummaryDto summary = new StaffProfileSummaryDto(
                10L, "An", "Nguyen", "an@example.com", "MALE", null, Set.of("STUDENT"), detailsSummary);

        StaffProfileDetailsResponseDto detailsRes = new StaffProfileDetailsResponseDto(
                "CB01234", "School of CS", "Lecturer", LocalDate.of(1990, 1, 1), "0901234567");
        StaffProfileResponseDto res = new StaffProfileResponseDto(
                10L, "An", "Nguyen", "an@example.com", "MALE", null, Set.of("STUDENT"), detailsRes);

        when(staffProfileApiMapper.toStaffProfileCreateDto(req)).thenReturn(createDto);
        when(staffProfileCreationService.createForCurrentUser(createDto)).thenReturn(summary);
        when(staffProfileApiMapper.toStaffProfileResponseDto(summary)).thenReturn(res);

        BaseResponse<StaffProfileResponseDto> response = userController.createStaffProfile(req);

        assertNotNull(response);
        assertNotNull(response.data());
        assertEquals("An", response.data().firstName());
        assertEquals("Nguyen", response.data().lastName());
        assertEquals("CB01234", response.data().staffProfile().staffId());

        verify(staffProfileCreationService).createForCurrentUser(createDto);
    }
}
