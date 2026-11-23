package com.ddd.application.service.user;

import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.application.dto.user.UserInfoDto;
import org.springframework.data.domain.Page;

public interface AdminUserQueryService {

    Page<UserInfoDto> getAllUsers(boolean isActive, int page, int size, String sortBy, String sortDir);

    StaffProfileSummaryDto getStaffProfileByUserId(Long userId);
}
