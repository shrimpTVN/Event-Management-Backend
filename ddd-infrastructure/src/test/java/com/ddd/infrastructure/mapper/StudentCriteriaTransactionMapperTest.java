package com.ddd.infrastructure.mapper;

import com.ddd.infrastructure.entity.StudentCriteriaTransactionJpaEntity;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StudentCriteriaTransactionMapperTest {
    @Test void editingNotePreservesInactiveStatusAndRecordIdentity() {
        var entity = new StudentCriteriaTransactionJpaEntity();
        entity.setId(42L);
        entity.setIsActive(false);
        entity.setCompletionSource("MANUAL");
        var mapper = new StudentCriteriaTransactionMapperImpl();
        var domain = mapper.toDomain(entity);
        assertFalse(domain.isActive());
        domain.setNote("Updated reason");
        var updated = mapper.toEntity(domain);
        assertFalse(updated.getIsActive());
        assertEquals(42L,updated.getId());
        assertEquals("Updated reason",updated.getNote());
    }
}
