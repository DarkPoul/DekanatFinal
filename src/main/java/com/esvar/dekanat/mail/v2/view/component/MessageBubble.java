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

    private static final String ATTACHMENT_API_URL = "/api/mail/v2/attachments/%d/inline";


    public MessageBubble(MessageDto message) {
        addClassName("message-bubble");
        addClassName(message.getDirection() == MailMessageEntity.Direction.IN ? "incoming" : "outgoing");

        Span meta = new Span(buildMeta(message));
        meta.addClassName("message-meta");

        Div body = new Div();
        body.addClassName("message-body");
        List<MessageDto.MessageAttachmentDto> attachments = Objects.requireNonNullElseGet(message.getAttachments(), List::of);
        List<MessageDto.MessageAttachmentDto> inlineAttachments = attachments.stream()
                .filter(attachment -> StringUtils.hasText(attachment.getContentId()))
                .toList();
        Set<Long> inlineAttachmentIds = inlineAttachments.stream()
                .map(MessageDto.MessageAttachmentDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        InlineBody inlineBody = sanitizeBodyHtml(message.getBodyHtml(), inlineAttachments);
        Set<Long> resolvedInlineIds = inlineBody.resolvedInlineIds();

        if (StringUtils.hasText(inlineBody.html())) {
            body.getElement().setProperty("innerHTML", inlineBody.html());
        } else if (StringUtils.hasText(message.getBodyText())) {
            body.setText(message.getBodyText());
        } else if (StringUtils.hasText(message.getSnippet())) {
            body.setText(message.getSnippet());
        } else {
            body.setText("Без вмісту");
        }

        appendInlineImages(body, inlineAttachments, resolvedInlineIds);

        add(meta, body);

        List<MessageDto.MessageAttachmentDto> downloadableAttachments = attachments.stream()
                .filter(attachment -> attachment.getId() != null)
                .filter(attachment -> !resolvedInlineIds.contains(attachment.getId()))
                .filter(attachment -> !inlineAttachmentIds.contains(attachment.getId()))
                .toList();
        List<MessageDto.MessageAttachmentDto> imageAttachments = downloadableAttachments.stream()
                .filter(this::isImageAttachment)
                .toList();
        if (!imageAttachments.isEmpty()) {
            add(buildImageGallery(imageAttachments));
        }

        List<MessageDto.MessageAttachmentDto> downloadableFiles = downloadableAttachments.stream()
                .filter(attachment -> !isImageAttachment(attachment))
                .toList();
        addDownloadableAttachments(downloadableFiles);
    }

    private void addDownloadableAttachments(List<MessageDto.MessageAttachmentDto> attachments) {
        if (attachments.isEmpty()) {
            return;
        }
        Div attachmentBlock = new Div();
        attachmentBlock.addClassName("attachments");
        for (MessageDto.MessageAttachmentDto attachment : attachments) {
            String label = buildAttachmentLabel(attachment);
            Anchor link = new Anchor("/api/mail/v2/attachments/" + attachment.getId() + "/download", label);
            link.setTarget("_blank");
            link.getElement().setAttribute("download", true);
            link.addClassName("attachment-link");
            attachmentBlock.add(link);
        }
        add(attachmentBlock);
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

            for (Element imageElement : document.select("img[src]")) {
                String originalSrc = imageElement.attr("src");
                if (!StringUtils.hasText(originalSrc)) {
                    continue;
                }
                String normalizedSrc = originalSrc.trim();
                if (!normalizedSrc.toLowerCase(Locale.ROOT).startsWith("cid:")) {
                    continue;
                }
                String cid = normalizedSrc.substring(4).trim();
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
        List<MessageDto.MessageAttachmentDto> remainingInline = inlineAttachments.stream()
                .filter(attachment -> attachment.getId() != null)
                .filter(attachment -> !alreadyInlined.contains(attachment.getId()))
                .filter(this::isImageAttachment) // Перевірка, що це зображення
                .toList();

        if (remainingInline.isEmpty()) {
            return;
        }

        Div inlineImages = new Div();
        inlineImages.addClassName("inline-images");
        for (MessageDto.MessageAttachmentDto attachment : remainingInline) {
            Image image = createImageComponent(attachment);
            // Використовуємо конструктор без тексту, оскільки додаємо компонент Image
            Anchor link = new Anchor(image.getSrc());
            link.setTarget("_blank");
            link.getElement().setAttribute("rel", "noopener noreferrer");
            // Додаємо опис для доступності (accessibility)
            String label = String.format("Відкрити зображення %s (%s)",
                    attachment.getFilename(),
                    humanReadableSize(attachment.getSize()));
            link.getElement().setAttribute("aria-label", label);
            link.addClassName("inline-image-link");
            link.add(image);
            inlineImages.add(link);
        }

        body.add(inlineImages);
    }

    private Div buildImageGallery(List<MessageDto.MessageAttachmentDto> imageAttachments) {
        Div gallery = new Div();
        gallery.addClassName("image-gallery");
        for (MessageDto.MessageAttachmentDto attachment : imageAttachments) {
            if (attachment.getId() == null) continue;

            Div card = new Div();
            card.addClassName("image-card");

            String url = String.format(ATTACHMENT_API_URL, attachment.getId());
            // Використовуємо конструктор Anchor(String href), оскільки текст не потрібен (всередині буде Image)
            Anchor preview = new Anchor(url);
            preview.setTarget("_blank");
            // Додаємо безпекові атрибути для target="_blank"
            preview.getElement().setAttribute("rel", "noopener noreferrer");
            preview.addClassName("image-link");
            preview.add(createImageComponent(attachment));

            Span caption = new Span(buildAttachmentLabel(attachment));
            caption.addClassName("image-caption");

            card.add(preview, caption);
            gallery.add(card);
        }
        return gallery;
    }


    private Image createImageComponent(MessageDto.MessageAttachmentDto attachment) {
        String alt = StringUtils.hasText(attachment.getFilename()) ? attachment.getFilename() : "Вкладення";
        Image image = new Image("/api/mail/v2/attachments/" + attachment.getId() + "/inline", alt);
        image.addClassName("inline-image");
        image.getElement().setAttribute("loading", "lazy");
        return image;
    }

    private String buildAttachmentLabel(MessageDto.MessageAttachmentDto attachment) {
        String size = humanReadableSize(attachment.getSize());
        String label = StringUtils.hasText(attachment.getFilename()) ? attachment.getFilename() : "Вкладення";
        if (StringUtils.hasText(size)) {
            label = label + " · " + size;
        }
        return label;
    }

    private boolean isImageAttachment(MessageDto.MessageAttachmentDto attachment) {
        if (!StringUtils.hasText(attachment.getContentType()) && !StringUtils.hasText(attachment.getFilename())) {
            return false;
        }
        String contentType = attachment.getContentType();
        if (StringUtils.hasText(contentType) && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        String filename = attachment.getFilename();
        if (!StringUtils.hasText(filename)) {
            return false;
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".gif")
                || lower.endsWith(".bmp")
                || lower.endsWith(".webp")
                || lower.endsWith(".heic")
                || lower.endsWith(".heif");
    }

    private record InlineBody(String html, Set<Long> resolvedInlineIds) {
    }

}
