package com.esvar.dekanat.mail.v2.view.component;

import com.esvar.dekanat.mail.v2.dto.ThreadListItemDto;
import com.esvar.dekanat.mail.v2.entity.MailThreadEntity;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ChatListItem extends Div {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM HH:mm")
            .withZone(ZoneId.systemDefault());

    private final Span name = new Span();
    private final Span time = new Span();
    private final Span org = new Span();
    private final Span snippet = new Span();
    private final Span statusBadge = new Span();
    private final Span externalBadge = new Span("⚠");
    private final Button renameButton = new Button(new Icon(VaadinIcon.EDIT));

    private ThreadListItemDto thread;

    public ChatListItem(ThreadListItemDto thread, boolean selected) {
        this.thread = thread;
        buildLayout();
        refresh(selected);
    }

    private void buildLayout() {
        addClassName("chat-item");
        setWidthFull();

        externalBadge.addClassName("external-icon");
        externalBadge.getElement().setProperty("title", "Зовнішня пошта");

        renameButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        renameButton.addClickListener(e -> fireEvent(new RenameEvent(this, false)));
        renameButton.getElement().setProperty("title", "Редагувати підпис контакту");

        statusBadge.addClassNames("status-pill");

        name.addClassName("chat-name");
        time.addClassName("chat-time");
        org.addClassName("chat-org");
        snippet.addClassName("chat-snippet");

        HorizontalLayout header = new HorizontalLayout(name, time);
        header.setSpacing(false);
        header.setPadding(false);
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.addClassName("chat-item-header");

        FlexLayout badges = new FlexLayout(statusBadge, externalBadge, renameButton);
        badges.addClassName("chat-badges");
        badges.setAlignItems(FlexLayout.Alignment.CENTER);

        VerticalLayout texts = new VerticalLayout(header, org, snippet);
        texts.setPadding(false);
        texts.setSpacing(false);

        FlexLayout row = new FlexLayout(texts, badges);
        row.setWidthFull();
        row.setAlignItems(FlexLayout.Alignment.START);
        row.setJustifyContentMode(FlexLayout.JustifyContentMode.BETWEEN);
        row.addClassName("chat-row");

        add(row);
    }

    private void refresh(boolean selected) {
        name.setText(StringUtils.hasText(thread.getDisplayName()) ? thread.getDisplayName() : "Невідомий");
        time.setText(formatTime(thread.getLastIncomingAt()));
        org.setText(StringUtils.hasText(thread.getOrgUnitText()) ? thread.getOrgUnitText() : "—");
        snippet.setText(buildSnippet());
        statusBadge.setText(statusLetter(thread.getStatus()));
        statusBadge.getElement().setProperty("title", thread.getStatus().name());
        statusBadge.getElement().setAttribute("theme", statusTheme(thread.getStatus()));

        externalBadge.setVisible(thread.isExternal());
        renameButton.setVisible(!thread.isSigned());
        getClassNames().set("active", selected);
    }

    private String buildSnippet() {
        if (StringUtils.hasText(thread.getLastSubject())) {
            return thread.getLastSubject();
        }
        return "—";
    }

    private String formatTime(Instant time) {
        if (time == null) {
            return "";
        }
        return DATE_FORMATTER.format(time);
    }

    private String statusLetter(MailThreadEntity.ThreadStatus status) {
        return switch (status) {
            case NEW -> "N";
            case IN_PROGRESS -> "P";
            case CLOSED -> "C";
        };
    }

    private String statusTheme(MailThreadEntity.ThreadStatus status) {
        return switch (status) {
            case NEW -> "badge primary";
            case IN_PROGRESS -> "badge success";
            case CLOSED -> "badge contrast";
        };
    }

    public void setSelected(boolean selected) {
        refresh(selected);
    }

    public ThreadListItemDto getThread() {
        return thread;
    }

    public Registration addRenameListener(ComponentEventListener<RenameEvent> listener) {
        return addListener(RenameEvent.class, listener);
    }

    public static class RenameEvent extends ComponentEvent<ChatListItem> {
        public RenameEvent(ChatListItem source, boolean fromClient) {
            super(source, fromClient);
        }
    }
}
