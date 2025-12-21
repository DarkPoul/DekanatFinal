package com.esvar.dekanat.mail.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AttachmentDto {
    Long id;
    String messageId;
    String attachmentId;
    String filename;
    Long sizeBytes;
    String mimeType;
}
