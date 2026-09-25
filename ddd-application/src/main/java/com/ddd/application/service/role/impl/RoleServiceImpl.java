package com.ddd.application.service.role.impl;

import com.ddd.application.dto.role.RoleDto;
import com.ddd.application.mapper.RoleDtoMapper;
import com.ddd.application.service.role.RoleService;
import com.ddd.domain.model.Role;
import com.ddd.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleDtoMapper roleDtoMapper;

    @Override
    public List<RoleDto> findAll(){
        return roleRepository.findAll()
                .stream()
                .map(roleDtoMapper::toDto)
                .toList();
    }

    @Override
    public RoleDto findById(Long id){
        Role role = roleRepository.findById(id);
        return roleDtoMapper.toDto(role);
    }

    @Override
    @Transactional
    public RoleDto createRole(RoleDto roleDto){
        Role role = roleDtoMapper.toEntity(roleDto);
        return roleDtoMapper.toDto(roleRepository.saveRole(role));
    }

    @Override
    @Transactional
    public RoleDto updateRole(Long id, RoleDto roleDto){
        Role role = roleRepository.findById(id);
        role = roleDtoMapper.toEntity(roleDto);
        return roleDtoMapper.toDto(roleRepository.saveRole(role));
    }

    @Override
    @Transactional
    public void deleteRole(Long id){
        roleRepository.deleteRole(id);
    }
}
