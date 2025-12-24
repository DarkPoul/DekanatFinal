package com.esvar.dekanat.mail.v2.controller;

import com.esvar.dekanat.mail.v2.service.AttachmentService;
import com.esvar.dekanat.utilites.ContentDispositionUtils;
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
                .map(content -> {
                    MediaType mediaType = attachmentService.resolveMediaType(content);
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    ContentDispositionUtils.buildHeaderValue("attachment", content.filename()))
                            .contentType(mediaType)
                            .body(content.resource());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{attachmentId}/inline")
    public ResponseEntity<Resource> inline(@PathVariable Long attachmentId) {
        return attachmentService.loadInline(attachmentId)
                .map(content -> {
                    MediaType mediaType = attachmentService.resolveMediaType(content);
                    return ResponseEntity.ok()
                            .contentType(mediaType)
                            .body(content.resource());
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
