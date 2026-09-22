package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Report extends BaseModel {
    private String subject;
    private String message;
    private String status;
    private Long userId;
    private Long fanpageId;
}
