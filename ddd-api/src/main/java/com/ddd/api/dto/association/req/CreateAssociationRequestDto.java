package com.ddd.api.dto.association.req;
import jakarta.validation.constraints.NotBlank;
public record CreateAssociationRequestDto(@NotBlank String name, String address, String description) {}
