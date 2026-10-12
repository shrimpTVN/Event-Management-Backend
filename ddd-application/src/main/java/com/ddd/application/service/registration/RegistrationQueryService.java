package com.ddd.application.service.registration;

import com.ddd.application.dto.registration.RegistrationDto;
import org.springframework.data.domain.Page;
import java.util.List;

public interface RegistrationQueryService {
    Page<RegistrationDto> getHistory(String email, int page, int size);
    List<RegistrationDto> getUpcoming(String email);
}
