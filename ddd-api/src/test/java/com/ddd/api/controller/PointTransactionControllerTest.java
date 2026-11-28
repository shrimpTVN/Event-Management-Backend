package com.ddd.api.controller;

import com.ddd.api.mapper.PointTransactionApiMapper;
import com.ddd.application.service.semesterpoint.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PointTransactionControllerTest {
    @Test void patchUpdatesSelectedRecordAndValidatesPositivePoints() throws Exception {
        var commands=mock(PointTransactionCommandService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new PointTransactionController(
                mock(PointTransactionQueryService.class),commands,mock(PointTransactionApiMapper.class))).build();
        mvc.perform(patch("/point-transactions/me/42").contentType(MediaType.APPLICATION_JSON)
                .content("{\"points\":5,\"description\":\"edited\"}")).andExpect(status().isOk());
        verify(commands).updatePointTransactionForStudent(42L,5,"edited");
        mvc.perform(patch("/point-transactions/me/42").contentType(MediaType.APPLICATION_JSON)
                .content("{\"points\":0}")).andExpect(status().isBadRequest());
        verifyNoMoreInteractions(commands);
    }
}
