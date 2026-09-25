package com.ddd.application.service.role;

import com.ddd.application.dto.role.RoleDto;
import java.util.List;

public interface RoleService {
    List<RoleDto> findAll();
    RoleDto findById(Long id);
    RoleDto createRole(RoleDto roleDto);
    RoleDto updateRole(Long id, RoleDto roleDto);
    void deleteRole(Long id);
}
