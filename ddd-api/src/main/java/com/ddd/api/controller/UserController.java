package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.user.req.StudentProfileRequestDto;
import com.ddd.api.dto.user.req.UpdateAvatarRequestDto;
import com.ddd.api.dto.user.res.StudentProfileResponseDto;
import com.ddd.api.dto.user.res.UserResponseDto;
import com.ddd.api.mapper.StudentProfileApiMapper;
import com.ddd.api.mapper.UserApiMapper;
import com.ddd.application.dto.user.StudentProfileSummaryDto;
import com.ddd.application.dto.user.UserSummaryDto;
import com.ddd.application.service.user.UserCommandService;
import com.ddd.application.service.user.UserQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;
    private final StudentProfileApiMapper studentProfileApiMapper;
    private final UserApiMapper userApiMapper;


    @GetMapping("/me/profile")
    public BaseResponse<StudentProfileResponseDto> getUserProfile(Authentication authentication) {
        String email = authentication.getName();
        StudentProfileSummaryDto profileSummaryDto = userQueryService.getProfileStudentByEmail(email);
        return BaseResponse.of(studentProfileApiMapper.toStudentProfileResponseDto(profileSummaryDto));
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
    public BaseResponse<Page<UserResponseDto>> getAllUsers(@RequestParam(defaultValue = "true") boolean isActive,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           @RequestParam(defaultValue = "createdAt") String sortBy,
                                                           @RequestParam(defaultValue = "desc") String sortDir) {
        Page<UserSummaryDto> pageUsers = userQueryService.getAllUsers(isActive, page, size, sortBy, sortDir);
        return BaseResponse.of(pageUsers.map(userApiMapper::toUserResponseDto));
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
    public BaseResponse<StudentProfileResponseDto> updateAvatar(
            Authentication authentication,
            @RequestBody UpdateAvatarRequestDto updateAvatarRequestDto) {
        String email = authentication.getName();
        String url = updateAvatarRequestDto.avatarUrl();
        StudentProfileSummaryDto summary = userCommandService.updateAvatar(email, url);
        return BaseResponse.of(studentProfileApiMapper.toStudentProfileResponseDto(summary));
    }

    @PutMapping("/admin/{id}/profile")
    public BaseResponse<StudentProfileResponseDto> updateAdminProfile(
            @PathVariable Long id,
            @RequestBody @Valid StudentProfileRequestDto studentProfileRequestDto) {
        StudentProfileSummaryDto summary = userCommandService.updateStudentProfileById(
                id, studentProfileApiMapper.toStudentProfileUpdateDto(studentProfileRequestDto));
        return BaseResponse.of(studentProfileApiMapper.toStudentProfileResponseDto(summary));
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
