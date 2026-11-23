package com.ddd.application.service.userprofile;

import com.ddd.application.dto.user.StaffProfileCreateDto;
import com.ddd.application.dto.user.StaffProfileSummaryDto;

public interface StaffProfileCreationService {
    StaffProfileSummaryDto createForCurrentUser(StaffProfileCreateDto dto);
}
