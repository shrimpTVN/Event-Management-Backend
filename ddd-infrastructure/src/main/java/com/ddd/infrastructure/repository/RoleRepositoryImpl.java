package com.ddd.infrastructure.repository;


import com.ddd.domain.model.Role;
import com.ddd.domain.repository.RoleRepository;
import com.ddd.infrastructure.entity.RoleJpaEntity;
import com.ddd.infrastructure.mapper.RoleMapper;
import com.ddd.infrastructure.repository.jpaRepository.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {
    private final RoleJpaRepository roleJpaRepository;
    private final RoleMapper roleMapper;
    @Override
    public Role findById(Long id) {
        RoleJpaEntity role = roleJpaRepository.findById(id).orElseThrow(() -> new RuntimeException("Role not found with id: " + id));
        return roleMapper.toDomainModel(role);
    }

    @Override
    public List<Role> findAll() {
        return roleJpaRepository.findAllByActive(true).stream()
                .map(roleMapper::toDomainModel).toList();
    }

    @Override
    public void deleteRole(Long id) {
        RoleJpaEntity roleEntity = roleJpaRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Role not found with id: " + id)
                );

        roleEntity.setIsActive(false);
        roleJpaRepository.save(roleEntity);
    }

    @Override
    public Role saveRole(Role role){
        RoleJpaEntity roleJpaEntity = roleMapper.toJpaEntity(role);
        return roleMapper.toDomainModel(roleJpaRepository.save(roleJpaEntity));
    }

    @Override
    public Role updateRole(Long id, Role role) {
        RoleJpaEntity roleEntity = roleJpaRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Role not found with id: " + id)
                );

        roleEntity.setName(role.getName());
        roleEntity.setIsActive(role.isActive());

        RoleJpaEntity savedEntity =
                roleJpaRepository.save(roleEntity);

        return roleMapper.toDomainModel(savedEntity);
    }
}
