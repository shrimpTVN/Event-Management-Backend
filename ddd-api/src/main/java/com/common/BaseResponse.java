package com.common;

import java.util.Collections;
import lombok.*;

public record BaseResponse<T>(T data, String message, String exceptionCode) {
    public static final String SUCCESS_REQUEST_MESSAGE = "Success";

    public static <T> BaseResponse<T> of(T data) {
        return new BaseResponse<>(data, SUCCESS_REQUEST_MESSAGE, null);
    }

    public static <T> BaseResponse<T> ok() {
        return new BaseResponse<>(null, SUCCESS_REQUEST_MESSAGE, null);
    }

    public static <T> BaseResponse<T> error(String exceptionCode, String exceptionMessage) {
        return new BaseResponse<>(null, exceptionMessage, exceptionCode);
    }

    @SuppressWarnings("unchecked")
    public static <T> BaseResponse<T> empty() {
        return new BaseResponse<>((T) Collections.emptyList(), SUCCESS_REQUEST_MESSAGE, null);
    }
}
