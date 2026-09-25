package com.ddd.application.service.user;

import com.ddd.application.dto.user.StudentProfileSummaryDto;
import com.ddd.application.dto.user.UserSummaryDto;
import org.springframework.data.domain.Page;

public interface UserQueryService {
    StudentProfileSummaryDto getProfileByEmail(String email);

    Page<StudentProfileSummaryDto> getAllProfiles(boolean isActive, int page, int size, String sortBy, String sortDir);

    Page<UserSummaryDto> getAllUsers(boolean isActive, int page, int size, String sortBy, String sortDir);
}
