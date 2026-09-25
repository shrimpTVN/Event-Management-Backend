package com.ddd.application.service.user;

import com.ddd.application.dto.user.StudentProfileSummaryDto;

public interface UserQueryService {
    StudentProfileSummaryDto getProfileByEmail(String email);
}
