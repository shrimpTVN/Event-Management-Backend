package com.ddd.application.service.impl;

import com.ddd.application.dto.auth.GoogleUserInfo;
import com.ddd.application.dto.auth.LoginDto;
import com.ddd.application.dto.auth.LoginResult;
import com.ddd.application.dto.auth.TokenRefreshResultDto;
import com.ddd.application.dto.user.FanpageAdminProfileDto;
import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.application.dto.user.UserDto;
import com.ddd.application.service.userprofile.FanpageAdminProfileCommandService;
import com.ddd.application.service.userprofile.StudentProfileCommandService;
import com.ddd.application.service.user.UserCommandService;
import com.ddd.application.service.AuthService;
import com.ddd.domain.enums.RoleEnum;
import com.ddd.domain.exception.AuthException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.RefreshToken;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.RefreshTokenRepository;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.config.security.custom.UserDetailsCustom;
import com.ddd.application.service.googleTokenVerifier.GoogleTokenVerifier;
import com.ddd.infrastructure.utils.JwtUtil;
import com.ddd.infrastructure.utils.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserCommandService userCommandService;
    private final StudentProfileCommandService studentProfileCommandService;
    private final FanpageAdminProfileCommandService fanpageAdminProfileCommandService;
    private final JwtUtil jwtUtil;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashUtil tokenHashUtil;
    private final UserRepository userRepository;

    private String createAndSaveRefreshToken(Long userId) {
        String rawToken = UUID.randomUUID().toString();
        // Hash the token before saving it to the database for security and use it as refresh token
        String hashed = tokenHashUtil.hash(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(hashed)
                .userId(userId)
                .expiresAt(Instant.now().plusMillis(jwtUtil.getRefreshExpirationMs()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Override
    @Transactional
    public LoginResult login(String email, String password) {
        var resultAuthentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));

        String jwtToken = jwtUtil.generateJwtToken(resultAuthentication);

        var fetchedUser = (UserDetailsCustom) resultAuthentication.getPrincipal();

        if (fetchedUser == null) {
            throw new BadCredentialsException("Invalid email or password");
        }

        log.info("User:{} logged in successfully", email);

        LoginDto loginDto = new LoginDto(
                fetchedUser.getUserId(),
                fetchedUser.getUsername(),
                fetchedUser.getRoles()
        );

        //generate refresh token and save to database
        String rawRefreshToken = createAndSaveRefreshToken(fetchedUser.getUserId());

        return new LoginResult(loginDto, jwtToken, rawRefreshToken);
    }

    @Transactional
    @Override
    public LoginResult loginWithGoogle(String idToken) {
        // 1. Verify Google ID Token and extract user info
        GoogleUserInfo googleUserInfo = googleTokenVerifier.verifyToken(idToken);
        String email = googleUserInfo.email();
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        if (user.getProviderId() == null || user.getProviderId().isBlank()) {
            user.setProviderId(googleUserInfo.googleId());
            userRepository.update(user);
        }

        log.info("Google user:{} logged in successfully", email);
        String jwtToken = jwtUtil.generateJwtTokenFromUser(user);
        String rawRefreshToken = createAndSaveRefreshToken(user.getId());
        LoginDto loginDto = new LoginDto(
                user.getId(),
                user.getEmail(),
                user.getRoleNames()
        );
        return new LoginResult(loginDto, jwtToken, rawRefreshToken);
    }

    @Transactional
    @Override
    public void createUser(UserDto userDto, StudentProfileDto studentProfileDto) {
        Long userId = userCommandService.createUser(userDto, RoleEnum.STUDENT.name());
        studentProfileCommandService.createStudentProfile(studentProfileDto, userId);
        log.info("Student:{} created successfully", userDto.email());
    }

    @Transactional
    @Override
    public void createFanpageAdmin(UserDto userDto, FanpageAdminProfileDto fanpageAdminProfileDto) {
        Long userId = userCommandService.createUser(userDto, RoleEnum.FANPAGE_ADMIN.name());
        fanpageAdminProfileCommandService.createFanpageAdminProfile(fanpageAdminProfileDto, userId);
        log.info("Fanpage Admin:{} created successfully", userDto.email());
    }

    @Override
    @Transactional
    public void changePassword(String oldPassword, String newPassword, String email) {
        userCommandService.changePassword(oldPassword, newPassword, email);
        User user = userRepository.findByEmail(email);
        if (user != null) {
            refreshTokenRepository.revokeAllByUserId(user.getId());
        }
        log.info("User:{} changed password successfully", email);
    }

    @Override
    @Transactional
    public TokenRefreshResultDto refreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AuthException("Refresh token is missing or empty");
        }

        String hashed = tokenHashUtil.hash(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByToken(hashed)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (!stored.isValid()) {
            // Compromise detection: if an invalid or expired token is submitted, revoke all sessions
            refreshTokenRepository.revokeAllByUserId(stored.getUserId());
            throw new AuthException("Expired or revoked refresh token");
        }

        // Token Rotation: invalidate current token, issue fresh one
        refreshTokenRepository.deleteByToken(hashed);
        String newRawToken = createAndSaveRefreshToken(stored.getUserId());

        // Reconstruct user to generate new access token
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + stored.getUserId()));

        String newAccessToken = jwtUtil.generateJwtTokenFromUser(user);

        return new TokenRefreshResultDto(newAccessToken, newRawToken);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String hashed = tokenHashUtil.hash(rawRefreshToken);
            refreshTokenRepository.deleteByToken(hashed);
        }
    }

    @Override
    public ResponseCookie getUserCookie(String jwtToken) {
        return ResponseCookie.from(jwtUtil.getCookieName(), jwtToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtUtil.getExpirationMs() / 1000)
                .build();
    }

    @Override
    public ResponseCookie getLogoutCookie() {
        return ResponseCookie.from(jwtUtil.getCookieName(), "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    @Override
    public ResponseCookie getRefreshCookie(String rawRefreshToken) {
        return jwtUtil.buildRefreshCookie(rawRefreshToken, jwtUtil.getRefreshExpirationMs() / 1000);
    }

    @Override
    public ResponseCookie getClearRefreshCookie() {
        return jwtUtil.buildClearRefreshCookie();
    }

    @Override
    public String extractRefreshToken(jakarta.servlet.http.HttpServletRequest request) {
        return jwtUtil.extractRefreshToken(request);
    }

}
