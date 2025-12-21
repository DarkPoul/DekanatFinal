package com.esvar.dekanat.mail;

import com.esvar.dekanat.mail.dto.AttachmentDto;
import com.esvar.dekanat.mail.dto.ChatMessageDetailDto;
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
    private final ChatMessageDetailDto detail;

    public MessageBubble(ChatMessageDetailDto detail) {
        this.detail = detail;
        addClassName("message-bubble");
        addClassName(detail.getDirection() == MessageDirection.IN ? "incoming" : "outgoing");

        bodyWrapper.addClassName("bubble-body");
        attachmentsWrapper.addClassName("bubble-attachments");

        add(buildHeader(), bodyWrapper, attachmentsWrapper);
        renderBody();
        renderAttachments();
    }

    private Component buildHeader() {
        Span directionBadge = new Span(detail.getDirection() == MessageDirection.IN ? "Вхідне" : "Вихідне");
        directionBadge.addClassName("direction-badge");
        directionBadge.addClassName(detail.getDirection() == MessageDirection.IN ? "incoming-badge" : "outgoing-badge");

        Span addresses = new Span(shortAddress());
        addresses.addClassName("bubble-address");

        Span time = new Span(detail.getSentAt() != null ? timeFormatter.format(detail.getSentAt()) : "");
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
        String mainHtml = detail.getBodyHtmlClean();
        if (!StringUtils.hasText(mainHtml)) {
            mainHtml = detail.getBodyHtml();
        }
        if (!StringUtils.hasText(mainHtml)) {
            mainHtml = toSafeHtml(Optional.ofNullable(detail.getBodyTextClean()).orElse(detail.getBodyText()));
        }

        Span body = new Span();
        body.addClassName("message-body-html");
        body.getElement().setProperty("innerHTML", mainHtml);
        bodyWrapper.add(body);
    }

    private void renderAttachments() {
        attachmentsWrapper.removeAll();
        List<AttachmentDto> attachments = detail.getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            attachmentsWrapper.add(new AttachmentList(attachments));
        }
    }

    private String shortAddress() {
        if (detail.getDirection() == MessageDirection.IN) {
            return "Від: " + Optional.ofNullable(detail.getFrom()).orElse("");
        }
        return "Кому: " + Optional.ofNullable(detail.getTo()).orElse("");
    }

    private String toSafeHtml(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return HtmlUtils.htmlEscape(text).replace("\n", "<br/>");
    }
}
