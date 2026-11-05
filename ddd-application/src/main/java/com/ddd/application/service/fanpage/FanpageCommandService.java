package com.ddd.application.service.fanpage;

import com.ddd.application.dto.fanpage.FanpageAdminUpdateDto;
import com.ddd.application.dto.fanpage.FanpageRegisterDto;
import com.ddd.application.dto.fanpage.FanpageSystemAdminUpdateDto;

public interface FanpageCommandService {
    void createFanpage(FanpageRegisterDto registerDto, String email);

    void acceptFanpage(Long fanpageId);

    void banFanpage(Long fanpageId);

    void inviteMemberToFanpage(Long fanpageId, String memberEmail);

    void removeMemberFromFanpage(Long fanpageId, String memberEmail);

    void unbanFanpage(Long fanpageId);

    void updateFanpageByFanpageAdmin(Long fanpageId, FanpageAdminUpdateDto updateDto, String email);

    void updateFanpageByAdmin(Long fanpageId, FanpageSystemAdminUpdateDto updateDto);

    void deleteFanpage(Long fanpageId);

    void followFanpage(Long fanpageId, String email);

    void unfollowFanpage(Long fanpageId, String email);

    void appointFanpageAdmin(Long fanpageId, String memberEmail);
}
