package com.ddd.domain.repository;

import com.ddd.domain.model.Role;
import lombok.RequiredArgsConstructor;

public interface RoleRepository {
    Role findById(Long id);
}
