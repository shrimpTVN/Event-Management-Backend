package com.common.enums;

import java.io.Serializable;
import org.springframework.http.HttpStatus;

public interface ErrorCode extends Serializable {
    String getCode();

    HttpStatus getStatus();

    String getDefaultMessage();
}