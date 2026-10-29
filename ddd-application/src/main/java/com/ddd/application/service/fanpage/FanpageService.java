package com.ddd.application.service.fanpage;

import com.ddd.application.dto.fanpage.FanpageAdminUpdateDto;
import com.ddd.application.dto.fanpage.FanpageRegisterDto;
import com.ddd.application.dto.fanpage.FanpageSystemAdminUpdateDto;

public interface FanpageService {
    void createFanpage(FanpageRegisterDto registerDto, String email);

    void acceptFanpage(Long fanpageId);

    void banFanpage(Long fanpageId);

    void addMemberToFanpage(Long fanpageId, Long userId, String email);

    void removeMemberFromFanpage(Long fanpageId, Long userId, String email);

    void unbanFanpage(Long fanpageId);

    void updateFanpageByFanpageAdmin(Long fanpageId, FanpageAdminUpdateDto updateDto, String email);

    void updateFanpageByAdmin(Long fanpageId, FanpageSystemAdminUpdateDto updateDto);

    void deleteFanpage(Long fanpageId);

    void followFanpage(Long fanpageId, String email);

    void unfollowFanpage(Long fanpageId, String email);
}
