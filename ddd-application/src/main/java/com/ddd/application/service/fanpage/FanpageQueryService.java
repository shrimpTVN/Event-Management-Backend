package com.ddd.application.service.fanpage;

import com.ddd.application.dto.fanpage.FanpageMemberDto;

import java.util.List;

public interface FanpageQueryService {
    List<FanpageMemberDto> getMembersOfFanpage(Long fanpageId);
}
