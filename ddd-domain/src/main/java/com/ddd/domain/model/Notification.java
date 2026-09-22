package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class Notification extends BaseModel {
    private String title;
    private String message;
    private String status;
    private String iconUrl;
    private String url;
    private Long userId;
}
