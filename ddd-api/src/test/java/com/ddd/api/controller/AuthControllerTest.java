package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.common.enums.ErrorCodeEnum;
import com.ddd.api.dto.auth.req.RegisterRequestDto;
import com.ddd.api.exception.GlobalExceptionHandler;
import com.ddd.api.mapper.AuthApiMapper;
import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.application.dto.user.UserDto;
import com.ddd.application.service.AuthService;
import com.ddd.domain.exception.DuplicateResourceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private AuthApiMapper authApiMapper;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private RegisterRequestDto validRegisterRequestDto;
    private UserDto userDto;
    private StudentProfileDto studentProfileDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        validRegisterRequestDto = new RegisterRequestDto(
                "testuser@gmail.com",
                "password123",
                "LOCAL",
                "Van A",
                "Nguyen",
                "SV123456",
                "MALE",
                19,
                1,
                1
        );

        userDto = new UserDto(
                "testuser@gmail.com",
                "password123",
                "LOCAL"
        );

        studentProfileDto = new StudentProfileDto(
                "Van A",
                "Nguyen",
                "SV123456",
                "MALE",
                19,
                1,
                1,
                null
        );
    }

    @Nested
    @DisplayName("Direct Unit Tests for register()")
    class DirectUnitTests {

        @Test
        @DisplayName("Should register successfully when valid request is provided")
        void register_Success() {
            // Arrange
            when(authApiMapper.toUserDto(validRegisterRequestDto)).thenReturn(userDto);
            when(authApiMapper.toUserProfileDto(validRegisterRequestDto)).thenReturn(studentProfileDto);
            doNothing().when(authService).createUser(userDto, studentProfileDto);

            // Act
            BaseResponse<Void> response = authController.register(validRegisterRequestDto);

            // Assert
            assertNotNull(response);
            assertEquals(BaseResponse.SUCCESS_REQUEST_MESSAGE, response.message());
            assertNull(response.data());
            assertNull(response.exceptionCode());

            verify(authApiMapper, times(1)).toUserDto(validRegisterRequestDto);
            verify(authApiMapper, times(1)).toUserProfileDto(validRegisterRequestDto);
            verify(authService, times(1)).createUser(userDto, studentProfileDto);
        }

        @Test
        @DisplayName("Should throw exception when AuthService throws exception")
        void register_WhenAuthServiceThrowsException_ShouldPropagate() {
            // Arrange
            when(authApiMapper.toUserDto(validRegisterRequestDto)).thenReturn(userDto);
            when(authApiMapper.toUserProfileDto(validRegisterRequestDto)).thenReturn(studentProfileDto);
            doThrow(new DuplicateResourceException("Email already exists"))
                    .when(authService).createUser(userDto, studentProfileDto);

            // Act & Assert
            DuplicateResourceException exception = assertThrows(
                    DuplicateResourceException.class,
                    () -> authController.register(validRegisterRequestDto)
            );

            assertEquals("Email already exists", exception.getMessage());
            verify(authService, times(1)).createUser(userDto, studentProfileDto);
        }
    }

    @Nested
    @DisplayName("MockMvc Integration Tests for POST /auth/register")
    class MockMvcTests {

        @Test
        @DisplayName("POST /auth/register - HTTP 200 OK when request is valid")
        void register_Http200_WhenRequestIsValid() throws Exception {
            // Arrange
            when(authApiMapper.toUserDto(any(RegisterRequestDto.class))).thenReturn(userDto);
            when(authApiMapper.toUserProfileDto(any(RegisterRequestDto.class))).thenReturn(studentProfileDto);
            doNothing().when(authService).createUser(userDto, studentProfileDto);

            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(BaseResponse.SUCCESS_REQUEST_MESSAGE))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.exceptionCode").doesNotExist());

            verify(authService, times(1)).createUser(userDto, studentProfileDto);
        }

        @Test
        @DisplayName("POST /auth/register - HTTP 409 Conflict when user already exists")
        void register_Http409_WhenUserAlreadyExists() throws Exception {
            // Arrange
            when(authApiMapper.toUserDto(any(RegisterRequestDto.class))).thenReturn(userDto);
            when(authApiMapper.toUserProfileDto(any(RegisterRequestDto.class))).thenReturn(studentProfileDto);
            doThrow(new DuplicateResourceException("Email already exists"))
                    .when(authService).createUser(userDto, studentProfileDto);

            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Email already exists"))
                    .andExpect(jsonPath("$.exceptionCode").value(ErrorCodeEnum.DUPLICATE_RESOURCE.getCode()));

            verify(authService, times(1)).createUser(userDto, studentProfileDto);
        }

        @Test
        @DisplayName("POST /auth/register - HTTP 400 Bad Request when email is invalid (too short)")
        void register_Http400_WhenEmailIsTooShort() throws Exception {
            // email is "a" which has size < 6
            RegisterRequestDto invalidDto = new RegisterRequestDto(
                    "a",
                    "password123",
                    "LOCAL",
                    "Van A",
                    "Nguyen",
                    "SV123456",
                    "MALE",
                    19,
                    1,
                    1
            );

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.exceptionCode").value(ErrorCodeEnum.VALIDATION_ERROR.getCode()));

            verify(authService, never()).createUser(any(), any());
        }

        @Test
        @DisplayName("POST /auth/register - HTTP 400 Bad Request when password is null or too short")
        void register_Http400_WhenPasswordIsTooShort() throws Exception {
            RegisterRequestDto invalidDto = new RegisterRequestDto(
                    "testuser@gmail.com",
                    "123", // min length is 6
                    "LOCAL",
                    "Van A",
                    "Nguyen",
                    "SV123456",
                    "MALE",
                    19,
                    1,
                    1
            );

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.exceptionCode").value(ErrorCodeEnum.VALIDATION_ERROR.getCode()));

            verify(authService, never()).createUser(any(), any());
        }

        @Test
        @DisplayName("POST /auth/register - HTTP 400 Bad Request when required string fields are blank")
        void register_Http400_WhenRequiredFieldIsBlank() throws Exception {
            RegisterRequestDto invalidDto = new RegisterRequestDto(
                    "testuser@gmail.com",
                    "password123",
                    "", // blank providerId
                    "Van A",
                    "Nguyen",
                    "SV123456",
                    "MALE",
                    19,
                    1,
                    1
            );

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.exceptionCode").value(ErrorCodeEnum.VALIDATION_ERROR.getCode()));

            verify(authService, never()).createUser(any(), any());
        }

        @Test
        @DisplayName("POST /auth/register - HTTP 400 Bad Request when kNumber exceeds maximum (> 100)")
        void register_Http400_WhenKNumberExceedsMax() throws Exception {
            RegisterRequestDto invalidDto = new RegisterRequestDto(
                    "testuser@gmail.com",
                    "password123",
                    "LOCAL",
                    "Van A",
                    "Nguyen",
                    "SV123456",
                    "MALE",
                    150, // max is 100
                    1,
                    1
            );

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.exceptionCode").value(ErrorCodeEnum.VALIDATION_ERROR.getCode()));

            verify(authService, never()).createUser(any(), any());
        }

        @Test
        @DisplayName("POST /auth/register - HTTP 400 Bad Request when number fields are negative or zero")
        void register_Http400_WhenNumberFieldIsNotPositive() throws Exception {
            RegisterRequestDto invalidDto = new RegisterRequestDto(
                    "testuser@gmail.com",
                    "password123",
                    "LOCAL",
                    "Van A",
                    "Nguyen",
                    "SV123456",
                    "MALE",
                    -5, // must be positive
                    1,
                    1
            );

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.exceptionCode").value(ErrorCodeEnum.VALIDATION_ERROR.getCode()));

            verify(authService, never()).createUser(any(), any());
        }

        @Test
        @DisplayName("POST /auth/register - HTTP 400 Bad Request when email is null")
        void register_Http400_WhenEmailIsNull() throws Exception {
            RegisterRequestDto invalidDto = new RegisterRequestDto(
                    null,
                    "password123",
                    "LOCAL",
                    "Van A",
                    "Nguyen",
                    "SV123456",
                    "MALE",
                    19,
                    1,
                    1
            );

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.exceptionCode").value(ErrorCodeEnum.VALIDATION_ERROR.getCode()));

            verify(authService, never()).createUser(any(), any());
        }

        @Test
        @DisplayName("POST /auth/register - HTTP 400 Bad Request when studentId is blank")
        void register_Http400_WhenStudentIdIsBlank() throws Exception {
            RegisterRequestDto invalidDto = new RegisterRequestDto(
                    "testuser@gmail.com",
                    "password123",
                    "LOCAL",
                    "Van A",
                    "Nguyen",
                    "   ", // blank studentId
                    "MALE",
                    19,
                    1,
                    1
            );

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.exceptionCode").value(ErrorCodeEnum.VALIDATION_ERROR.getCode()));

            verify(authService, never()).createUser(any(), any());
        }

        @Test
        @DisplayName("POST /auth/register - HTTP 400 Bad Request when request body is empty or invalid JSON")
        void register_Http400_WhenRequestBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.exceptionCode").value(ErrorCodeEnum.VALIDATION_ERROR.getCode()));

            verify(authService, never()).createUser(any(), any());
        }
    }
}
