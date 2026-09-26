package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.association.req.*;
import com.ddd.api.dto.association.res.AssociationResponseDto;
import com.ddd.api.mapper.AssociationApiMapper;
import com.ddd.application.dto.association.AssociationDto;
import com.ddd.application.service.association.AssociationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/associations")
@RequiredArgsConstructor
public class AssociationController {
    private final AssociationService service;
    private final AssociationApiMapper mapper;

    @GetMapping
    public BaseResponse<List<AssociationResponseDto>> getAll() {
        List<AssociationResponseDto> res = service.findAll().stream()
                .map(mapper::toResponseDto)
                .toList();
        return BaseResponse.of(res);
    }

    @GetMapping("/{id}")
    public BaseResponse<AssociationResponseDto> getById(@PathVariable Long id) {
        return BaseResponse.of(mapper.toResponseDto(service.findById(id)));
    }

    @PostMapping("/admin")
    public BaseResponse<AssociationResponseDto> create(@Valid @RequestBody CreateAssociationRequestDto req) {
        AssociationDto created = service.createAssociation(mapper.toDto(req));
        return BaseResponse.of(mapper.toResponseDto(created));
    }

    @PutMapping("/admin/{id}")
    public BaseResponse<AssociationResponseDto> update(@PathVariable Long id, @Valid @RequestBody UpdateAssociationRequestDto req) {
        AssociationDto updated = service.updateAssociation(id, mapper.toDto(req));
        return BaseResponse.of(mapper.toResponseDto(updated));
    }

    @DeleteMapping("/admin/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteAssociation(id);
    }
}
