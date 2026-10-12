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

    @PostMapping("/admin")
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
            @RequestParam(defaultValue = "true") boolean isActive,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Integer level) {
        List<CriteriaDto> criteriaList;
        if (parentId != null) {
            criteriaList = criteriaService.getCriteriasByParentId(parentId);
        } else if (level != null) {
            criteriaList = criteriaService.getCriteriasByLevel(level);
        } else {
            criteriaList = criteriaService.getAllCriterias(isActive);
        }
        List<CriteriaResponseDto> responseList = criteriaList.stream()
                .map(criteriaApiMapper::toCriteriaResponseDto)
                .toList();
        return BaseResponse.of(responseList);
    }

    @GetMapping("/parentId")
    public BaseResponse<List<CriteriaResponseDto>> getCriteriasByParentId(
            @RequestParam(defaultValue = "0") Long parentId) {
        List<CriteriaDto> criteriaList = criteriaService.getCriteriasByParentId(parentId);
        List<CriteriaResponseDto> responseList = criteriaList.stream()
                .map(criteriaApiMapper::toCriteriaResponseDto)
                .toList();
        return BaseResponse.of(responseList);
    }

    @GetMapping("/level")
    public BaseResponse<List<CriteriaResponseDto>> getCriteriasByLevel(
            @RequestParam(defaultValue = "0") Integer level) {
        List<CriteriaDto> criteriaList = criteriaService.getCriteriasByLevel(level);
        List<CriteriaResponseDto> responseList = criteriaList.stream()
                .map(criteriaApiMapper::toCriteriaResponseDto)
                .toList();
        return BaseResponse.of(responseList);
    }

    @PutMapping("/admin/{id}")
    public BaseResponse<CriteriaResponseDto> updateCriteria(
            @PathVariable Long id,
            @Valid @RequestBody CriteriaRequestDto request) {
        CriteriaDto criteriaDto = criteriaApiMapper.toCriteriaDto(request);
        CriteriaDto result = criteriaService.updateCriteria(id, criteriaDto);
        return BaseResponse.of(criteriaApiMapper.toCriteriaResponseDto(result));
    }

    @DeleteMapping("/admin/{id}")
    public BaseResponse<Void> deleteCriteria(@PathVariable Long id) {
        criteriaService.deleteCriteria(id);
        return BaseResponse.ok();
    }

    @PatchMapping("/admin/{id}/change-status")
    public BaseResponse<Void> changeStatus(@PathVariable Long id) {
        criteriaService.changeStatus(id);
        return BaseResponse.ok();
    }
}
