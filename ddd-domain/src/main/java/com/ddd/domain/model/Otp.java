package com.ddd.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Otp extends BaseModel {

    private String email;
    private String otpCode;
    private Instant expiredAt;
    private Boolean isUsed = false;

}
