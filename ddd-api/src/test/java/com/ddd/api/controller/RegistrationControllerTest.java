package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.fanpage.req.BatchAttendanceRequestDto;
import com.ddd.api.mapper.RegistrationApiMapper;
import com.ddd.application.service.registration.RegistrationCommandService;
import com.ddd.application.service.registration.RegistrationQueryService;
import com.ddd.domain.enums.RegistrationStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ddd.api.exception.GlobalExceptionHandler;
import com.ddd.domain.exception.EventFullException;
import com.ddd.domain.exception.EventGenderFullException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    void registrationReportsFullAndBusyAsDistinctConflicts() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(registrationController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        doThrow(new EventFullException(), new CannotAcquireLockException("lock timeout"),
                        new EventGenderFullException("Slots for female participants are full."))
                .when(registrationCommandService).registerEvent(10L);
        mvc.perform(post("/registrations/me/events/10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.exceptionCode").value("EVENT.004"));
        mvc.perform(post("/registrations/me/events/10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.exceptionCode").value("005"));
        mvc.perform(post("/registrations/me/events/10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.exceptionCode").value("EVENT.005"))
                .andExpect(jsonPath("$.message").value("Slots for female participants are full."));
    }

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
        verify(registrationCommandService).fanpageMemberCheckOut(eventId, studentUserIds, RegistrationStatusEnum.COMPLETED);
    }
}
