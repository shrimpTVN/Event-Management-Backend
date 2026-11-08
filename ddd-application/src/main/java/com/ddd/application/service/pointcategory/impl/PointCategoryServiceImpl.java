package com.ddd.application.service.pointcategory.impl;

import com.ddd.application.dto.pointcategory.*;
import com.ddd.application.mapper.PointCategoryDtoMapper;
import com.ddd.application.service.pointcategory.PointCategoryService;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.PointCategory;
import com.ddd.domain.repository.PointCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PointCategoryServiceImpl implements PointCategoryService {
    private static final Comparator<PointCategory> POINT_CATEGORY_COMPARATOR = Comparator
            .comparing((PointCategory c) -> c.getLevel() != null ? c.getLevel() : 0)
            .thenComparing(c -> c.getChildrenOrder() != null ? c.getChildrenOrder() : 0)
            .thenComparing(PointCategory::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final PointCategoryRepository pointCategoryRepository;
    private final PointCategoryDtoMapper pointCategoryDtoMapper;

    @Override
    public PointCategoryInfoDto createPointCategory(PointCategoryCreateDto pointCategoryCreateDto) {
        PointCategory pointCategory = pointCategoryDtoMapper.toEntity(pointCategoryCreateDto);
        //calculate level
        if (pointCategoryCreateDto.parentCategoryId() == null) {
            pointCategory.setLevel(0);
        } else {
            PointCategory parentCategory = pointCategoryRepository.findById(pointCategoryCreateDto.parentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            pointCategory.setLevel(parentCategory.getLevel() + 1);
        }
        return pointCategoryDtoMapper.toInfoDto(pointCategoryRepository.save(pointCategory));
    }

    @Transactional(readOnly = true)
    @Override
    public List<PointCategoryInfoDto> getAllPointCategories() {
        return pointCategoryDtoMapper.toInfoDtoList(pointCategoryRepository.findAllActive());
    }

    @Transactional(readOnly = true)
    @Override
    public List<PointCategoryTreeNodeDto> getPointCategoryTree() {
        CategoryGroup categoryGroup = groupCategoryAndRetrieveRoots();
        Map<Long, List<PointCategory>> childrenByParent = categoryGroup.childrenByParent();
        List<PointCategory> roots = categoryGroup.roots();

        return roots.stream()
                .map(node -> buildTreeNodeTopDown(node, childrenByParent))
                .toList();
    }

    private PointCategoryTreeNodeDto buildTreeNodeTopDown(PointCategory current,
                                                          Map<Long, List<PointCategory>> childrenByParent) {
        List<PointCategory> childEntities = childrenByParent.getOrDefault(current.getId(), List.of());
        List<PointCategoryTreeNodeDto> children = childEntities.stream()
                .sorted(POINT_CATEGORY_COMPARATOR)
                .map(child -> buildTreeNodeTopDown(child, childrenByParent))
                .toList();

        return new PointCategoryTreeNodeDto(
                current.getId(),
                current.getName(),
                current.getNote(),
                current.getMaximum(),
                current.getDateApply(),
                current.getLevel(),
                current.getParentCategoryId(),
                current.isActive(),
                current.getChildrenOrder(),
                children
        );
    }

    @Transactional(readOnly = true)
    @Override
    public PointCategoryInfoDto getPointCategoryById(Long id) {
        PointCategory category = pointCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Point category not found with id: " + id));
        return pointCategoryDtoMapper.toInfoDto(category);
    }

    @Transactional(readOnly = true)
    @Override
    public List<PointCategoryInfoDto> getPointCategoryByLevel(Integer level) {
        return pointCategoryDtoMapper.toInfoDtoList(pointCategoryRepository.findByLevel(level));
    }

    @Transactional(readOnly = true)
    @Override
    public List<PointCategoryInfoDto> getPointCategoryByParentId(Long parentId) {
        return pointCategoryDtoMapper.toInfoDtoList(pointCategoryRepository.findByParentId(parentId));
    }

    @Override
    public PointCategoryInfoDto updatePointCategory(Long id, PointCategoryUpdateDto pointCategoryUpdateDto) {
        PointCategory existingCategory = pointCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Point category not found with id: " + id));

        pointCategoryDtoMapper.updateEntityFromDto(pointCategoryUpdateDto, existingCategory);

        if (pointCategoryUpdateDto.parentCategoryId() == null) {
            existingCategory.setLevel(0);
        } else {
            PointCategory parentCategory = pointCategoryRepository.findById(pointCategoryUpdateDto.parentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

            if (parentCategory.getId().equals(id)) {
                throw new IllegalArgumentException("A category cannot be its own parent");
            }

            existingCategory.setLevel(parentCategory.getLevel() + 1);
        }

        return pointCategoryDtoMapper.toInfoDto(pointCategoryRepository.save(existingCategory));
    }

    @Override
    public void deletePointCategory(Long id) {
        pointCategoryRepository.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PointCategoryGroupedTreeDto> getGroupedPointCategoryTree() {
        CategoryGroup categoryGroup = groupCategoryAndRetrieveRoots();
        Map<Long, List<PointCategory>> childrenByParent = categoryGroup.childrenByParent();
        List<PointCategory> roots = categoryGroup.roots();

        return roots.stream().map(root -> {
            List<PointCategoryFlattenedItemDto> flatItems = new ArrayList<>();
            flattenDescendantsDfs(root.getId(), childrenByParent, flatItems);
            return new PointCategoryGroupedTreeDto(
                    root.getId(),
                    root.getName(),
                    root.getNote(),
                    root.getMaximum(),
                    root.getDateApply(),
                    root.getLevel(),
                    root.getParentCategoryId(),
                    root.isActive(),
                    root.getChildrenOrder(),
                    flatItems
            );
        }).toList();
    }

    private void flattenDescendantsDfs(Long parentId,
                                       Map<Long, List<PointCategory>> childrenByParent,
                                       List<PointCategoryFlattenedItemDto> resultList) {
        List<PointCategory> directChildren = childrenByParent.getOrDefault(parentId, List.of()).stream()
                .sorted(POINT_CATEGORY_COMPARATOR)
                .toList();

        directChildren.forEach(child -> {
            boolean isLeaf = !childrenByParent.containsKey(child.getId())
                    || childrenByParent.get(child.getId()).isEmpty();

            resultList.add(new PointCategoryFlattenedItemDto(
                    child.getId(),
                    child.getName(),
                    child.getNote(),
                    child.getMaximum(),
                    child.getDateApply(),
                    child.getLevel(),
                    child.getParentCategoryId(),
                    child.isActive(),
                    child.getChildrenOrder(),
                    isLeaf
            ));
            flattenDescendantsDfs(child.getId(), childrenByParent, resultList);
        });

    }

    private CategoryGroup groupCategoryAndRetrieveRoots() {
        List<PointCategory> allCategories = pointCategoryRepository.findAllActive();
        Map<Long, List<PointCategory>> childrenByParent = allCategories.stream()
                .filter(c -> c.getParentCategoryId() != null && c.getParentCategoryId() != 0L)
                .collect(Collectors.groupingBy(PointCategory::getParentCategoryId));

        List<PointCategory> roots = allCategories.stream()
                .filter(c -> c.getParentCategoryId() == null || c.getParentCategoryId() == 0L)
                .sorted(POINT_CATEGORY_COMPARATOR)
                .toList();

        return new CategoryGroup(childrenByParent, roots);
    }
}

