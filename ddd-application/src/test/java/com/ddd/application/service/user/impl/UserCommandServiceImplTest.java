package com.ddd.application.service.user.impl;

import com.ddd.application.dto.user.StaffProfileDetailsUpdateDto;
import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.application.dto.user.StaffProfileUpdateDto;
import com.ddd.application.mapper.StaffProfileDtoMapper;
import com.ddd.application.mapper.StudentProfileDtoMapper;
import com.ddd.application.mapper.UserDtoMapper;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.entity.FanpageAdminProfileJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.FanpageAdminProfileJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.MajorJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.StudentProfileJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceImplTest {

    @Mock
    private UserDtoMapper userDtoMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private StudentProfileJpaRepository studentProfileJpaRepository;

    @Mock
    private FanpageAdminProfileJpaRepository fanpageAdminProfileJpaRepository;

    @Mock
    private MajorJpaRepository majorJpaRepository;

    @Mock
    private StudentProfileDtoMapper studentProfileDtoMapper;

    @Mock
    private SecurityContextPort securityContextPort;

    @Mock
    private FanpageMemberRepository fanpageMemberRepository;

    private final StaffProfileDtoMapper staffProfileDtoMapper = Mappers.getMapper(StaffProfileDtoMapper.class);

    private UserCommandServiceImpl userCommandService;

    @BeforeEach
    void setUp() {
        userCommandService = new UserCommandServiceImpl(
                userDtoMapper,
                userRepository,
                passwordEncoder,
                userJpaRepository,
                studentProfileJpaRepository,
                fanpageAdminProfileJpaRepository,
                majorJpaRepository,
                studentProfileDtoMapper,
                staffProfileDtoMapper,
                securityContextPort,
                fanpageMemberRepository
        );
    }

    @Test
    void updateStaffProfile_shouldUpdateFields_whenValid() {
        when(securityContextPort.getCurrentUserId()).thenReturn(5L);

        UserJpaEntity user = new UserJpaEntity();
        user.setId(5L);
        user.setFirstName("OldFirst");
        user.setLastName("OldLast");
        user.setGender("OTHER");
        when(userJpaRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userJpaRepository.save(any(UserJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

        FanpageAdminProfileJpaEntity staffProfile = new FanpageAdminProfileJpaEntity();
        staffProfile.setId(10L);
        staffProfile.setStaffId("STAFF05");
        staffProfile.setTitle("OldTitle");
        staffProfile.setPhoneNumber("0123456789");
        staffProfile.setDob(LocalDate.of(1990, 1, 1));
        when(fanpageAdminProfileJpaRepository.findByUserId(5L)).thenReturn(Optional.of(staffProfile));
        when(fanpageAdminProfileJpaRepository.save(any(FanpageAdminProfileJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

        StaffProfileDetailsUpdateDto detailsUpdate = new StaffProfileDetailsUpdateDto(
                "NewTitle", LocalDate.of(1992, 5, 20), "0987654321",
                true, true, true
        );
        StaffProfileUpdateDto updateDto = new StaffProfileUpdateDto(
                "NewFirst", "NewLast", "MALE", detailsUpdate,
                true, true, true, true
        );

        StaffProfileSummaryDto result = userCommandService.updateStaffProfile(updateDto);

        assertNotNull(result);
        assertEquals("NewFirst", user.getFirstName());
        assertEquals("NewLast", user.getLastName());
        assertEquals("MALE", user.getGender());
        assertEquals("NewTitle", staffProfile.getTitle());
        assertEquals("0987654321", staffProfile.getPhoneNumber());
        assertEquals(LocalDate.of(1992, 5, 20), staffProfile.getDob());
    }

    @Test
    void updateStaffProfile_shouldThrow404_whenUpdatingStaffProfileOnUserWithoutStaffRecord() {
        when(securityContextPort.getCurrentUserId()).thenReturn(6L);

        UserJpaEntity user = new UserJpaEntity();
        user.setId(6L);
        when(userJpaRepository.findById(6L)).thenReturn(Optional.of(user));
        when(fanpageAdminProfileJpaRepository.findByUserId(6L)).thenReturn(Optional.empty());

        StaffProfileDetailsUpdateDto detailsUpdate = new StaffProfileDetailsUpdateDto(
                "Lecturer", null, null,
                true, false, false
        );
        StaffProfileUpdateDto updateDto = new StaffProfileUpdateDto(
                null, null, null, detailsUpdate,
                false, false, false, true
        );

        assertThrows(ResourceNotFoundException.class, () -> userCommandService.updateStaffProfile(updateDto));
    }

    @Test
    void updateStaffProfile_shouldAllowNullToClearOptionalFields() {
        when(securityContextPort.getCurrentUserId()).thenReturn(7L);

        UserJpaEntity user = new UserJpaEntity();
        user.setId(7L);
        when(userJpaRepository.findById(7L)).thenReturn(Optional.of(user));

        FanpageAdminProfileJpaEntity staffProfile = new FanpageAdminProfileJpaEntity();
        staffProfile.setId(11L);
        staffProfile.setTitle("OldTitle");
        staffProfile.setPhoneNumber("0123456789");
        staffProfile.setDob(LocalDate.of(1990, 1, 1));
        when(fanpageAdminProfileJpaRepository.findByUserId(7L)).thenReturn(Optional.of(staffProfile));
        when(fanpageAdminProfileJpaRepository.save(any(FanpageAdminProfileJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

        StaffProfileDetailsUpdateDto detailsUpdate = new StaffProfileDetailsUpdateDto(
                null, null, null,
                true, true, true
        );
        StaffProfileUpdateDto updateDto = new StaffProfileUpdateDto(
                null, null, null, detailsUpdate,
                false, false, false, true
        );

        userCommandService.updateStaffProfile(updateDto);

        assertNull(staffProfile.getTitle());
        assertNull(staffProfile.getDob());
        assertNull(staffProfile.getPhoneNumber());
    }

    @Test
    void updateStaffProfile_shouldThrowBusinessException_whenFirstNameIsBlank() {
        when(securityContextPort.getCurrentUserId()).thenReturn(8L);

        UserJpaEntity user = new UserJpaEntity();
        user.setId(8L);
        when(userJpaRepository.findById(8L)).thenReturn(Optional.of(user));

        StaffProfileUpdateDto updateDto = new StaffProfileUpdateDto(
                "   ", null, null, null,
                true, false, false, false
        );

        assertThrows(BusinessException.class, () -> userCommandService.updateStaffProfile(updateDto));
    }

    @Test
    void updateStaffProfile_shouldThrowBusinessException_whenDateOfBirthInFuture() {
        when(securityContextPort.getCurrentUserId()).thenReturn(9L);

        UserJpaEntity user = new UserJpaEntity();
        user.setId(9L);
        when(userJpaRepository.findById(9L)).thenReturn(Optional.of(user));

        FanpageAdminProfileJpaEntity staffProfile = new FanpageAdminProfileJpaEntity();
        when(fanpageAdminProfileJpaRepository.findByUserId(9L)).thenReturn(Optional.of(staffProfile));

        StaffProfileDetailsUpdateDto detailsUpdate = new StaffProfileDetailsUpdateDto(
                null, LocalDate.now().plusDays(5), null,
                false, true, false
        );
        StaffProfileUpdateDto updateDto = new StaffProfileUpdateDto(
                null, null, null, detailsUpdate,
                false, false, false, true
        );

        assertThrows(BusinessException.class, () -> userCommandService.updateStaffProfile(updateDto));
    }

    @Test
    void updateAvatar_shouldUpdateAvatarUrlAndReturnUserSummaryDto_withoutQueryingStudentProfile() {
        when(securityContextPort.getCurrentUserId()).thenReturn(15L);

        UserJpaEntity user = new UserJpaEntity();
        user.setId(15L);
        user.setEmail("staff@example.com");
        user.setAvatarUrl("https://old.png");
        when(userJpaRepository.findById(15L)).thenReturn(Optional.of(user));
        when(userJpaRepository.save(any(UserJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

        com.ddd.application.dto.user.UserSummaryDto summaryDto = new com.ddd.application.dto.user.UserSummaryDto(
                15L, "staff@example.com", java.util.Set.of("FANPAGE_ADMIN"), "FANPAGE_ADMIN",
                "Staff", "User", "MALE", true, null, "https://new.png"
        );
        when(userDtoMapper.toUserSummaryDto(any(UserJpaEntity.class))).thenReturn(summaryDto);

        com.ddd.application.dto.user.UserSummaryDto result = userCommandService.updateAvatar("https://new.png");

        assertNotNull(result);
        assertEquals("https://new.png", user.getAvatarUrl());
        verify(studentProfileJpaRepository, never()).findByEmail(anyString());
        verify(studentProfileJpaRepository, never()).findByUserId(anyLong());
    }

    @Test
    void updateAvatar_shouldThrowIllegalArgumentException_whenAvatarUrlIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> userCommandService.updateAvatar("   "));
    }

    @Test
    void createPassword_shouldEncodeAndSetPassword_whenUserHasNoPassword() {
        Long userId = 10L;
        String rawPassword = "securePassword123";
        String encodedPassword = "encodedSecurePassword123";

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.setPasswordIfAbsent(userId, encodedPassword)).thenReturn(true);

        assertDoesNotThrow(() -> userCommandService.createPassword(rawPassword));

        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).setPasswordIfAbsent(userId, encodedPassword);
    }

    @Test
    void createPassword_shouldThrowPasswordAlreadySetException_whenPasswordAlreadyExists() {
        Long userId = 10L;
        String rawPassword = "newPassword123";
        String encodedPassword = "encodedPassword123";

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.setPasswordIfAbsent(userId, encodedPassword)).thenReturn(false);

        assertThrows(com.ddd.domain.exception.PasswordAlreadySetException.class,
                () -> userCommandService.createPassword(rawPassword));
        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).setPasswordIfAbsent(userId, encodedPassword);
    }

    @Test
    void createPassword_shouldThrowUnauthorizedException_whenNotAuthenticated() {
        when(securityContextPort.getCurrentUserId()).thenReturn(null);

        assertThrows(com.ddd.domain.exception.UnauthorizedException.class, () ->
                userCommandService.createPassword("newPassword123")
        );
        verifyNoInteractions(userRepository);
    }

    @Test
    void changePassword_shouldThrowPasswordNotSetException_whenPasswordIsNull() {
        String email = "google@example.com";
        com.ddd.domain.model.User user = new com.ddd.domain.model.User();
        user.setEmail(email);
        user.setPassword(null);

        when(userRepository.findByEmail(email)).thenReturn(user);

        assertThrows(com.ddd.domain.exception.PasswordNotSetException.class,
                () -> userCommandService.changePassword("oldPass", "newPass", email));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_shouldThrowPasswordNotSetException_whenPasswordIsBlank() {
        String email = "google@example.com";
        com.ddd.domain.model.User user = new com.ddd.domain.model.User();
        user.setEmail(email);
        user.setPassword("   ");

        when(userRepository.findByEmail(email)).thenReturn(user);

        assertThrows(com.ddd.domain.exception.PasswordNotSetException.class,
                () -> userCommandService.changePassword("oldPass", "newPass", email));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_shouldThrowAuthException_whenOldPasswordDoesNotMatch() {
        String email = "user@example.com";
        com.ddd.domain.model.User user = new com.ddd.domain.model.User();
        user.setEmail(email);
        user.setPassword("hashedOldPassword");

        when(userRepository.findByEmail(email)).thenReturn(user);
        when(passwordEncoder.matches("wrongOldPass", "hashedOldPassword")).thenReturn(false);

        assertThrows(com.ddd.domain.exception.AuthException.class,
                () -> userCommandService.changePassword("wrongOldPass", "newPass", email));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_shouldUpdatePassword_whenOldPasswordMatches() {
        String email = "user@example.com";
        com.ddd.domain.model.User user = new com.ddd.domain.model.User();
        user.setEmail(email);
        user.setPassword("hashedOldPassword");

        when(userRepository.findByEmail(email)).thenReturn(user);
        when(passwordEncoder.matches("correctOldPass", "hashedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("hashedNewPass");

        userCommandService.changePassword("correctOldPass", "newPass", email);

        assertEquals("hashedNewPass", user.getPassword());
        verify(userRepository).save(user);
    }
}
