package com.ddd.application.service.registration;

import com.ddd.domain.enums.RegistrationStatusEnum;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public interface RegistrationCommandService {
    void registerEvent(Long eventId);

    void cancelRegistration(Long eventId, String reason);

    void checkIn(Long eventId, String proofUrl);

    void checkOut(Long eventId, String proofUrl);

    void fanpageMemberCheckIn(Long eventId, List<Long> studentUserIds);

    void fanpageMemberCheckOut(Long eventId, List<Long> studentUserIds, RegistrationStatusEnum status);

    void fanpageMemberBatchAbsent(Long eventId, List<Long> studentUserIds);


}
