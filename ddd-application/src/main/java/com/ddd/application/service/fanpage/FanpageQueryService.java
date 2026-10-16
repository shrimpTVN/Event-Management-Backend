package com.ddd.application.service.fanpage;

import com.ddd.application.dto.fanpage.FanpageMemberDto;
import com.ddd.application.dto.fanpage.AttendeeInfoDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface FanpageQueryService {
    List<FanpageMemberDto> getMembersOfFanpage(Long fanpageId);

}
