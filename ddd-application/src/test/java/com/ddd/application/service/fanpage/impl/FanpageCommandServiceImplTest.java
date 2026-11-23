package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.fanpage.FanpageRegisterDto;
import com.ddd.application.mapper.FanpageDtoMapper;
import com.ddd.application.service.fanpage.FanpageRegistrationEligibilityService;
import com.ddd.domain.enums.FanpageRoleEnum;
import com.ddd.domain.enums.FanpageStatusEnum;
import com.ddd.domain.exception.FanpageAdminProfileRequiredException;
import com.ddd.domain.model.Fanpage;
import com.ddd.domain.model.FanpageMember;
import com.ddd.domain.model.User;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FanpageCommandServiceImplTest {

    @Mock
    private FanpageRepository fanpageRepository;
    @Mock
    private FanpageMemberRepository fanpageMemberRepository;
    @Mock
    private FanpageMemberInvitationRepository fanpageMemberInvitationRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private FanpageDtoMapper fanpageDtoMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FollowingRepository followingRepository;
    @Mock
    private SecurityContextPort securityContextPort;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private FanpageRegistrationEligibilityService fanpageRegistrationEligibilityService;

    @InjectMocks
    private FanpageCommandServiceImpl fanpageCommandService;

    @Nested
    @DisplayName("Create Fanpage Profile Gating Tests")
    class CreateFanpageGateTests {

        private final String testEmail = "admin@fanpage.com";
        private final FanpageRegisterDto registerDto = new FanpageRegisterDto(
                "Tech Club",
                "Technology and AI Club",
                "https://avatar.url",
                "ACADEMIC",
                "School of Computing",
                "University",
                "https://cloudinary.com/evidence.pdf",
                "help@example.com",
                "123456789"
        );

        @Test
        @DisplayName("shouldThrowFanpageAdminProfileRequiredExceptionWhenEligibilityFails")
        void shouldThrowFanpageAdminProfileRequiredExceptionWhenEligibilityFails() {
            doThrow(new FanpageAdminProfileRequiredException())
                    .when(fanpageRegistrationEligibilityService).assertCurrentUserCanRegister();

            assertThrows(FanpageAdminProfileRequiredException.class, () ->
                    fanpageCommandService.createFanpage(registerDto));

            verify(fanpageRepository, never()).save(any());
            verify(fanpageMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("shouldAllowFanpageCreationWhenProfileComplete")
        void shouldAllowFanpageCreationWhenProfileComplete() {
            doNothing().when(fanpageRegistrationEligibilityService).assertCurrentUserCanRegister();
            when(fanpageRepository.existsByName(registerDto.name())).thenReturn(false);
            when(securityContextPort.getCurrentUserEmail()).thenReturn(testEmail);

            User user = new User();
            user.setId(50L);
            user.setEmail(testEmail);

            Fanpage fanpage = new Fanpage();
            fanpage.setName("Tech Club");

            Fanpage savedFanpage = new Fanpage();
            savedFanpage.setId(100L);
            savedFanpage.setName("Tech Club");
            savedFanpage.setStatus(FanpageStatusEnum.PENDING.name());

            when(userRepository.findByEmail(testEmail)).thenReturn(user);
            when(fanpageDtoMapper.toFanpage(registerDto)).thenReturn(fanpage);
            when(fanpageRepository.save(fanpage)).thenReturn(savedFanpage);

            assertDoesNotThrow(() -> fanpageCommandService.createFanpage(registerDto));

            verify(fanpageRepository).save(fanpage);
            verify(fanpageMemberRepository).save(any(FanpageMember.class));
        }
    }

    @Test
    @DisplayName("rejectFanpage_shouldSetStatusToRejectedAndStoreReason")
    void rejectFanpage_shouldSetStatusToRejectedAndStoreReason() {
        Fanpage fanpage = new Fanpage();
        fanpage.setId(5L);
        fanpage.setName("Club");
        when(fanpageRepository.findById(5L)).thenReturn(Optional.of(fanpage));

        fanpageCommandService.rejectFanpage(5L, "Missing proof");

        org.junit.jupiter.api.Assertions.assertEquals("REJECTED", fanpage.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("Missing proof", fanpage.getRejectionReason());
        verify(fanpageRepository).save(fanpage);
        verify(applicationEventPublisher).publishEvent(any(com.ddd.domain.event.FanpageRejectedEvent.class));
    }
}
