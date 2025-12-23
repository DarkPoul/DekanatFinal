package com.esvar.dekanat.mail.v2.controller;

import com.esvar.dekanat.mail.v2.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mail/v2/attachments")
@RequiredArgsConstructor
public class MailAttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long attachmentId) {
        return attachmentService.loadAttachment(attachmentId)
                .map(content -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + content.filename() + "\"")
                        .contentType(MediaType.parseMediaType(content.contentType() != null ? content.contentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                        .body(content.resource()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{attachmentId}/inline")
    public ResponseEntity<Resource> inline(@PathVariable Long attachmentId) {
        return attachmentService.loadInline(attachmentId)
                .map(content -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(content.contentType() != null ? content.contentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                        .body(content.resource()))
                .orElse(ResponseEntity.notFound().build());
    }
}
