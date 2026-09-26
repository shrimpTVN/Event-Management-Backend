package com.ddd.application.service.association.impl;

import com.ddd.application.dto.association.AssociationDto;
import com.ddd.application.mapper.AssociationDtoMapper;
import com.ddd.application.service.association.AssociationService;
import com.ddd.domain.model.Association;
import com.ddd.domain.repository.AssociationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssociationServiceImpl implements AssociationService {
    private final AssociationRepository associationRepository;
    private final AssociationDtoMapper mapper;

    @Override
    public List<AssociationDto> findAll() {
        return associationRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public AssociationDto findById(Long id) {
        return mapper.toDto(associationRepository.findById(id));
    }

    @Override
    @Transactional
    public AssociationDto createAssociation(AssociationDto dto) {
        Association association = mapper.toEntity(dto);
        return mapper.toDto(associationRepository.saveAssociation(association));
    }

    @Override
    @Transactional
    public AssociationDto updateAssociation(Long id, AssociationDto dto) {
        Association association = associationRepository.findById(id);
        mapper.updateEntityFromDto(association, dto);
        return mapper.toDto(associationRepository.saveAssociation(association));
    }

    @Override
    @Transactional
    public void deleteAssociation(Long id) {
        associationRepository.deleteAssociation(id);
    }
}
