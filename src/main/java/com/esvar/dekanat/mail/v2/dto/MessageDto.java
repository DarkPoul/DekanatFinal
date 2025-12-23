package com.esvar.dekanat.mail.v2.dto;

import com.esvar.dekanat.mail.v2.entity.MailAttachmentEntity;
import com.esvar.dekanat.mail.v2.entity.MailMessageEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private Long id;
    private MailMessageEntity.Direction direction;
    private String fromEmail;
    private String toEmail;
    private String subject;
    private Instant sentAt;
    private String bodyHtml;
    private String bodyText;
    @Builder.Default
    private List<MessageAttachmentDto> attachments = new ArrayList<>();
    private String snippet;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageAttachmentDto {
        private Long id;
        private String filename;
        private String contentType;
        private Long size;
        private boolean inline;
        private String contentId;
    }

    public static MessageDto fromEntity(MailMessageEntity entity, List<MailAttachmentEntity> attachments) {
        List<MailAttachmentEntity> safeAttachments = attachments != null ? attachments : Collections.emptyList();
        MessageDto dto = MessageDto.builder()
                .id(entity.getId())
                .direction(entity.getDirection())
                .fromEmail(entity.getFromEmail())
                .toEmail(entity.getToEmail())
                .subject(entity.getSubject())
                .sentAt(entity.getSentAt())
                .bodyHtml(entity.getBodyHtml())
                .bodyText(entity.getBodyText())
                .snippet(entity.getSnippet())
                .build();
        for (MailAttachmentEntity attachment : safeAttachments) {
            dto.attachments.add(MessageAttachmentDto.builder()
                    .id(attachment.getId())
                    .filename(attachment.getFilename())
                    .contentType(attachment.getContentType())
                    .size(attachment.getSize())
                    .inline(attachment.isInline())
                    .contentId(attachment.getContentId())
                    .build());
        }
        return dto;
    }
}
