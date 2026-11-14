package com.ddd.application.service.fanpage;

import com.ddd.application.dto.fanpage.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FanpageQueryService {
    List<FanpageMemberDto> getMembersOfFanpage(Long fanpageId);

    Page<FanpageInfoDto> searchAndFilterFanpages(FanpageSearchCriteriaDto criteria);

    FanpageInfoDto getFanpageById(Long fanpageId);

    List<ManagedFanpageInfoDto> getMyManagedFanpages();

    FanpageFollowCountDto getFollowerCount(Long fanpageId);

    Page<FanpageInfoDto> getFollowingFanpages(Pageable pageable);
}
