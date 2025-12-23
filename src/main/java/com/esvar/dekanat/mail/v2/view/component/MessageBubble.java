package com.esvar.dekanat.mail.v2.view.component;

import com.esvar.dekanat.mail.v2.dto.MessageDto;
import com.esvar.dekanat.mail.v2.entity.MailMessageEntity;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.owasp.html.HtmlSanitizer;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        List<MessageDto.MessageAttachmentDto> attachments = Objects.requireNonNullElseGet(message.getAttachments(), List::of);
        List<MessageDto.MessageAttachmentDto> inlineAttachments = attachments.stream()
                .filter(MessageDto.MessageAttachmentDto::isInline)
                .toList();

        InlineBody inlineBody = sanitizeBodyHtml(message.getBodyHtml(), inlineAttachments);

        if (StringUtils.hasText(inlineBody.html())) {
            body.getElement().setProperty("innerHTML", inlineBody.html());
        } else if (StringUtils.hasText(message.getBodyText())) {
            body.setText(message.getBodyText());
        } else if (StringUtils.hasText(message.getSnippet())) {
            body.setText(message.getSnippet());
        } else {
            body.setText("Без вмісту");
        }

        appendInlineImages(body, inlineAttachments, inlineBody.resolvedInlineIds());

        add(meta, body);

        List<MessageDto.MessageAttachmentDto> downloadableAttachments = attachments.stream()
                .filter(attachment -> !attachment.isInline())
                .toList();
        if (!downloadableAttachments.isEmpty()) {
            Div attachmentBlock = new Div();
            attachmentBlock.addClassName("attachments");
            for (MessageDto.MessageAttachmentDto attachment : downloadableAttachments) {
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

    private InlineBody sanitizeBodyHtml(String bodyHtml, List<MessageDto.MessageAttachmentDto> inlineAttachments) {
        if (!StringUtils.hasText(bodyHtml)) {
            return new InlineBody(null, Set.of());
        }

        Document document = Jsoup.parse(bodyHtml);
        Set<Long> resolvedInlineIds = new HashSet<>();

        if (!inlineAttachments.isEmpty()) {
            var inlineByContentId = inlineAttachments.stream()
                    .filter(attachment -> attachment.getId() != null)
                    .filter(attachment -> StringUtils.hasText(attachment.getContentId()))
                    .collect(Collectors.toMap(attachment -> attachment.getContentId().toLowerCase(Locale.ROOT), Function.identity(), (first, duplicate) -> first));

            for (Element imageElement : document.select("img[src^=cid:]")) {
                String cid = imageElement.attr("src").substring(4).trim();
                MessageDto.MessageAttachmentDto attachment = inlineByContentId.get(cid.toLowerCase(Locale.ROOT));
                if (attachment != null) {
                    imageElement.attr("src", "/api/mail/v2/attachments/" + attachment.getId() + "/inline");
                    resolvedInlineIds.add(attachment.getId());
                }
            }
        }

        String sanitized = MAIL_HTML_POLICY.sanitize(document.body().html());
        return new InlineBody(sanitized, resolvedInlineIds);
    }

    private void appendInlineImages(Div body, List<MessageDto.MessageAttachmentDto> inlineAttachments, Set<Long> alreadyInlined) {
        for (MessageDto.MessageAttachmentDto attachment : inlineAttachments) {
            if (attachment.getId() == null || alreadyInlined.contains(attachment.getId())) {
                continue;
            }
            Image image = new Image("/api/mail/v2/attachments/" + attachment.getId() + "/inline", attachment.getFilename());
            image.addClassName("inline-image");
            body.add(image);
        }
    }

    private record InlineBody(String html, Set<Long> resolvedInlineIds) {
    }

}
