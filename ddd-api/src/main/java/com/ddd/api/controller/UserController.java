package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.fanpage.res.ManagedFanpageResponseDto;
import com.ddd.api.dto.user.req.StaffProfileCreateRequestDto;
import com.ddd.api.dto.user.req.StaffProfileUpdateRequestDto;
import com.ddd.api.dto.user.req.StudentProfileRequestDto;
import com.ddd.api.dto.user.req.UpdateAvatarRequestDto;
import com.ddd.api.dto.user.res.AccountSecurityResponseDto;
import com.ddd.api.dto.user.res.FanpageRegistrationEligibilityResponseDto;
import com.ddd.api.dto.user.res.StaffProfileResponseDto;
import com.ddd.api.dto.user.res.StudentProfileResponseDto;
import com.ddd.api.dto.user.res.UserInfoResponseDto;
import com.ddd.api.dto.user.res.UserResponseDto;
import com.ddd.api.mapper.FanpageApiMapper;
import com.ddd.api.mapper.StaffProfileApiMapper;
import com.ddd.api.mapper.StudentProfileApiMapper;
import com.ddd.api.mapper.UserInfoApiMapper;
import com.ddd.api.mapper.UserApiMapper;
import com.ddd.application.dto.fanpage.FanpageRegistrationEligibilityDto;
import com.ddd.application.dto.fanpage.ManagedFanpageInfoDto;
import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.application.dto.user.StudentProfileSummaryDto;
import com.ddd.application.dto.user.UserInfoDto;
import com.ddd.application.dto.user.UserSummaryDto;
import com.ddd.application.service.fanpage.FanpageQueryService;
import com.ddd.application.service.fanpage.FanpageRegistrationEligibilityService;
import com.ddd.application.service.user.AccountSecurityQueryService;
import com.ddd.application.service.user.AdminUserQueryService;
import com.ddd.application.service.user.UserCommandService;
import com.ddd.application.service.user.UserQueryService;
import com.ddd.application.service.userprofile.StaffProfileCreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserQueryService userQueryService;
    private final AdminUserQueryService adminUserQueryService;
    private final UserCommandService userCommandService;
    private final AccountSecurityQueryService accountSecurityQueryService;
    private final StaffProfileCreationService staffProfileCreationService;
    private final FanpageRegistrationEligibilityService fanpageRegistrationEligibilityService;
    private final FanpageQueryService fanpageQueryService;
    private final FanpageApiMapper fanpageApiMapper;
    private final StudentProfileApiMapper studentProfileApiMapper;
    private final StaffProfileApiMapper staffProfileApiMapper;
    private final UserInfoApiMapper userInfoApiMapper;
    private final UserApiMapper userApiMapper;

    @GetMapping("/me/fanpage-registration-eligibility")
    public BaseResponse<FanpageRegistrationEligibilityResponseDto> getFanpageRegistrationEligibility() {
        FanpageRegistrationEligibilityDto dto = fanpageRegistrationEligibilityService.evaluateCurrentUser();
        return BaseResponse.of(new FanpageRegistrationEligibilityResponseDto(
                dto.status(),
                dto.reasonCode(),
                dto.requiresReauthentication()
        ));
    }

    @PostMapping("/me/staff-profile")
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<StaffProfileResponseDto> createStaffProfile(
            @RequestBody @Valid StaffProfileCreateRequestDto requestDto) {
        StaffProfileSummaryDto summary = staffProfileCreationService.createForCurrentUser(
                staffProfileApiMapper.toStaffProfileCreateDto(requestDto));
        return BaseResponse.of(staffProfileApiMapper.toStaffProfileResponseDto(summary));
    }

    @GetMapping("/me/security")
    public ResponseEntity<BaseResponse<AccountSecurityResponseDto>> getAccountSecurity() {
        boolean hasPassword = accountSecurityQueryService.hasPassword();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(BaseResponse.of(new AccountSecurityResponseDto(hasPassword)));
    }

    @GetMapping("/me/staff-profile")
    public BaseResponse<StaffProfileResponseDto> getStaffProfile() {
        StaffProfileSummaryDto summary = userQueryService.getStaffProfile();
        return BaseResponse.of(staffProfileApiMapper.toStaffProfileResponseDto(summary));
    }

    @PatchMapping("/me/staff-profile")
    public BaseResponse<StaffProfileResponseDto> updateStaffProfile(
            @RequestBody @Valid StaffProfileUpdateRequestDto requestDto) {
        StaffProfileSummaryDto summary = userCommandService.updateStaffProfile(
                staffProfileApiMapper.toStaffProfileUpdateDto(requestDto));
        return BaseResponse.of(staffProfileApiMapper.toStaffProfileResponseDto(summary));
    }

    @GetMapping("/search-users")
    public BaseResponse<List<UserResponseDto>> searchUsers(@RequestParam(required = false) String keyword) {
        List<UserSummaryDto> users = userQueryService.searchUsers(keyword);
        return BaseResponse.of(users.stream().map(userApiMapper::toUserResponseDto).toList());
    }

    @GetMapping("/me/profile")
    public BaseResponse<UserInfoResponseDto> getUserProfile(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        UserInfoDto userInfo = userQueryService.getCurrentUserProfileByEmail(email);
        return BaseResponse.of(userInfoApiMapper.toUserInfoResponseDto(userInfo));
    }

    @GetMapping("/admin/profiles")
    public BaseResponse<Page<StudentProfileResponseDto>> getAdminProfiles(@RequestParam(defaultValue = "true") boolean isActive,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "10") int size,
                                                                          @RequestParam(defaultValue = "createdAt") String sortBy,
                                                                          @RequestParam(defaultValue = "desc") String sortDir) {

        Page<StudentProfileSummaryDto> pageProfiles = userQueryService.getAllProfiles(isActive, page, size, sortBy, sortDir);
        return BaseResponse.of(pageProfiles.map(studentProfileApiMapper::toStudentProfileResponseDto));
    }

    @GetMapping("/admin")
    public BaseResponse<Page<UserInfoResponseDto>> getAllUsers(@RequestParam(required = false) Boolean isActive,
                                                               @RequestParam(required = false) String role,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size,
                                                               @RequestParam(defaultValue = "createdAt") String sortBy,
                                                               @RequestParam(defaultValue = "desc") String sortDir) {
        Page<UserInfoDto> pageUsers = (role == null || role.isBlank()) && (keyword == null || keyword.isBlank()) && isActive != null
                ? adminUserQueryService.getAllUsers(isActive, page, size, sortBy, sortDir)
                : adminUserQueryService.getAllUsers(isActive, role, keyword, page, size, sortBy, sortDir);
        return BaseResponse.of(pageUsers.map(userInfoApiMapper::toUserInfoResponseDto));
    }

    @GetMapping("/admin/{userId}/staff-profile")
    public BaseResponse<StaffProfileResponseDto> getAdminStaffProfileByUserId(@PathVariable Long userId) {
        StaffProfileSummaryDto summary = adminUserQueryService.getStaffProfileByUserId(userId);
        return BaseResponse.of(staffProfileApiMapper.toStaffProfileResponseDto(summary));
    }

    @GetMapping("/admin/{userId}/fanpages")
    public BaseResponse<List<ManagedFanpageResponseDto>> getAdminUserFanpages(@PathVariable Long userId) {
        List<ManagedFanpageInfoDto> fanpages = fanpageQueryService.getManagedFanpagesByUserId(userId);
        return BaseResponse.of(fanpages.stream().map(fanpageApiMapper::toResponseDto).toList());
    }

    @GetMapping("/admin/{id}/profile")
    public BaseResponse<StudentProfileResponseDto> getAdminProfileById(@PathVariable Long id) {
        StudentProfileSummaryDto summary = userQueryService.getProfileStudentById(id);
        return BaseResponse.of(studentProfileApiMapper.toStudentProfileResponseDto(summary));
    }

    @PutMapping("/me/profile")
    public BaseResponse<StudentProfileResponseDto> updateUserProfile(
            Authentication authentication,
            @RequestBody @Valid StudentProfileRequestDto studentProfileRequestDto) {
        String email = authentication.getName();
        StudentProfileSummaryDto summary = userCommandService.updateStudentProfile(
                email, studentProfileApiMapper.toStudentProfileUpdateDto(studentProfileRequestDto));
        return BaseResponse.of(studentProfileApiMapper.toStudentProfileResponseDto(summary));
    }

    @PatchMapping("/me/profile/change-avatar")
    public BaseResponse<UserResponseDto> updateAvatar(
            @RequestBody UpdateAvatarRequestDto updateAvatarRequestDto) {
        String url = updateAvatarRequestDto != null ? updateAvatarRequestDto.avatarUrl() : null;
        UserSummaryDto summary = userCommandService.updateAvatar(url);
        return BaseResponse.of(userApiMapper.toUserResponseDto(summary));
    }

    @PutMapping("/admin/{id}/profile")
    public BaseResponse<StudentProfileResponseDto> updateAdminProfile(
            @PathVariable Long id,
            @RequestBody @Valid StudentProfileRequestDto studentProfileRequestDto) {
        StudentProfileSummaryDto summary = userCommandService.updateStudentProfileById(
                id, studentProfileApiMapper.toStudentProfileUpdateDto(studentProfileRequestDto));
        return BaseResponse.of(studentProfileApiMapper.toStudentProfileResponseDto(summary));
    }

    @DeleteMapping("/me/out-fanpage-member/{fanpageId}")
    public BaseResponse<Void> outFanpageMember(@PathVariable Long fanpageId) {
        userCommandService.outFanpageMember(fanpageId);
        return BaseResponse.ok();
    }

    @PatchMapping("/admin/{id}/change-status")
    public BaseResponse<UserResponseDto> updateAdminProfileStatus(@PathVariable Long id) {
        UserSummaryDto userSummary = userCommandService.updateProfileStatusById(id);
        return BaseResponse.of(userApiMapper.toUserResponseDto(userSummary));
    }

    @PatchMapping("/admin/{id}/change-role")
    public BaseResponse<Void> updateAdminProfileRole(@PathVariable Long id,
                                                     @RequestParam String role) {
        userCommandService.assignRolesToUser(id, Set.of(role));
        return BaseResponse.ok();
    }

    @PatchMapping("/admin/{id}/change-roles")
    public BaseResponse<Void> updateAdminProfileRoles(@PathVariable Long id,
                                                      @RequestBody Set<String> roles) {
        userCommandService.assignRolesToUser(id, roles);
        return BaseResponse.ok();
    }

}
