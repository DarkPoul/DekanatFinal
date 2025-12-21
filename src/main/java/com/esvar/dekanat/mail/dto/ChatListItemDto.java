package com.esvar.dekanat.mail.dto;

import com.esvar.dekanat.mail.ChatStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ChatListItemDto {
    Long id;
    String threadKey;
    String title;
    String displayName;
    String peerEmail;
    String orgUnit;
    ChatStatus status;
    boolean hasUnprocessed;
    int unreadCount;
    Instant lastMessageAt;
    String lastSnippet;
    boolean hasAttachments;
}
