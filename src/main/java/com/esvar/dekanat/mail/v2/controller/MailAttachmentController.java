package com.esvar.dekanat.mail.v2.controller;

import com.esvar.dekanat.mail.v2.service.AttachmentService;
import com.esvar.dekanat.utilites.ContentDispositionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class MailAttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long attachmentId) {
        log.debug("Received request to download attachment {}", attachmentId);
        return attachmentService.loadAttachment(attachmentId)
                .map(content -> {
                    MediaType mediaType = attachmentService.resolveMediaType(content);
                    log.debug("Attachment {} resolved for download with mediaType={} and filename={}",
                            attachmentId, mediaType, content.filename());
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    ContentDispositionUtils.buildHeaderValue("attachment", content.filename()))
                            .contentType(mediaType)
                            .body(content.resource());
                })
                .orElseGet(() -> {
                    log.warn("Attachment {} not found for download", attachmentId);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/{attachmentId}/inline")
    public ResponseEntity<Resource> inline(@PathVariable Long attachmentId) {
        log.debug("Received request to render attachment {} inline", attachmentId);
        return attachmentService.loadInline(attachmentId)
                .map(content -> {
                    MediaType mediaType = attachmentService.resolveMediaType(content);
                    log.debug("Attachment {} resolved for inline rendering with mediaType={} and filename={}",
                            attachmentId, mediaType, content.filename());
                    return ResponseEntity.ok()
                            .contentType(mediaType)
                            .body(content.resource());
                })
                .orElseGet(() -> {
                    log.warn("Attachment {} not found or not renderable inline", attachmentId);
                    return ResponseEntity.notFound().build();
                });
    }
}
