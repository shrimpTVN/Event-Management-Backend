package com.ddd.application.service.criteria;

import com.ddd.application.dto.criteria.CriteriaDto;
import java.util.List;

public interface CriteriaService {
    CriteriaDto createCriteria(CriteriaDto criteriaDto);
    CriteriaDto updateCriteria(Long id, CriteriaDto criteriaDto);
    void deleteCriteria(Long id);
    CriteriaDto getCriteriaById(Long id);
    List<CriteriaDto> getAllCriterias(boolean isActive);
    void changeStatus(Long id);
}
