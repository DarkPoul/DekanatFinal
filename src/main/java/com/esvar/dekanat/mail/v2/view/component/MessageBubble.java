package com.esvar.dekanat.mail.v2.view.component;

import com.esvar.dekanat.mail.v2.dto.MessageDto;
import com.esvar.dekanat.mail.v2.entity.MailMessageEntity;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;

import org.owasp.html.HtmlSanitizer;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MessageBubble extends Div {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM HH:mm")
            .withZone(ZoneId.systemDefault());

    public MessageBubble(MessageDto message) {
        addClassName("message-bubble");
        addClassName(message.getDirection() == MailMessageEntity.Direction.IN ? "incoming" : "outgoing");

        Span meta = new Span(buildMeta(message));
        meta.addClassName("message-meta");

        Div body = new Div();
        body.addClassName("message-body");
        if (StringUtils.hasText(message.getBodyHtml())) {
            String safeHtml = MAIL_HTML_POLICY.sanitize(message.getBodyHtml());
            body.getElement().setProperty("innerHTML", safeHtml);
        } else if (StringUtils.hasText(message.getBodyText())) {
            body.setText(message.getBodyText());
        }

        add(meta, body);

        List<MessageDto.MessageAttachmentDto> attachments = message.getAttachments();
        if (!attachments.isEmpty()) {
            Div attachmentBlock = new Div();
            attachmentBlock.addClassName("attachments");
            for (MessageDto.MessageAttachmentDto attachment : attachments) {
                if (attachment.isInline()) {
                    Image image = new Image("/api/mail/v2/attachments/" + attachment.getId() + "/inline", attachment.getFilename());
                    image.addClassName("inline-image");
                    attachmentBlock.add(image);
                } else {
                    String size = humanReadableSize(attachment.getSize());
                    String label = attachment.getFilename();
                    if (StringUtils.hasText(size)) {
                        label = label + " · " + size;
                    }
                    Anchor link = new Anchor("/api/mail/v2/attachments/" + attachment.getId() + "/download", label);
                    link.setTarget("_blank");
                    link.getElement().setAttribute("download", true);
                    link.addClassName("attachment-link");
                    attachmentBlock.add(link);
                }
            }
            add(attachmentBlock);
        }
    }

    private String buildMeta(MessageDto message) {
        StringBuilder meta = new StringBuilder();
        if (message.getSentAt() != null) {
            meta.append(FORMATTER.format(message.getSentAt()));
        }
        if (StringUtils.hasText(message.getSubject())) {
            meta.append(" • ").append(message.getSubject());
        }
        if (StringUtils.hasText(message.getFromEmail())) {
            meta.append(" • ").append(message.getFromEmail());
        }
        return meta.toString();
    }

    private String humanReadableSize(Long size) {
        if (size == null) {
            return "";
        }
        double bytes = size.doubleValue();
        String[] units = {"Б", "КБ", "МБ", "ГБ"};
        int unitIndex = 0;
        while (bytes >= 1024 && unitIndex < units.length - 1) {
            bytes /= 1024;
            unitIndex++;
        }
        return String.format("%.1f %s", bytes, units[unitIndex]);
    }

    private static final PolicyFactory MAIL_HTML_POLICY =
            Sanitizers.FORMATTING
                    .and(Sanitizers.BLOCKS)
                    .and(Sanitizers.LINKS)
                    .and(Sanitizers.IMAGES)
                    .and(Sanitizers.STYLES);

}
