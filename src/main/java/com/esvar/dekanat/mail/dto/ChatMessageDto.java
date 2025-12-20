package com.esvar.dekanat.mail.dto;

import com.esvar.dekanat.mail.MessageDirection;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class ChatMessageDto {
    Long id;
    String messageId;
    String from;
    String to;
    String subject;
    String bodyHtml;
    String bodyText;
    Instant sentAt;
    MessageDirection direction;
    boolean hasAttachments;
    List<AttachmentDto> attachments;
}
