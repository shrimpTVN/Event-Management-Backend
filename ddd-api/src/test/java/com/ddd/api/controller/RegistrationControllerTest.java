package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.fanpage.req.BatchAttendanceRequestDto;
import com.ddd.api.mapper.RegistrationApiMapper;
import com.ddd.application.service.registration.RegistrationCommandService;
import com.ddd.application.service.registration.RegistrationQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private RegistrationCommandService registrationCommandService;

    @Mock
    private RegistrationQueryService registrationQueryService;

    @Mock
    private RegistrationApiMapper registrationApiMapper;

    @InjectMocks
    private RegistrationController registrationController;

    @Test
    @DisplayName("fanpageMemberBatchAbsent should delegate to command service and return ok")
    void fanpageMemberBatchAbsent_success() {
        Long eventId = 10L;
        List<Long> studentUserIds = List.of(101L, 102L);
        BatchAttendanceRequestDto request = new BatchAttendanceRequestDto(studentUserIds);

        BaseResponse<Void> response = registrationController.fanpageMemberBatchAbsent(eventId, request);

        assertNotNull(response);
        verify(registrationCommandService).fanpageMemberBatchAbsent(eventId, studentUserIds);
    }

    @Test
    @DisplayName("fanpageMemberBatchCheckIn should delegate to command service and return ok")
    void fanpageMemberBatchCheckIn_success() {
        Long eventId = 10L;
        List<Long> studentUserIds = List.of(101L, 102L);
        BatchAttendanceRequestDto request = new BatchAttendanceRequestDto(studentUserIds);

        BaseResponse<Void> response = registrationController.fanpageMemberBatchCheckIn(eventId, request);

        assertNotNull(response);
        verify(registrationCommandService).fanpageMemberCheckIn(eventId, studentUserIds);
    }

    @Test
    @DisplayName("fanpageMemberBatchCheckOut should delegate to command service and return ok")
    void fanpageMemberBatchCheckOut_success() {
        Long eventId = 10L;
        List<Long> studentUserIds = List.of(101L, 102L);
        BatchAttendanceRequestDto request = new BatchAttendanceRequestDto(studentUserIds);

        BaseResponse<Void> response = registrationController.fanpageMemberBatchCheckOut(eventId, request);

        assertNotNull(response);
        verify(registrationCommandService).fanpageMemberCheckOut(eventId, studentUserIds);
    }
}
