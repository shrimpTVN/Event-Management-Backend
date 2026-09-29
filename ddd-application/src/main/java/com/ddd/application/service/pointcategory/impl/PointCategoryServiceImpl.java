package com.ddd.application.service.pointcategory.impl;

import com.ddd.application.dto.pointcategory.PointCategoryCreateDto;
import com.ddd.application.dto.pointcategory.PointCategoryInfoDto;
import com.ddd.application.mapper.PointCategoryDtoMapper;
import com.ddd.application.service.pointcategory.PointCategoryService;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.PointCategory;
import com.ddd.domain.repository.PointCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointCategoryServiceImpl implements PointCategoryService {
    private final PointCategoryRepository pointCategoryRepository;
    private final PointCategoryDtoMapper pointCategoryDtoMapper;

    @Override
    public PointCategoryInfoDto createPointCategory(PointCategoryCreateDto pointCategoryCreateDto) {
        PointCategory pointCategory = pointCategoryDtoMapper.toEntity(pointCategoryCreateDto);

        //calculate level
        if (pointCategoryCreateDto.parentCategoryId() == null){
            pointCategory.setLevel(0);
        } else {
            PointCategory parentCategory = pointCategoryRepository.findById(pointCategoryCreateDto.parentCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            pointCategory.setLevel(parentCategory.getLevel() + 1);
        }


        return pointCategoryDtoMapper.toInfoDto(pointCategoryRepository.save(pointCategory));
    }

    @Override
    public List<PointCategoryInfoDto> getAllPointCategories() {

        return pointCategoryDtoMapper.toInfoDtoList(pointCategoryRepository.findAll());
    }

    @Override
    public PointCategoryInfoDto getPointCategoryById(Long id) {
        PointCategory category = pointCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Point category not found with id: " + id));
        return pointCategoryDtoMapper.toInfoDto(category);
    }
}

