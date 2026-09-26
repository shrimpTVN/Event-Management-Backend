package com.ddd.application.service.association;
import com.ddd.application.dto.association.AssociationDto;
import java.util.List;

public interface AssociationService {
    List<AssociationDto> findAll();
    AssociationDto findById(Long id);
    AssociationDto createAssociation(AssociationDto dto);
    AssociationDto updateAssociation(Long id, AssociationDto dto);
    void deleteAssociation(Long id);
}
