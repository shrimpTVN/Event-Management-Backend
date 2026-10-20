package com.ddd.application.service.fanpage;

import com.ddd.application.dto.fanpage.FanpageInfoDto;
import com.ddd.application.dto.fanpage.FanpageMemberDto;
import com.ddd.application.dto.fanpage.FanpageSearchCriteriaDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface FanpageQueryService {
    List<FanpageMemberDto> getMembersOfFanpage(Long fanpageId);

    Page<FanpageInfoDto> searchAndFilterFanpages(FanpageSearchCriteriaDto criteria);

    FanpageInfoDto getFanpageById(Long fanpageId, String currentUserEmail, boolean isAdmin);

    List<FanpageInfoDto> getMyManagedFanpages(String email);
}
