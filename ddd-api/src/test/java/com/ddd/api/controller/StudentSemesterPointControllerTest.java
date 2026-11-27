package com.ddd.api.controller;

import com.ddd.api.mapper.StudentSemesterPointApiMapper;
import com.ddd.application.service.semesterpoint.StudentSemesterPointQueryService;
import com.ddd.application.dto.semesterpoint.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StudentSemesterPointControllerTest {
    @Test void serializesRawTotalMetadataAndSummaryWithoutCapping() throws Exception {
        var service=mock(StudentSemesterPointQueryService.class);
        var mapper=Mappers.getMapper(StudentSemesterPointApiMapper.class);
        var mvc=MockMvcBuilders.standaloneSetup(new StudentSemesterPointController(service,mapper)).build();
        var leaf=new StudentSemesterPointFlattenedItemDto(2L,"Leaf",null,10,null,1,1L,true,1,true,false,52L,15,8,7,null,true,100,null,true);
        var root=new StudentSemesterPointGroupedTreeDto(1L,"Root",null,20,null,0,null,true,1,false,51L,10,8,7,null,false,50,null,true,List.of(leaf));
        when(service.getSemesterPointTreeForCurrentStudent(9L)).thenReturn(List.of(root));
        when(service.getSummaryForCurrentStudent(9L)).thenReturn(new StudentSemesterPointSummaryDto(9L,120,100,"Xuất sắc",null,0,3));
        mvc.perform(get("/student-semester-points/grouped-tree/9")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].items[0].totalPoint").value(15))
            .andExpect(jsonPath("$.data[0].items[0].isAtMaximum").value(true))
            .andExpect(jsonPath("$.data[0].completionPercentage").value(50));
        mvc.perform(get("/student-semester-points/me/semesters/9/summary")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalPoint").value(120)).andExpect(jsonPath("$.data.classificationLabel").value("Xuất sắc"))
            .andExpect(jsonPath("$.data.activitiesCount").value(3));
    }
    @Test void categoryRequestsAcceptZeroMaximumAndRejectNegative() {
        try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator=factory.getValidator();
            var zero=new com.ddd.api.dto.pointcategory.req.UpdatePointCategoryRequestDto("Root","",0,null,null,1,true,false);
            org.junit.jupiter.api.Assertions.assertTrue(validator.validate(zero).isEmpty());
            var negative=new com.ddd.api.dto.pointcategory.req.UpdatePointCategoryRequestDto("Root",null,-1,null,null,1,true,false);
            org.junit.jupiter.api.Assertions.assertFalse(validator.validate(negative).isEmpty());
            var create=new com.ddd.api.dto.pointcategory.req.PointCategoryRequestDto("Root","",0,java.time.LocalDate.now(),null,1,false);
            org.junit.jupiter.api.Assertions.assertTrue(validator.validate(create).isEmpty());
        }
    }
}
