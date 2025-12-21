package com.esvar.dekanat.mail;

import com.esvar.dekanat.mail.dto.ChatFilter;
import com.esvar.dekanat.mail.dto.ChatListItemDto;
import com.esvar.dekanat.mail.dto.ChatMessageDetailDto;
import com.esvar.dekanat.mail.dto.ChatMessageHeaderDto;
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

import java.io.InputStream;

@RestController
@RequestMapping("/mail")
@RolesAllowed({"ROLE_ADMIN", "ROLE_DEKANAT"})
public class MailController {

    private final ChatService chatService;

    public MailController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chats")
    public Page<ChatListItemDto> getChats(@RequestParam(name = "q", required = false) String query,
                                          @RequestParam(name = "page", defaultValue = "0") int page,
                                          @RequestParam(name = "size", defaultValue = "20") int size) {
        ChatFilter filter = new ChatFilter();
        filter.setQuery(query);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        return chatService.findChats(filter, pageable);
    }

    @GetMapping("/chats/{chatId}/messages")
    public Page<ChatMessageHeaderDto> getMessages(@PathVariable Long chatId,
                                                  @RequestParam(name = "page", defaultValue = "0") int page,
                                                  @RequestParam(name = "size", defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sentAt"));
        return chatService.findMessageHeaders(chatId, pageable);
    }

    @GetMapping("/messages/{messageId}")
    public ChatMessageDetailDto getMessageDetails(@PathVariable Long messageId) throws MessagingException {
        return chatService.getMessageDetails(messageId);
    }

    @PostMapping("/chats/{chatId}/status")
    public void updateStatus(@PathVariable Long chatId, @RequestParam ChatStatus status) {
        chatService.updateStatus(chatId, status);
    }

    @PostMapping("/chats/{chatId}/processed")
    public void markProcessed(@PathVariable Long chatId) {
        chatService.markProcessed(chatId);
    }

    @PostMapping("/chats/{chatId}/reply")
    public void reply(@PathVariable Long chatId, @RequestBody ReplyRequest request) {
        chatService.replyToChat(chatId, request.getBody(), request.getSubject());
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<InputStreamResource> downloadAttachment(@PathVariable Long attachmentId) throws MessagingException {
        InputStream stream = chatService.loadAttachment(attachmentId);
        return buildAttachmentResponse(stream);
    }

    @GetMapping("/messages/{messageId}/attachments/{attachmentId}")
    public ResponseEntity<InputStreamResource> downloadAttachment(@PathVariable String messageId,
                                                                  @PathVariable String attachmentId) throws MessagingException {
        InputStream stream = chatService.loadAttachment(messageId, attachmentId);
        return buildAttachmentResponse(stream);
    }

    private ResponseEntity<InputStreamResource> buildAttachmentResponse(InputStream stream) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(stream));
    }
}
