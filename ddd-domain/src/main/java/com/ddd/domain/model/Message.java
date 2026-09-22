package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class Message extends BaseModel {
    private Long id;
    private Long conversationId;
    private String senderType;
    private Long senderId;
    private String content;
    private String status;
}
