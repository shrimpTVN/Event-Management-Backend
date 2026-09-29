package com.ddd.application.service.criteria.impl;

import com.ddd.application.dto.criteria.CriteriaDto;
import com.ddd.application.mapper.CriteriaDtoMapper;
import com.ddd.application.service.criteria.CriteriaService;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Criteria;
import com.ddd.domain.repository.CriteriaRepository;
import com.ddd.infrastructure.repository.jpaRepository.CriteriaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CriteriaServiceImpl implements CriteriaService {
    private final CriteriaRepository criteriaRepository;
    private final CriteriaJpaRepository criteriaJpaRepository;
    private final CriteriaDtoMapper criteriaDtoMapper;

    @Override
    public CriteriaDto createCriteria(CriteriaDto criteriaDto) {
        Criteria criteria = criteriaDtoMapper.toCriteria(criteriaDto);
        return criteriaDtoMapper.toDto(criteriaRepository.save(criteria));
    }

    @Override
    public CriteriaDto updateCriteria(Long id, CriteriaDto criteriaDto) {
        Criteria criteria = criteriaRepository.findById(id);
        criteriaDtoMapper.updateEntityFromDto(criteria, criteriaDto);
        return criteriaDtoMapper.toDto(criteriaRepository.save(criteria));
    }

    @Override
    public void deleteCriteria(Long id) {
        Criteria criteria = criteriaRepository.findById(id);
        criteriaRepository.delete(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public CriteriaDto getCriteriaById(Long id) {
        return criteriaJpaRepository.findById(id)
                .map(criteriaDtoMapper::toCriteriaDto)
                .orElseThrow(() -> new ResourceNotFoundException("Criteria not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CriteriaDto> getAllCriterias(boolean isActive) {
        return criteriaJpaRepository.findAllByIsActive(isActive).stream()
                .map(criteriaDtoMapper::toCriteriaDto)
                .collect(Collectors.toList());
    }

    @Override
    public void changeStatus(Long id) {
        Criteria criteria = criteriaRepository.findById(id);
        criteria.setActive(!criteria.isActive());
        criteriaRepository.save(criteria);
    }
}
