package com.esvar.dekanat.mail.v2.controller;

import com.esvar.dekanat.mail.v2.entity.MailThreadEntity;
import com.esvar.dekanat.mail.v2.service.SendMailService;
import com.esvar.dekanat.mail.v2.service.ThreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/mail/v2")
@RequiredArgsConstructor
public class SendMailController {

    private final ThreadService threadService;
    private final SendMailService sendMailService;

    @PostMapping("/threads/{threadId}/send")
    public void send(@PathVariable Long threadId,
                     @RequestParam(name = "text", required = false) String text,
                     @RequestParam(name = "subject", required = false) String subject,
                     @RequestParam(name = "files", required = false) List<MultipartFile> files) {
        MailThreadEntity thread = threadService.findById(threadId).orElseThrow();
        sendMailService.send(thread, text, subject, files);
    }
}
