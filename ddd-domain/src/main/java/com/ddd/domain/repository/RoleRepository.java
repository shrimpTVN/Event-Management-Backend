package com.ddd.domain.repository;

import com.ddd.domain.model.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {

    List<Role> findAll();

    void deleteRole(Long id);

    Role updateRole(Long id, Role role);

    Role findById(Long id);

    Role saveRole(Role role);

    Role findByName(String name);
}
