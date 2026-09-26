package com.ddd.domain.repository;

import com.ddd.domain.model.Association;
import java.util.List;

public interface AssociationRepository {
    List<Association> findAll();
    Association findById(Long id);
    Association saveAssociation(Association association);
    void deleteAssociation(Long id);
}
