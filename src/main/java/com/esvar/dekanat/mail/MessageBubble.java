package com.esvar.dekanat.mail;

import com.esvar.dekanat.mail.dto.AttachmentDto;
import com.esvar.dekanat.mail.dto.ChatMessageDto;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
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

    public MessageBubble(ChatMessageDto message) {
        addClassName("message-bubble");
        addClassName(message.getDirection() == MessageDirection.IN ? "incoming" : "outgoing");
        add(buildHeader(message), buildBody(message), buildAttachments(message.getAttachments()));
    }

    private Component buildHeader(ChatMessageDto message) {
        Span directionBadge = new Span(message.getDirection() == MessageDirection.IN ? "Вхідне" : "Вихідне");
        directionBadge.addClassName("direction-badge");
        directionBadge.addClassName(message.getDirection() == MessageDirection.IN ? "incoming-badge" : "outgoing-badge");

        Span addresses = new Span(shortAddress(message));
        addresses.addClassName("bubble-address");

        Span time = new Span(message.getSentAt() != null ? timeFormatter.format(message.getSentAt()) : "");
        time.addClassName("message-time");

        HorizontalLayout header = new HorizontalLayout(directionBadge, addresses, time);
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.addClassName("message-header");
        header.setSpacing(true);
        return header;
    }

    private Component buildBody(ChatMessageDto message) {
        QuoteCollapsePanel.QuoteExtraction extraction = QuoteCollapsePanel.extract(message.getBodyText());
        boolean hasQuote = extraction.hasQuote();
        String mainHtml;
        if (!hasQuote && StringUtils.hasText(message.getBodyHtml())) {
            mainHtml = message.getBodyHtml();
        } else {
            mainHtml = toSafeHtml(extraction.main());
        }

        Div bodyWrapper = new Div();
        bodyWrapper.addClassName("bubble-body");
        bodyWrapper.add(new Html("<div class='message-body-html'>" + mainHtml + "</div>"));

        if (hasQuote) {
            bodyWrapper.add(new QuoteCollapsePanel(toSafeHtml(extraction.quote())));
        }
        return bodyWrapper;
    }

    private Component buildAttachments(List<AttachmentDto> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return new Div();
        }
        return new AttachmentList(attachments);
    }

    private String shortAddress(ChatMessageDto message) {
        if (message.getDirection() == MessageDirection.IN) {
            return "Від: " + Optional.ofNullable(message.getFrom()).orElse("");
        }
        return "Кому: " + Optional.ofNullable(message.getTo()).orElse("");
    }

    private String toSafeHtml(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return HtmlUtils.htmlEscape(text).replace("\n", "<br/>");
    }
}
