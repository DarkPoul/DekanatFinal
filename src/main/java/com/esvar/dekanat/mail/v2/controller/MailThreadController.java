package com.esvar.dekanat.mail.v2.controller;

import com.esvar.dekanat.mail.v2.dto.SignContactRequest;
import com.esvar.dekanat.mail.v2.dto.ThreadListItemDto;
import com.esvar.dekanat.mail.v2.dto.ThreadStatusUpdateRequest;
import com.esvar.dekanat.mail.v2.entity.MailThreadEntity;
import com.esvar.dekanat.mail.v2.service.ThreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mail/v2")
@RequiredArgsConstructor
public class MailThreadController {

    private final ThreadService threadService;

    @GetMapping("/threads")
    public Page<ThreadListItemDto> listThreads(@RequestParam(name = "nameQuery", required = false) String nameQuery,
                                               @RequestParam(name = "emailQuery", required = false) String emailQuery,
                                               @RequestParam(name = "orgQuery", required = false) String orgQuery,
                                               @RequestParam(name = "status", required = false) MailThreadEntity.ThreadStatus status,
                                               @RequestParam(name = "offset", defaultValue = "0") int offset,
                                               @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return threadService.findThreads(nameQuery, emailQuery, orgQuery, status, offset, limit);
    }

    @PostMapping("/threads/{threadId}/status")
    public void updateStatus(@PathVariable Long threadId, @RequestBody ThreadStatusUpdateRequest request) {
        threadService.updateStatus(threadId, request.getStatus());
    }

    @PostMapping("/contacts/{contactId}/sign")
    public void signContact(@PathVariable Long contactId, @RequestBody SignContactRequest request) {
        threadService.signContact(contactId, request.getDisplayName(), request.getOrgUnitText());
    }

    @PostMapping("/threads/{threadId}/viewed")
    public void markViewed(@PathVariable Long threadId) {
        threadService.markViewed(threadId);
    }
}
