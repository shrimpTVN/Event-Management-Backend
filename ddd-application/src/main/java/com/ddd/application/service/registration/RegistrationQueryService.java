package com.ddd.application.service.registration;

import com.ddd.application.dto.fanpage.AttendeeInfoDto;
import com.ddd.application.dto.registration.RegistrationDto;
import com.ddd.application.dto.registration.RegistrationInfoDto;
import org.springframework.data.domain.Page;
import java.util.List;

public interface RegistrationQueryService {
   Page<AttendeeInfoDto> getAttendeesOfEvent(Long fanpageId,
                                             Long eventId, int page, int size, String sortBy, String sortDir);

    Page<RegistrationInfoDto> getHistory(int page, int size);
    List<RegistrationInfoDto> getUpcoming();
}
