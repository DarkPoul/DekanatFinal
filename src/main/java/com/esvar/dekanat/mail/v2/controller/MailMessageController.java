package com.esvar.dekanat.mail.v2.controller;

import com.esvar.dekanat.mail.v2.dto.MessageDto;
import com.esvar.dekanat.mail.v2.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/mail/v2")
@RequiredArgsConstructor
public class MailMessageController {

    private final MessageService messageService;

    @GetMapping("/threads/{threadId}/messages")
    public List<MessageDto> loadMessages(@PathVariable Long threadId,
                                         @RequestParam(name = "before", required = false) Instant before,
                                         @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return messageService.loadMessages(threadId, before, limit);
    }
}
