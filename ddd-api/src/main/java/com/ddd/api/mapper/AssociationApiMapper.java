package com.ddd.api.mapper;

import com.ddd.api.dto.association.req.*;
import com.ddd.api.dto.association.res.AssociationResponseDto;
import com.ddd.application.dto.association.AssociationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AssociationApiMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    AssociationDto toDto(CreateAssociationRequestDto req);

    @Mapping(target = "id", ignore = true)
    AssociationDto toDto(UpdateAssociationRequestDto req);

    AssociationResponseDto toResponseDto(AssociationDto dto);
}
