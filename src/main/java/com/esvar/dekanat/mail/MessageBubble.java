package com.esvar.dekanat.mail;

import com.esvar.dekanat.mail.dto.AttachmentDto;
import com.esvar.dekanat.mail.dto.ChatMessageDetailDto;
import com.esvar.dekanat.mail.dto.ChatMessageHeaderDto;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class MessageBubble extends Div {

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());

    private final Div bodyWrapper = new Div();
    private final Div attachmentsWrapper = new Div();
    private final MessageDetailLoader detailLoader;

    private ChatMessageHeaderDto header;
    private ChatMessageDetailDto detail;

    public MessageBubble(ChatMessageHeaderDto header, MessageDetailLoader detailLoader) {
        this.header = header;
        this.detailLoader = detailLoader;
        addClassName("message-bubble");
        addClassName(header.getDirection() == MessageDirection.IN ? "incoming" : "outgoing");

        bodyWrapper.addClassName("bubble-body");
        attachmentsWrapper.addClassName("bubble-attachments");

        add(buildHeader(), bodyWrapper, attachmentsWrapper);
        renderBody();
        renderAttachments();

        addClickListener(e -> {
            if (detail == null) {
                loadDetails();
            }
        });
    }

    private Component buildHeader() {
        Span directionBadge = new Span(header.getDirection() == MessageDirection.IN ? "Вхідне" : "Вихідне");
        directionBadge.addClassName("direction-badge");
        directionBadge.addClassName(header.getDirection() == MessageDirection.IN ? "incoming-badge" : "outgoing-badge");

        Span addresses = new Span(shortAddress());
        addresses.addClassName("bubble-address");

        Span time = new Span(header.getSentAt() != null ? timeFormatter.format(header.getSentAt()) : "");
        time.addClassName("message-time");

        HorizontalLayout header = new HorizontalLayout(directionBadge, addresses, time);
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.addClassName("message-header");
        header.setSpacing(true);
        return header;
    }

    private void renderBody() {
        bodyWrapper.removeAll();
        if (detail == null) {
            String previewText = toSafeHtml(Optional.ofNullable(header.getSnippet()).orElse("Натисніть, щоб завантажити"));
            Span preview = new Span();
            preview.addClassName("message-body-html");
            preview.getElement().setProperty("innerHTML", previewText);
            bodyWrapper.add(preview);
            return;
        }

        QuoteCollapsePanel.QuoteExtraction extraction = QuoteCollapsePanel.extract(detail.getBodyText());
        boolean hasQuote = extraction.hasQuote();
        String mainHtml;
        if (!hasQuote && StringUtils.hasText(detail.getBodyHtml())) {
            mainHtml = detail.getBodyHtml();
        } else {
            mainHtml = toSafeHtml(extraction.main());
        }

        Span body = new Span();
        body.addClassName("message-body-html");
        body.getElement().setProperty("innerHTML", mainHtml);
        bodyWrapper.add(body);

        if (hasQuote) {
            bodyWrapper.add(new QuoteCollapsePanel(toSafeHtml(extraction.quote())));
        }
    }

    private void renderAttachments() {
        attachmentsWrapper.removeAll();
        if (detail == null) {
            return;
        }
        List<AttachmentDto> attachments = detail.getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            attachmentsWrapper.add(new AttachmentList(attachments));
        }
    }

    private String shortAddress() {
        if (header.getDirection() == MessageDirection.IN) {
            return "Від: " + Optional.ofNullable(header.getFrom()).orElse("");
        }
        return "Кому: " + Optional.ofNullable(header.getTo()).orElse("");
    }

    private String toSafeHtml(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return HtmlUtils.htmlEscape(text).replace("\n", "<br/>");
    }

    private void loadDetails() {
        if (detailLoader == null) {
            return;
        }
        ChatMessageDetailDto loaded = detailLoader.load(header.getId());
        if (loaded != null) {
            this.detail = loaded;
            renderBody();
            renderAttachments();
        }
    }

    @FunctionalInterface
    public interface MessageDetailLoader {
        ChatMessageDetailDto load(Long messageId);
    }
}
