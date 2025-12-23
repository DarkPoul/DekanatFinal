package com.esvar.dekanat.mail.v2.dto;

import com.esvar.dekanat.mail.v2.entity.MailThreadEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreadListItemDto {
    private Long threadId;
    private Long contactId;
    private String displayName;
    private String email;
    private String orgUnitText;
    private String lastSubject;
    private Instant lastIncomingAt;
    private MailThreadEntity.ThreadStatus status;
    private int unreadIncomingCount;
    private boolean external;
    private boolean signed;
}
