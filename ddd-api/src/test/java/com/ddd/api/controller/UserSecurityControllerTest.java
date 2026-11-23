package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.user.res.AccountSecurityResponseDto;
import com.ddd.api.mapper.StaffProfileApiMapper;
import com.ddd.api.mapper.StudentProfileApiMapper;
import com.ddd.api.mapper.UserInfoApiMapper;
import com.ddd.api.mapper.UserApiMapper;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSecurityControllerTest {

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
    @DisplayName("getAccountSecurity should return 200 with hasPassword=true and no-store header")
    void getAccountSecurity_hasPasswordTrue() {
        when(accountSecurityQueryService.hasPassword()).thenReturn(true);

        ResponseEntity<BaseResponse<AccountSecurityResponseDto>> response = userController.getAccountSecurity();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getHeaders().getCacheControl().contains("no-store"));
        assertNotNull(response.getBody());
        assertTrue(response.getBody().data().hasPassword());
        verify(accountSecurityQueryService).hasPassword();
    }

    @Test
    @DisplayName("getAccountSecurity should return 200 with hasPassword=false and no-store header")
    void getAccountSecurity_hasPasswordFalse() {
        when(accountSecurityQueryService.hasPassword()).thenReturn(false);

        ResponseEntity<BaseResponse<AccountSecurityResponseDto>> response = userController.getAccountSecurity();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getHeaders().getCacheControl().contains("no-store"));
        assertNotNull(response.getBody());
        assertFalse(response.getBody().data().hasPassword());
        verify(accountSecurityQueryService).hasPassword();
    }
}
