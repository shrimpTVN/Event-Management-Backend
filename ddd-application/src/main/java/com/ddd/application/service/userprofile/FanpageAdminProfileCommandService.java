package com.ddd.application.service.userprofile;

import com.ddd.application.dto.user.FanpageAdminProfileDto;

public interface FanpageAdminProfileCommandService {
    void createFanpageAdminProfile(FanpageAdminProfileDto fanpageAdminProfileDto, Long userId);
}
