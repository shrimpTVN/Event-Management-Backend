package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.criteria.req.CriteriaRequestDto;
import com.ddd.api.dto.criteria.res.CriteriaResponseDto;
import com.ddd.api.mapper.CriteriaApiMapper;
import com.ddd.application.dto.criteria.CriteriaDto;
import com.ddd.application.service.criteria.CriteriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/criterias")
@RequiredArgsConstructor
public class CriteriaController {
    private final CriteriaService criteriaService;
    private final CriteriaApiMapper criteriaApiMapper;

    @PostMapping
    public BaseResponse<CriteriaResponseDto> createCriteria(@Valid @RequestBody CriteriaRequestDto request) {
        CriteriaDto criteriaDto = criteriaApiMapper.toCriteriaDto(request);
        CriteriaDto result = criteriaService.createCriteria(criteriaDto);
        return BaseResponse.of(criteriaApiMapper.toCriteriaResponseDto(result));
    }

    @GetMapping("/{id}")
    public BaseResponse<CriteriaResponseDto> getCriteriaById(@PathVariable Long id) {
        CriteriaDto result = criteriaService.getCriteriaById(id);
        return BaseResponse.of(criteriaApiMapper.toCriteriaResponseDto(result));
    }

    @GetMapping
    public BaseResponse<List<CriteriaResponseDto>> getAllCriterias(
            @RequestParam(defaultValue = "true") boolean isActive) {
        List<CriteriaDto> criteriaList = criteriaService.getAllCriterias(isActive);
        List<CriteriaResponseDto> responseList = criteriaList.stream()
                .map(criteriaApiMapper::toCriteriaResponseDto)
                .collect(Collectors.toList());
        return BaseResponse.of(responseList);
    }

    @PutMapping("/{id}")
    public BaseResponse<CriteriaResponseDto> updateCriteria(
            @PathVariable Long id,
            @Valid @RequestBody CriteriaRequestDto request) {
        CriteriaDto criteriaDto = criteriaApiMapper.toCriteriaDto(request);
        CriteriaDto result = criteriaService.updateCriteria(id, criteriaDto);
        return BaseResponse.of(criteriaApiMapper.toCriteriaResponseDto(result));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> deleteCriteria(@PathVariable Long id) {
        criteriaService.deleteCriteria(id);
        return BaseResponse.ok();
    }

    @PatchMapping("/{id}/change-status")
    public BaseResponse<Void> changeStatus(@PathVariable Long id) {
        criteriaService.changeStatus(id);
        return BaseResponse.ok();
    }
}
