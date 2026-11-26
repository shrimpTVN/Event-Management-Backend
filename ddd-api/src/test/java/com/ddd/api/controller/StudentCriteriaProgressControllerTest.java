package com.ddd.api.controller;
import com.ddd.api.mapper.StudentCriteriaProgressApiMapperImpl;
import com.ddd.application.dto.criteria.*;
import com.ddd.application.service.criteriaprogress.*;
import com.ddd.domain.enums.CriteriaScopeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StudentCriteriaProgressControllerTest {
 @Test void returnsCompleteContractAndAcceptsFacultyScope() throws Exception {
  var service = mock(StudentCriteriaProgressQueryService.class);
  var controller = new StudentCriteriaProgressController(service,
    mock(StudentCriteriaProgressCommandService.class), new StudentCriteriaProgressApiMapperImpl());
  var node = new CriteriaProgressItemDto(402L,"Leaf",null,"FACULTY",1,1,null,0,3,2,false,
    0,true,true,false,false,null,null,null,"Event",66,List.of());
  var response = new StudentCriteriaProgressDto(9L,"2026-2027",0,1,List.of(node),"FACULTY",0,false);
  when(service.getProgress("s",9L,CriteriaScopeEnum.FACULTY)).thenReturn(response);
  var mvc = MockMvcBuilders.standaloneSetup(controller).build();
  mvc.perform(get("/criteria-progress/me/academic-years/9?scope=FACULTY")
    .principal(new UsernamePasswordAuthenticationToken("s","")))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data.scope").value("FACULTY"))
    .andExpect(jsonPath("$.data.criterias[0].id").value(402))
    .andExpect(jsonPath("$.data.criterias[0].requiredOccurrences").value(3))
    .andExpect(jsonPath("$.data.criterias[0].completedOccurrences").value(2))
    .andExpect(jsonPath("$.data.criterias[0].isCompleted").value(false))
    .andExpect(jsonPath("$.data.criterias[0].isLeaf").value(true))
    .andExpect(jsonPath("$.data.isEligible").value(false));
  mvc.perform(get("/criteria-progress/me/academic-years/9?scope=SCHOOL"))
    .andExpect(status().isBadRequest());
 }
 @Test void patchesManualNoteThroughPublicEndpointWithoutCreatingNewRecord() throws Exception {
  var commands = mock(StudentCriteriaTransactionCommandService.class);
  var controller = new CriteriaTransactionController(commands,
    mock(StudentCriteriaTransactionQueryService.class), mock(com.ddd.api.mapper.CriteriaApiMapper.class));
  var mvc = MockMvcBuilders.standaloneSetup(controller).build();
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/criteria-transactions/me/42")
    .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"note\":\"Updated reason\"}"))
    .andExpect(status().isOk());
  verify(commands).updateTransactionNote(42L,"Updated reason");
  verifyNoMoreInteractions(commands);
 }

}
