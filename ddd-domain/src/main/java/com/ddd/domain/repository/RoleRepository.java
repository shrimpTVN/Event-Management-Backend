package com.ddd.domain.repository;

import com.ddd.domain.model.Role;
import lombok.RequiredArgsConstructor;

import java.util.List;

public interface RoleRepository {

    List<Role> findAll();

    void deleteRole(Long id);

    Role updateRole(Long id, Role role);

    Role findById(Long id);

    Role saveRole(Role role);
}
