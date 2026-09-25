package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.user.req.StudentProfileRequestDto;
import com.ddd.api.dto.user.res.StudentProfileResponseDto;
import com.ddd.api.mapper.StudentProfileApiMapper;
import com.ddd.application.dto.user.StudentProfileSummaryDto;
import com.ddd.application.service.user.UserCommandService;
import com.ddd.application.service.user.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
     private final UserQueryService userQueryService;
     private final UserCommandService userCommandService;
     private final StudentProfileApiMapper studentProfileApiMapper;
//    private final UserService userService;

    @GetMapping("/me/profile")
    public BaseResponse<StudentProfileResponseDto> getUserProfile(Authentication authentication){
            String email = authentication.getName();
            StudentProfileSummaryDto profileSummaryDto = userQueryService.getProfileByEmail(email);
            return BaseResponse.of(studentProfileApiMapper.toStudentProfileResponseDto(profileSummaryDto));
    }

    @GetMapping("/admin/profiles")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Page<StudentProfileResponseDto>> getAdminProfiles(@RequestParam(defaultValue = "true") boolean isActive,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "10") int size,
                                                                          @RequestParam(defaultValue = "createdAt") String sortBy,
                                                                          @RequestParam(defaultValue = "desc") String sortDir){
        //userCommandService.getAllProfiles(isActive);
//        Page<StudentProfileResponseDto> profiles = userQueryService.getAllProfiles(isActive);
        return BaseResponse.ok();
    }

    @GetMapping("/admin/{id}/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<StudentProfileResponseDto> getAdminProfileById(@RequestParam Long id){
        //userCommandService.getProfileById(id);
        return BaseResponse.ok();
    }

    @PutMapping("/me/profile")
    public BaseResponse<StudentProfileResponseDto> updateUserProfile(Authentication authentication,
                                                                     @RequestBody StudentProfileRequestDto studentProfileRequestDto){
        //userCommandService.updateProfile(authentication, studentProfileResponseDto);
        return BaseResponse.ok();
    }

    @PatchMapping("/me/profile/change-avatar")
    public BaseResponse<StudentProfileResponseDto> updateAvatar(@RequestBody Map<String, String> avatarUrl){
        //userCommandService.updateAvatar(studentProfileRequestDto);
        return BaseResponse.ok();
    }

    @PutMapping("/admin/{id}/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<StudentProfileResponseDto> updateAdminProfile(@RequestParam Long id,
                                                                       @RequestBody StudentProfileRequestDto studentProfileRequestDto) {
        //userCommandService.updateProfileById(id, studentProfileResponseDto);
        return BaseResponse.ok();
    }

    @PatchMapping("/admin/{id}/profile/change-status")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<StudentProfileResponseDto> updateAdminProfileStatus(@PathVariable Long id) {
        //userCommandService.updateProfileStatusById(id);
        return BaseResponse.ok();
    }

    @PatchMapping("/admin/{id}/profile/change-role")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<StudentProfileResponseDto> updateAdminProfileRole(@PathVariable Long id,
                                                                           @RequestParam String role) {
        //userCommandService.updateProfileRoleById(id, role);
        return BaseResponse.ok();
    }

}
