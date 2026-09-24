package com.ddd.api.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCodeEnum {
    // COMMON
    VALIDATION_ERROR("000", HttpStatus.BAD_REQUEST, "Validation error"),
    UNAUTHORIZED_ERROR("001", HttpStatus.UNAUTHORIZED, "Unauthorized error"),
    ACCESS_DENIED_ERROR("002", HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
    NOT_FOUND_ERROR("003", HttpStatus.NOT_FOUND, "Not found error"),
    INTERNAL_SERVER_ERROR("004", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    CONCURRENT_MODIFICATION(
            "005", HttpStatus.CONFLICT, "Concurrent occurs when try to persist the entity"),
    DUPLICATE_RESOURCE("006", HttpStatus.CONFLICT, "Resource already exists"),
    RESOURCE_NOT_FOUND("007", HttpStatus.NOT_FOUND, "Resource not found"),

    // SEMESTER
    SEMESTER_NOT_FOUND("SEMESTER.001", HttpStatus.NOT_FOUND, "Semester not found"),
    SEMESTER_DUPLICATE("SEMESTER.002", HttpStatus.CONFLICT, "Semester already exists"),

    // STUDENT_PROFILE
    STUDENT_PROFILE_NOT_FOUND("STUDENT.001", HttpStatus.NOT_FOUND, "Student profile not found"),
    STUDENT_ID_DUPLICATE("STUDENT.002", HttpStatus.CONFLICT, "Student ID already exists"),
    STUDENT_USER_ALREADY_LINKED("STUDENT.003", HttpStatus.CONFLICT, "User is already linked to another student profile"),

    // ROLE
    ROLES_NOT_FOUND("ROLE.001", HttpStatus.NOT_FOUND, "One or more roles could not be found"),
    ROLE_NOT_FOUND("ROLE.002", HttpStatus.NOT_FOUND, "The role could not be found"),

    // AUTH
    AUTH_LOGIN_FAILED("AUTH.001", HttpStatus.UNAUTHORIZED, "Login failed"),
    AUTH_EMAIL_EXISTS("AUTH.003", HttpStatus.BAD_REQUEST, "Email already exists"),
    AUTH_INVALID_TOKEN("AUTH.004", HttpStatus.BAD_REQUEST, "Invalid token"),
    AUTH_INVALID_CREDENTIALS("AUTH.005", HttpStatus.BAD_REQUEST, "Invalid username or password"),
    AUTH_ACCOUNT_EXISTS_BY_USERNAME(
            "AUTH.006", HttpStatus.BAD_REQUEST, "Account already exists with the given username"),
    AUTH_EXPIRED_REVOKED_TOKEN(
            "AUTH.007", HttpStatus.UNAUTHORIZED, "Expired or revoked refresh token"),
    AUTH_INVALID_PASSWORD_CONFIRM(
            "AUTH.008", HttpStatus.BAD_REQUEST, "Password confirm does not match with new password"),
    AUTH_INVALID_OLD_PASSWORD(
            "AUTH.009", HttpStatus.BAD_REQUEST, "Old password does not match with one in system"),
    AUTH_INACTIVE_USER("AUTH.010", HttpStatus.UNAUTHORIZED, "User is disabled"),
//    AUTH_MISSING_CREDENTIALS(
//            "AUTH.011",
//            HttpStatus.UNAUTHORIZED,
//            "You're missing credential to access this resource because full authentication is required"),
    AUTH_SOCIAL_PROVIDER_NOT_LINKED(
            "AUTH.012", HttpStatus.NOT_FOUND, "Social provider is not linked to this account"),
    AUTH_UNAUTHORIZED_ROLE(
            "AUTH.013", HttpStatus.FORBIDDEN, "Your role is not authorized to access the CMS"),
    AUTH_ACCOUNT_EXISTS_BY_MOBILE(
            "AUTH.014", HttpStatus.BAD_REQUEST, "Account already exists with the given mobile number"),
    AUTH_ACCOUNT_EXISTS_BY_EMAIL(
            "AUTH.015", HttpStatus.BAD_REQUEST, "Account already exists with the given email address");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;

    public static ErrorCode of(String code, HttpStatus status, String message) {
        return new ErrorCode() {
            @Override
            public String getCode() {
                return code;
            }

            @Override
            public HttpStatus getStatus() {
                return status;
            }

            @Override
            public String getDefaultMessage() {
                return message;
            }
        };
    }

    public ErrorCode withFormattedMessage(Object... args) {
        return ErrorCodeEnum.of(code, status, String.format(defaultMessage, args));
    }
}
