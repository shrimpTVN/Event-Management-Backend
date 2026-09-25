package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.role.req.RoleRequestDto;
import com.ddd.api.dto.role.res.RoleResponseDto;
import com.ddd.api.mapper.RoleApiMapper;
import com.ddd.application.dto.role.RoleDto;
import com.ddd.application.service.role.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final RoleApiMapper roleApiMapper;

    @GetMapping
    public BaseResponse<List<RoleResponseDto>> findAll() {
        List<RoleResponseDto> roles = roleService.findAll().stream()
                .map(roleApiMapper::toResponseDto)
                .toList();
        return BaseResponse.of(roles);
    }

    @GetMapping("/{id}")
    public BaseResponse<RoleResponseDto> findById(@PathVariable Long id) {
        RoleDto roleDto = roleService.findById(id);
        return BaseResponse.of(roleApiMapper.toResponseDto(roleDto));
    }

    @PostMapping
    public BaseResponse<RoleResponseDto> createRole(@Valid @RequestBody RoleRequestDto req) {
        RoleDto roleDto = roleApiMapper.toDto(req);
        RoleDto createdRole = roleService.createRole(roleDto);
        return BaseResponse.of(roleApiMapper.toResponseDto(createdRole));
    }

    @PutMapping("/{id}")
    public BaseResponse<RoleResponseDto> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequestDto req) {
        RoleDto roleDto = roleApiMapper.toDto(req);
        RoleDto updatedRole = roleService.updateRole(id, roleDto);
        return BaseResponse.of(roleApiMapper.toResponseDto(updatedRole));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return BaseResponse.ok();
    }
}
