package com.ddd.application.service.pointcategory.impl;

import com.ddd.application.dto.pointcategory.PointCategoryCreateDto;
import com.ddd.application.dto.pointcategory.PointCategoryInfoDto;
import com.ddd.application.mapper.PointCategoryDtoMapper;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.PointCategory;
import com.ddd.domain.model.Semester;
import com.ddd.domain.repository.PointCategoryRepository;
import com.ddd.domain.repository.SemesterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointCategoryServiceImplTest {

        @Mock
        private PointCategoryRepository pointCategoryRepository;

        @Mock
        private SemesterRepository semesterRepository;

        @Mock
        private PointCategoryDtoMapper pointCategoryDtoMapper;

        @InjectMocks
        private PointCategoryServiceImpl pointCategoryService;

        @Test
        @DisplayName("Should create root point category with level 0 when parentCategoryId is null")
        void createPointCategory_whenParentCategoryIdIsNull_shouldSetLevel0AndSave() {
                PointCategoryCreateDto createDto = new PointCategoryCreateDto(
                                "Academics",
                                "Academic activities",
                                100,
                                LocalDate.now(),
                                null,
                                1,
                                false);

                PointCategory entity = new PointCategory();
                entity.setName("Academics");
                entity.setChildrenOrder(1);

                PointCategory savedEntity = new PointCategory();
                savedEntity.setId(1L);
                savedEntity.setName("Academics");
                savedEntity.setLevel(0);
                savedEntity.setChildrenOrder(1);

                PointCategoryInfoDto expectedInfoDto = new PointCategoryInfoDto(
                                1L,
                                "Academics",
                                "Academic activities",
                                100,
                                LocalDate.now(),
                                0,
                                null,
                                true,
                                1,
                                false);

                when(pointCategoryDtoMapper.toEntity(createDto)).thenReturn(entity);
                when(pointCategoryRepository.save(entity)).thenReturn(savedEntity);
                when(pointCategoryDtoMapper.toInfoDto(savedEntity)).thenReturn(expectedInfoDto);

                PointCategoryInfoDto result = pointCategoryService.createPointCategory(createDto);

                assertNotNull(result);
                assertEquals(0, entity.getLevel());
                assertEquals(expectedInfoDto, result);
                verify(pointCategoryRepository, never()).findById(any());
                verify(pointCategoryRepository).save(entity);
        }

        @Test
        @DisplayName("Should create child point category with level parent+1 when parentCategoryId exists")
        void createPointCategory_whenParentCategoryExists_shouldSetParentLevelPlusOneAndSave() {
                PointCategoryCreateDto createDto = new PointCategoryCreateDto(
                                "Scientific Research",
                                "Research activities",
                                50,
                                LocalDate.now(),
                                1L,
                                2,
                                false);

                PointCategory entity = new PointCategory();
                entity.setName("Scientific Research");
                entity.setParentCategoryId(1L);
                entity.setChildrenOrder(2);

                PointCategory parentEntity = new PointCategory();
                parentEntity.setId(1L);
                parentEntity.setLevel(0);

                PointCategory savedEntity = new PointCategory();
                savedEntity.setId(2L);
                savedEntity.setName("Scientific Research");
                savedEntity.setLevel(1);
                savedEntity.setParentCategoryId(1L);
                savedEntity.setChildrenOrder(2);

                PointCategoryInfoDto expectedInfoDto = new PointCategoryInfoDto(
                                2L,
                                "Scientific Research",
                                "Research activities",
                                50,
                                LocalDate.now(),
                                1,
                                1L,
                                true,
                                2,
                                false);

                when(pointCategoryDtoMapper.toEntity(createDto)).thenReturn(entity);
                when(pointCategoryRepository.findById(1L)).thenReturn(Optional.of(parentEntity));
                when(pointCategoryRepository.save(entity)).thenReturn(savedEntity);
                when(pointCategoryDtoMapper.toInfoDto(savedEntity)).thenReturn(expectedInfoDto);

                PointCategoryInfoDto result = pointCategoryService.createPointCategory(createDto);

                assertNotNull(result);
                assertEquals(1, entity.getLevel());
                assertEquals(expectedInfoDto, result);
                verify(pointCategoryRepository).findById(1L);
                verify(pointCategoryRepository).save(entity);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when parent category is not found")
        void createPointCategory_whenParentCategoryNotFound_shouldThrowResourceNotFoundException() {
                PointCategoryCreateDto createDto = new PointCategoryCreateDto(
                                "Scientific Research",
                                "Research activities",
                                50,
                                LocalDate.now(),
                                999L,
                                1,
                                false);

                PointCategory entity = new PointCategory();
                entity.setName("Scientific Research");
                entity.setParentCategoryId(999L);
                entity.setChildrenOrder(1);

                when(pointCategoryDtoMapper.toEntity(createDto)).thenReturn(entity);
                when(pointCategoryRepository.findById(999L)).thenReturn(Optional.empty());

                ResourceNotFoundException exception = assertThrows(
                                ResourceNotFoundException.class,
                                () -> pointCategoryService.createPointCategory(createDto));

                assertEquals("Parent category not found", exception.getMessage());
                verify(pointCategoryRepository).findById(999L);
                verify(pointCategoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return all point categories mapped to InfoDto")
        void getAllPointCategories_shouldReturnAllCategories() {
                PointCategory entity = new PointCategory();
                entity.setId(1L);
                entity.setName("Academics");
                entity.setLevel(0);
                entity.setChildrenOrder(1);

                PointCategoryInfoDto infoDto = new PointCategoryInfoDto(
                                1L,
                                "Academics",
                                "Academic activities",
                                100,
                                LocalDate.now(),
                                0,
                                null,
                                true,
                                1,
                                false);

                List<PointCategory> entities = List.of(entity);
                List<PointCategoryInfoDto> expectedDtos = List.of(infoDto);

                when(pointCategoryRepository.findActiveEffectiveAsOf(any(LocalDate.class))).thenReturn(entities);
                when(pointCategoryDtoMapper.toInfoDtoList(entities)).thenReturn(expectedDtos);

                List<PointCategoryInfoDto> result = pointCategoryService.getAllPointCategories();

                assertNotNull(result);
                assertEquals(1, result.size());
                assertEquals(expectedDtos, result);
                verify(pointCategoryRepository).findActiveEffectiveAsOf(any(LocalDate.class));
                verify(pointCategoryDtoMapper).toInfoDtoList(entities);
        }

        @Test
        @DisplayName("Should filter point categories by semester endDate when semesterId is provided")
        void getAllPointCategories_whenSemesterIdProvided_shouldQueryBySemesterEndDate() {
                Long semesterId = 101L;
                LocalDate endDate = LocalDate.of(2026, 5, 31);
                Semester semester = new Semester();
                semester.setId(semesterId);
                semester.setEndDate(endDate);

                PointCategory entity = new PointCategory();
                entity.setId(1L);
                entity.setName("Academics 2008");
                List<PointCategory> entities = List.of(entity);

                PointCategoryInfoDto infoDto = new PointCategoryInfoDto(
                                1L, "Academics 2008", null, 30, LocalDate.of(2008, 12, 5), 0, null, true, 1, false);
                List<PointCategoryInfoDto> expectedDtos = List.of(infoDto);

                when(semesterRepository.findById(semesterId)).thenReturn(Optional.of(semester));
                when(pointCategoryRepository.findActiveEffectiveAsOf(endDate)).thenReturn(entities);
                when(pointCategoryDtoMapper.toInfoDtoList(entities)).thenReturn(expectedDtos);

                List<PointCategoryInfoDto> result = pointCategoryService.getAllPointCategories(semesterId);

                assertEquals(expectedDtos, result);
                verify(semesterRepository).findById(semesterId);
                verify(pointCategoryRepository).findActiveEffectiveAsOf(endDate);
        }
}
