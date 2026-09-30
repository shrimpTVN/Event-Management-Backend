package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.user.req.StudentProfileRequestDto;
import com.ddd.api.dto.user.req.UpdateAvatarRequestDto;
import com.ddd.api.dto.user.req.UpdateRoleRequestDto;
import com.ddd.api.dto.user.res.StudentProfileResponseDto;
import com.ddd.api.dto.user.res.UserResponseDto;
import com.ddd.api.mapper.StudentProfileApiMapper;
import com.ddd.api.mapper.UserApiMapper;
import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.application.dto.user.StudentProfileSummaryDto;
import com.ddd.application.dto.user.UserSummaryDto;
import com.ddd.application.service.user.UserCommandService;
import com.ddd.application.service.user.UserQueryService;
import com.ddd.application.service.userprofile.StudentProfileCommandService;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;
    private final StudentProfileApiMapper studentProfileApiMapper;
    private final UserApiMapper userApiMapper;
    private final StudentProfileCommandService studentProfileCommandService;


    @GetMapping("/me/profile")
    public BaseResponse<StudentProfileResponseDto> getUserProfile(Authentication authentication) {
        String email = authentication.getName();
        StudentProfileSummaryDto profileSummaryDto = userQueryService.getProfileByEmail(email);
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
        StudentProfileSummaryDto profileSummaryDto = userQueryService.getProfileById(id);
        return BaseResponse.of(studentProfileApiMapper.toStudentProfileResponseDto(profileSummaryDto));
    }

    @PutMapping("/me/profile")
    public BaseResponse<StudentProfileResponseDto> updateUserProfile(Authentication authentication,
                                                                     @RequestBody StudentProfileRequestDto requestDto) {
        String email = authentication.getName();
        studentProfileCommandService.updateProfileByEmail(email, studentProfileApiMapper.toStudentProfileDto(requestDto));
        StudentProfileSummaryDto updatedProfile = userQueryService.getProfileByEmail(email);

        return BaseResponse.of(studentProfileApiMapper.toStudentProfileResponseDto(updatedProfile));
    }


    @PatchMapping("/me/profile/change-avatar")
    public BaseResponse<StudentProfileResponseDto> updateAvatar(Authentication authentication,
                                                                @RequestBody UpdateAvatarRequestDto updateAvatarRequestDto) {
        String email = authentication.getName();
        studentProfileCommandService.updateAvatarByEmail(email, updateAvatarRequestDto.avatarUrl());
        StudentProfileSummaryDto updatedProfile = userQueryService.getProfileByEmail(email);

        return BaseResponse.of(studentProfileApiMapper.toStudentProfileResponseDto(updatedProfile));
    }

    @PutMapping("/admin/{id}/profile")
    public BaseResponse<StudentProfileResponseDto> updateAdminProfile(@PathVariable Long id,
                                                                      @RequestBody StudentProfileRequestDto studentProfileRequestDto) {

        studentProfileCommandService.updateProfileById(id, studentProfileApiMapper.toStudentProfileDto(studentProfileRequestDto));
        return BaseResponse.of(studentProfileApiMapper.toStudentProfileResponseDto(userQueryService.getProfileById(id)));
    }

    @PatchMapping("/admin/{id}/change-status")
    public BaseResponse<UserResponseDto> updateAdminProfileStatus(@PathVariable Long id) {
        userCommandService.changeStatusUser(id);
        return BaseResponse.of(userApiMapper.toUserResponseDto(userQueryService.findUserById(id)));
    }

    @PatchMapping("/admin/{id}/change-role")
    public BaseResponse<UserResponseDto> updateAdminProfileRole(@PathVariable Long id,
                                                                @RequestBody UpdateRoleRequestDto updateRoleRequestDto) {
        userCommandService.changeRoleUser(id, updateRoleRequestDto.roleName());
        return BaseResponse.of(userApiMapper.toUserResponseDto(userQueryService.findUserById(id)));
    }

}
