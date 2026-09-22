package com.ddd.infrastructure.repository;


import com.ddd.domain.model.Role;
import com.ddd.domain.repository.RoleRepository;
import com.ddd.infrastructure.entity.RoleJpaEntity;
import com.ddd.infrastructure.mapper.RoleMapper;
import com.ddd.infrastructure.repository.jpaRepository.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
}
