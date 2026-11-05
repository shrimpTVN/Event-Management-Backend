package com.ddd.application.service.fanpage;

import com.ddd.application.dto.fanpage.FanpageMemberInvitationInfoDto;
import com.ddd.domain.enums.InvatitationStatusEnum;
import org.springframework.data.domain.Page;

import java.util.List;

public interface FanpageMemberInvitationQueryService {
    List<FanpageMemberInvitationInfoDto> getAllFanpageMemberInvitationsForUser(String userEmail);

    Page<FanpageMemberInvitationInfoDto> getAllFanpageMemberInvitationsForFanpage(
            Long fanpageId,
            InvatitationStatusEnum status,
            int page,
            int size,
            String sortBy,
            String sortDir,
            String adminEmail
    );
}
