package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class Otp extends BaseModel {

    private String email;
    private String otpCode;
    private Instant expiredAt;
    private Boolean isUsed = false;

}
