package com.ddd.application.mapper;

import com.ddd.application.dto.role.RoleDto;
import com.ddd.domain.model.Role;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RoleDtoMapper {
    //Chuyen tu domain model -> dto
    RoleDto toDto(Role role);

    //Chuyen tu dto - > domain model
    @Mapping(target = "id", ignore = true)
    Role toEntity(RoleDto roleDto);

    //Cai nao null thi khong ghi de
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(RoleDto roleDto, @MappingTarget Role role);
}
