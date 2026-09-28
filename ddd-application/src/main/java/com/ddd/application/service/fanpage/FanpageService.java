package com.ddd.application.service.fanpage;

import com.ddd.application.dto.fanpage.FanpageRegisterDto;

public interface FanpageService {
    void createFanpage(FanpageRegisterDto registerDto, String email);
}
