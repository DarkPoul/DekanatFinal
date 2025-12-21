package com.esvar.dekanat.mail;

import com.esvar.dekanat.mail.dto.ChatFilter;
import com.esvar.dekanat.mail.dto.ChatListItemDto;
import com.esvar.dekanat.mail.dto.ChatMessageDetailDto;
import com.esvar.dekanat.mail.dto.ReplyRequest;
import jakarta.mail.MessagingException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/mail")
@RolesAllowed({"ROLE_ADMIN", "ROLE_DEKANAT"})
public class MailController {

    private final ChatService chatService;

    public MailController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/threads")
    public Page<ChatListItemDto> getChats(@RequestParam(name = "q", required = false) String query,
                                          @RequestParam(name = "page", defaultValue = "0") int page,
                                          @RequestParam(name = "size", defaultValue = "20") int size) {
        ChatFilter filter = new ChatFilter();
        filter.setQuery(query);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        return chatService.findChats(filter, pageable);
    }

    @GetMapping("/threads/{threadKey}/messages")
    public List<ChatMessageDetailDto> getThreadMessages(@PathVariable String threadKey,
                                                        @RequestParam(name = "limit", defaultValue = "30") int limit,
                                                        @RequestParam(name = "before", required = false) Instant before) throws MessagingException {
        return chatService.findThreadMessages(threadKey, limit, before);
    }

    @GetMapping("/messages/{messageId}")
    public ChatMessageDetailDto getMessageDetails(@PathVariable Long messageId) throws MessagingException {
        return chatService.getMessageDetails(messageId);
    }

    @PostMapping("/threads/{chatId}/status")
    public void updateStatus(@PathVariable Long chatId, @RequestParam ChatStatus status) {
        chatService.updateStatus(chatId, status);
    }

    @PostMapping("/threads/{chatId}/processed")
    public void markProcessed(@PathVariable Long chatId) {
        chatService.markProcessed(chatId);
    }

    @PostMapping("/threads/{chatId}/reply")
    public void reply(@PathVariable Long chatId, @RequestBody ReplyRequest request) {
        chatService.replyToChat(chatId, request.getBody(), request.getSubject());
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<InputStreamResource> downloadAttachment(@PathVariable Long attachmentId) throws MessagingException {
        ChatService.AttachmentContent content = chatService.loadAttachment(attachmentId);
        return buildAttachmentResponse(content);
    }

    @GetMapping("/messages/{messageId}/attachments/{attachmentId}")
    public ResponseEntity<InputStreamResource> downloadAttachment(@PathVariable String messageId,
                                                                  @PathVariable String attachmentId) throws MessagingException {
        ChatService.AttachmentContent content = chatService.loadAttachment(messageId, attachmentId);
        return buildAttachmentResponse(content);
    }

    @GetMapping("/messages/{messageId}/inline/{cid}")
    public ResponseEntity<InputStreamResource> loadInline(@PathVariable Long messageId,
                                                          @PathVariable String cid) throws MessagingException {
        ChatService.AttachmentContent content = chatService.loadInline(messageId, cid);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .body(new InputStreamResource(content.stream()));
    }

    private ResponseEntity<InputStreamResource> buildAttachmentResponse(ChatService.AttachmentContent content) {
        String disposition = "attachment";
        if (content.filename() != null) {
            disposition += "; filename=\"" + content.filename() + "\"";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(content.contentType() != null ? content.contentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(new InputStreamResource(content.stream()));
    }
}
