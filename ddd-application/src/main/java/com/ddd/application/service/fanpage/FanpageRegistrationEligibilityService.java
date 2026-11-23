package com.ddd.application.service.fanpage;

import com.ddd.application.dto.fanpage.FanpageRegistrationEligibilityDto;

public interface FanpageRegistrationEligibilityService {

    FanpageRegistrationEligibilityDto evaluateCurrentUser();

    void assertCurrentUserCanRegister();
}
