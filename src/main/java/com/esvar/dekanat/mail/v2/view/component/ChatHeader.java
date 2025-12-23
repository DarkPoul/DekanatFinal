package com.esvar.dekanat.mail.v2.view.component;

import com.esvar.dekanat.mail.v2.dto.ThreadListItemDto;
import com.esvar.dekanat.mail.v2.entity.MailThreadEntity;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import org.springframework.util.StringUtils;

import java.util.function.BiConsumer;

public class ChatHeader extends Div {

    private final Span title = new Span("Обрати чат");
    private final Span email = new Span();
    private final Span externalBadge = new Span("⚠");

    private final Button statusNew = new Button("NEW");
    private final Button statusInProgress = new Button("IN_PROGRESS");
    private final Button statusClosed = new Button("CLOSED");
    private final Button rename = new Button(new Icon(VaadinIcon.EDIT));

    private ThreadListItemDto thread;

    public ChatHeader() {
        addClassName("chat-header");
        setWidthFull();
        buildLayout();
    }

    private void buildLayout() {
        title.addClassName("chat-header-title");
        email.addClassName("chat-header-email");
        externalBadge.addClassName("chat-header-external");
        externalBadge.getElement().setProperty("title", "Зовнішня пошта");

        rename.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        rename.getElement().setProperty("title", "Редагувати контакт");
        rename.addClickListener(e -> fireEvent(new RenameEvent(this, false)));

        statusNew.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        statusInProgress.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        statusClosed.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        statusNew.addClickListener(e -> fireEvent(new StatusChangeEvent(this, MailThreadEntity.ThreadStatus.NEW)));
        statusInProgress.addClickListener(e -> fireEvent(new StatusChangeEvent(this, MailThreadEntity.ThreadStatus.IN_PROGRESS)));
        statusClosed.addClickListener(e -> fireEvent(new StatusChangeEvent(this, MailThreadEntity.ThreadStatus.CLOSED)));

        HorizontalLayout info = new HorizontalLayout(title, externalBadge, rename);
        info.setSpacing(true);
        info.setPadding(false);
        info.setWidthFull();
        info.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        HorizontalLayout statuses = new HorizontalLayout(statusNew, statusInProgress, statusClosed);
        statuses.setSpacing(false);
        statuses.addClassName("status-toggle");

        FlexLayout content = new FlexLayout();
        content.addClassName("chat-header-content");
        content.setAlignItems(FlexLayout.Alignment.CENTER);
        content.setJustifyContentMode(FlexLayout.JustifyContentMode.BETWEEN);
        content.add(info, email, statuses);

        add(content);
    }

    public void setThread(ThreadListItemDto thread) {
        this.thread = thread;
        title.setText(thread.getDisplayName());
        email.setText(thread.getEmail());
        externalBadge.setVisible(thread.isExternal());
        rename.setVisible(thread.isExternal() || !thread.isSigned());
        highlightStatus(thread.getStatus());
    }

    private void highlightStatus(MailThreadEntity.ThreadStatus status) {
        statusNew.removeThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_CONTRAST);
        statusInProgress.removeThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_CONTRAST);
        statusClosed.removeThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_CONTRAST);
        switch (status) {
            case NEW -> statusNew.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            case IN_PROGRESS -> statusInProgress.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            case CLOSED -> statusClosed.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            default -> {
            }
        }
    }

    public void openRenameDialog(ThreadListItemDto thread, BiConsumer<String, String> onSave) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Підписати контакт");
        TextField nameField = new TextField("ПІБ/Назва");
        nameField.setWidthFull();
        nameField.setValue(StringUtils.hasText(thread.getDisplayName()) ? thread.getDisplayName() : "");
        TextField orgField = new TextField("Кафедра/Факультет/Інститут");
        orgField.setWidthFull();
        orgField.setValue(StringUtils.hasText(thread.getOrgUnitText()) ? thread.getOrgUnitText() : "");

        Button save = new Button("Зберегти", e -> {
            dialog.close();
            onSave.accept(nameField.getValue(), orgField.getValue());
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Скасувати", e -> dialog.close());

        HorizontalLayout actions = new HorizontalLayout(cancel, save);
        actions.setPadding(false);
        actions.setJustifyContentMode(JustifyContentMode.END);
        actions.setWidthFull();

        VerticalLayout layout = new VerticalLayout(nameField, orgField, actions);
        layout.setPadding(false);
        layout.setSpacing(true);
        dialog.add(layout);
        dialog.open();
    }

    public Registration addStatusChangeListener(ComponentEventListener<StatusChangeEvent> listener) {
        return addListener(StatusChangeEvent.class, listener);
    }

    public Registration addRenameListener(ComponentEventListener<RenameEvent> listener) {
        return addListener(RenameEvent.class, listener);
    }

    public static class StatusChangeEvent extends ComponentEvent<ChatHeader> {
        private final MailThreadEntity.ThreadStatus status;

        public StatusChangeEvent(ChatHeader source, MailThreadEntity.ThreadStatus status) {
            super(source, false);
            this.status = status;
        }

        public MailThreadEntity.ThreadStatus getStatus() {
            return status;
        }
    }

    public static class RenameEvent extends ComponentEvent<ChatHeader> {
        public RenameEvent(ChatHeader source, boolean fromClient) {
            super(source, fromClient);
        }
    }
}
