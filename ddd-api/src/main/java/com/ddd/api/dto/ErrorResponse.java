package com.ddd.api.dto;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ErrorResponse(String apiPath, HttpStatus httpStatus,
                               String errorMessage, LocalDateTime errorTime) {
}
