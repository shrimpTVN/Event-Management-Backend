package com.ddd.application.service.criteria.impl;

import com.ddd.application.dto.criteria.CriteriaDto;
import com.ddd.application.dto.criteria.CriteriaTreeNodeDto;
import com.ddd.application.mapper.CriteriaDtoMapper;
import com.ddd.domain.enums.CriteriaScopeEnum;
import com.ddd.domain.model.Criteria;
import com.ddd.domain.repository.CriteriaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CriteriaServiceImplTest {

    @Mock
    private CriteriaRepository criteriaRepository;

    @Mock
    private CriteriaDtoMapper criteriaDtoMapper;

    @InjectMocks
    private CriteriaServiceImpl service;

    @Test
    @DisplayName("Should default requiredOccurrences to 1 when null on createCriteria")
    void createCriteria_whenRequiredOccurrencesNull_shouldDefaultToOne() {
        CriteriaDto inputDto = new CriteriaDto(null, "Hội nhập tốt", null, "UNIVERSITY", 0, 1, 1, null, null, true);
        Criteria mapped = new Criteria();
        mapped.setName("Hội nhập tốt");
        mapped.setRequiredOccurrences(null);

        Criteria saved = new Criteria();
        saved.setId(402L);
        saved.setRequiredOccurrences(1);

        CriteriaDto expected = new CriteriaDto(402L, "Hội nhập tốt", null, "UNIVERSITY", 0, 1, 1, 1, null, true);

        when(criteriaDtoMapper.toCriteria(inputDto)).thenReturn(mapped);
        when(criteriaRepository.save(mapped)).thenReturn(saved);
        when(criteriaDtoMapper.toDto(saved)).thenReturn(expected);

        CriteriaDto result = service.createCriteria(inputDto);

        assertEquals(1, mapped.getRequiredOccurrences());
        assertEquals(1, result.requiredOccurrences());
    }

    @Test
    @DisplayName("Should expose requiredOccurrences on CriteriaTreeNodeDto in getCriteriaTree")
    void getCriteriaTree_shouldIncludeRequiredOccurrencesOnNodes() {
        Criteria containerRoot = new Criteria();
        containerRoot.setId(1L);
        containerRoot.setName("SV5T");
        containerRoot.setLevel(0);
        containerRoot.setChildrenOrder(1);
        containerRoot.setRequiredOccurrences(1);
        containerRoot.setParentId(null);

        Criteria parent = new Criteria();
        parent.setId(4L);
        parent.setName("Hội nhập tốt");
        parent.setLevel(1);
        parent.setChildrenOrder(1);
        parent.setRequiredChildren(1);
        parent.setRequiredOccurrences(1);
        parent.setParentId(1L);

        Criteria leaf = new Criteria();
        leaf.setId(402L);
        leaf.setName("Tham gia hoạt động tình nguyện 3 ngày");
        leaf.setLevel(2);
        leaf.setChildrenOrder(1);
        leaf.setRequiredOccurrences(3);
        leaf.setParentId(4L);

        when(criteriaRepository.findAllByIsActive(true)).thenReturn(List.of(containerRoot, parent, leaf));

        List<CriteriaTreeNodeDto> tree = service.getCriteriaTree();

        assertEquals(1, tree.size());
        assertEquals(1, tree.get(0).requiredOccurrences());
        assertEquals(1, tree.get(0).children().size());
        assertEquals(3, tree.get(0).children().get(0).requiredOccurrences());
    }

    @Test
    @DisplayName("Should filter criteria tree by scope and unwrap container root for that scope")
    void getCriteriaTree_whenScopeProvided_shouldFilterByScopeAndUnwrapContainerRoot() {
        Criteria uniRoot = new Criteria();
        uniRoot.setId(1L);
        uniRoot.setName("SV5T University");
        uniRoot.setScope("UNIVERSITY");
        uniRoot.setLevel(0);
        uniRoot.setChildrenOrder(1);
        uniRoot.setParentId(null);

        Criteria uniMain = new Criteria();
        uniMain.setId(2L);
        uniMain.setName("Đạo đức tốt");
        uniMain.setScope("UNIVERSITY");
        uniMain.setLevel(1);
        uniMain.setChildrenOrder(1);
        uniMain.setParentId(1L);

        Criteria schoolRoot = new Criteria();
        schoolRoot.setId(100L);
        schoolRoot.setName("SV5T School");
        schoolRoot.setScope("FACULTY");
        schoolRoot.setLevel(0);
        schoolRoot.setChildrenOrder(1);
        schoolRoot.setParentId(null);

        when(criteriaRepository.findAllByIsActive(true)).thenReturn(List.of(uniRoot, uniMain, schoolRoot));

        List<CriteriaTreeNodeDto> uniTree = service.getCriteriaTree(CriteriaScopeEnum.UNIVERSITY);
        assertEquals(1, uniTree.size());
        assertEquals(2L, uniTree.get(0).id());
        assertEquals("UNIVERSITY", uniTree.get(0).scope());

        List<CriteriaTreeNodeDto> schoolTree = service.getCriteriaTree(CriteriaScopeEnum.FACULTY);
        assertEquals(1, schoolTree.size());
        assertEquals(100L, schoolTree.get(0).id());
        assertEquals("FACULTY", schoolTree.get(0).scope());
    }
}
