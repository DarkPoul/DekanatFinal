package com.esvar.dekanat.mail.dto;

import com.esvar.dekanat.mail.MessageDirection;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ChatMessageHeaderDto {
    Long id;
    String messageId;
    String from;
    String to;
    String subject;
    String snippet;
    Instant sentAt;
    MessageDirection direction;
    boolean hasAttachments;
}
