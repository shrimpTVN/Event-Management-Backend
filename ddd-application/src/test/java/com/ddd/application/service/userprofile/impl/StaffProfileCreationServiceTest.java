package com.ddd.application.service.userprofile.impl;

import com.ddd.application.dto.user.StaffProfileCreateDto;
import com.ddd.application.dto.user.StaffProfileDetailsCreateDto;
import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.application.mapper.StaffProfileDtoMapper;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.exception.StaffIdAlreadyExistsException;
import com.ddd.domain.exception.StaffProfileAlreadyExistsException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.infrastructure.entity.FanpageAdminProfileJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.FanpageAdminProfileJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffProfileCreationServiceTest {

    @Mock
    private SecurityContextPort securityContextPort;

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private FanpageAdminProfileJpaRepository fanpageAdminProfileJpaRepository;

    @Spy
    private StaffProfileDtoMapper staffProfileDtoMapper = Mappers.getMapper(StaffProfileDtoMapper.class);

    @InjectMocks
    private StaffProfileCreationServiceImpl staffProfileCreationService;

    @Test
    @DisplayName("shouldCreateStaffProfileSuccessfullyWhenValid")
    void shouldCreateStaffProfileSuccessfullyWhenValid() {
        Long userId = 10L;
        UserJpaEntity user = new UserJpaEntity();
        user.setId(userId);
        user.setEmail("lecturer@ctu.edu.vn");
        user.setFirstName("OldFirst");
        user.setLastName("OldLast");

        StaffProfileDetailsCreateDto detailsDto = new StaffProfileDetailsCreateDto(
                " CB01234 ",
                " School of CS ",
                " Lecturer ",
                LocalDate.of(1990, 1, 1),
                " 0901234567 "
        );
        StaffProfileCreateDto createDto = new StaffProfileCreateDto(
                " An ",
                " Nguyen ",
                " MALE ",
                detailsDto
        );

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(userJpaRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fanpageAdminProfileJpaRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(fanpageAdminProfileJpaRepository.existsByStaffId("CB01234")).thenReturn(false);
        when(userJpaRepository.save(any(UserJpaEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(fanpageAdminProfileJpaRepository.save(any(FanpageAdminProfileJpaEntity.class))).thenAnswer(i -> {
            FanpageAdminProfileJpaEntity p = i.getArgument(0);
            p.setId(100L);
            return p;
        });

        StaffProfileSummaryDto summary = staffProfileCreationService.createForCurrentUser(createDto);

        assertNotNull(summary);
        assertEquals("An", summary.firstName());
        assertEquals("Nguyen", summary.lastName());
        assertEquals("MALE", summary.gender());
        assertNotNull(summary.staffProfile());
        assertEquals("CB01234", summary.staffProfile().staffId());
        assertEquals("School of CS", summary.staffProfile().orgName());
        assertEquals("Lecturer", summary.staffProfile().title());
        assertEquals("0901234567", summary.staffProfile().phoneNumber());

        verify(userJpaRepository).save(user);
        verify(fanpageAdminProfileJpaRepository).save(any(FanpageAdminProfileJpaEntity.class));
    }

    @Test
    @DisplayName("shouldThrowStaffProfileAlreadyExistsExceptionWhenUserAlreadyHasProfile")
    void shouldThrowStaffProfileAlreadyExistsExceptionWhenUserAlreadyHasProfile() {
        Long userId = 10L;
        UserJpaEntity user = new UserJpaEntity();
        user.setId(userId);

        StaffProfileDetailsCreateDto detailsDto = new StaffProfileDetailsCreateDto(
                "CB01234", "Org", "Title", null, null);
        StaffProfileCreateDto createDto = new StaffProfileCreateDto("An", "Nguyen", null, detailsDto);

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(userJpaRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fanpageAdminProfileJpaRepository.findByUserId(userId))
                .thenReturn(Optional.of(new FanpageAdminProfileJpaEntity()));

        assertThrows(StaffProfileAlreadyExistsException.class,
                () -> staffProfileCreationService.createForCurrentUser(createDto));

        verify(userJpaRepository, never()).save(any());
        verify(fanpageAdminProfileJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldThrowStaffIdAlreadyExistsExceptionWhenStaffIdAlreadyTaken")
    void shouldThrowStaffIdAlreadyExistsExceptionWhenStaffIdAlreadyTaken() {
        Long userId = 10L;
        UserJpaEntity user = new UserJpaEntity();
        user.setId(userId);

        StaffProfileDetailsCreateDto detailsDto = new StaffProfileDetailsCreateDto(
                " CB01234 ", "Org", "Title", null, null);
        StaffProfileCreateDto createDto = new StaffProfileCreateDto("An", "Nguyen", null, detailsDto);

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(userJpaRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fanpageAdminProfileJpaRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(fanpageAdminProfileJpaRepository.existsByStaffId("CB01234")).thenReturn(true);

        assertThrows(StaffIdAlreadyExistsException.class,
                () -> staffProfileCreationService.createForCurrentUser(createDto));

        verify(userJpaRepository, never()).save(any());
        verify(fanpageAdminProfileJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldThrowUnauthorizedExceptionWhenNotAuthenticated")
    void shouldThrowUnauthorizedExceptionWhenNotAuthenticated() {
        when(securityContextPort.getCurrentUserId()).thenReturn(null);

        StaffProfileCreateDto createDto = new StaffProfileCreateDto("An", "Nguyen", null, null);

        assertThrows(UnauthorizedException.class,
                () -> staffProfileCreationService.createForCurrentUser(createDto));
        verifyNoInteractions(userJpaRepository);
        verifyNoInteractions(fanpageAdminProfileJpaRepository);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenUserNotFound")
    void shouldThrowResourceNotFoundExceptionWhenUserNotFound() {
        Long userId = 404L;
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(userJpaRepository.findById(userId)).thenReturn(Optional.empty());

        StaffProfileCreateDto createDto = new StaffProfileCreateDto("An", "Nguyen", null, null);

        assertThrows(ResourceNotFoundException.class,
                () -> staffProfileCreationService.createForCurrentUser(createDto));
    }
}
