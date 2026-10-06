package com.ddd.application.service.fanpage;

import com.ddd.application.dto.fanpage.FanpageRegisterDto;

public interface FanpageService {
    void createFanpage(FanpageRegisterDto registerDto, String email);

    void acceptFanpage(Long fanpageId);

    void banFanpage(Long fanpageId);

    void addMemberToFanpage(Long fanpageId, Long userId, String email);

    void removeMemberFromFanpage(Long fanpageId, Long userId, String email);
}
