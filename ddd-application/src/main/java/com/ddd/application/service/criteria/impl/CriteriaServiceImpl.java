package com.ddd.application.service.criteria.impl;

import com.ddd.application.dto.criteria.CriteriaDto;
import com.ddd.application.mapper.CriteriaDtoMapper;
import com.ddd.application.service.criteria.CriteriaService;
import com.ddd.domain.enums.CriteriaScopeEnum;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Criteria;
import com.ddd.domain.repository.CriteriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CriteriaServiceImpl implements CriteriaService {
    private final CriteriaRepository criteriaRepository;
    private final CriteriaDtoMapper criteriaDtoMapper;

    @Override
    public CriteriaDto createCriteria(CriteriaDto criteriaDto) {
        Criteria criteria = criteriaDtoMapper.toCriteria(criteriaDto);
        if (criteria.getParentId() != null) {
            Criteria parent = criteriaRepository.findById(criteria.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent criteria not found with id: " + criteria.getParentId()));
            criteria.setLevel(parent.getLevel() + 1);
            criteria.setScope(parent.getScope());
        } else {
            criteria.setLevel(0);
            criteria.setScope(criteriaDto.scope() != null ? criteriaDto.scope() : CriteriaScopeEnum.UNIVERSITY.name());
        }
        return criteriaDtoMapper.toDto(criteriaRepository.save(criteria));
    }

    @Override
    public CriteriaDto updateCriteria(Long id, CriteriaDto criteriaDto) {
        Criteria criteria = criteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Criteria not found with id: " + id));
        criteriaDtoMapper.updateEntityFromDto(criteria, criteriaDto);
        return criteriaDtoMapper.toDto(criteriaRepository.save(criteria));
    }

    @Override
    public void deleteCriteria(Long id) {
        Criteria criteria = criteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Criteria not found with id: " + id));
        criteriaRepository.delete(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public CriteriaDto getCriteriaById(Long id) {
        return criteriaRepository.findById(id)
                .map(criteriaDtoMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Criteria not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CriteriaDto> getAllCriterias(boolean isActive) {
        return criteriaRepository.findAll(isActive).stream()
                .map(criteriaDtoMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CriteriaDto> getCriteriasByParentId(Long parentId) {
        return criteriaRepository.findByParentId(parentId).stream()
                .map(criteriaDtoMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CriteriaDto> getCriteriasByLevel(Integer level) {
        return criteriaRepository.findByLevel(level).stream()
                .map(criteriaDtoMapper::toDto)
                .toList();
    }

    @Override
    public void changeStatus(Long id) {
        Criteria criteria = criteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Criteria not found with id: " + id));
        criteria.setActive(!criteria.isActive());
        criteriaRepository.save(criteria);
    }
}

