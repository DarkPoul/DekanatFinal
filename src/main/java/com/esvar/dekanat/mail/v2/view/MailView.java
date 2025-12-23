package com.esvar.dekanat.mail.v2.view;

import com.esvar.dekanat.mail.v2.dto.MessageDto;
import com.esvar.dekanat.mail.v2.dto.ThreadListItemDto;
import com.esvar.dekanat.mail.v2.entity.MailMessageEntity;
import com.esvar.dekanat.mail.v2.entity.MailThreadEntity;
import com.esvar.dekanat.mail.v2.service.MessageService;
import com.esvar.dekanat.mail.v2.service.SendMailService;
import com.esvar.dekanat.mail.v2.service.ThreadService;
import com.esvar.dekanat.view.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.util.StringUtils;

import java.util.List;

@Route(value = "mail", layout = MainLayout.class)
@PageTitle("Пошта")
@RolesAllowed("ROLE_ADMIN")
@CssImport("./styles/mail-messenger.css")
public class MailView extends HorizontalLayout {

    private final ThreadService threadService;
    private final MessageService messageService;
    private final SendMailService sendMailService;

    private final com.vaadin.flow.component.grid.Grid<ThreadListItemDto> threadGrid = new com.vaadin.flow.component.grid.Grid<>();
    private final TextField nameField = new TextField("ПІБ/Назва");
    private final TextField emailField = new TextField("Email");
    private final TextField orgField = new TextField("Кафедра/Фак/Інст");
    private final Select<MailThreadEntity.ThreadStatus> statusSelect = new Select<>();

    private final Div messageContainer = new Div();
    private final Button loadMoreButton = new Button("Завантажити ще");
    private final Button statusNew = new Button("NEW");
    private final Button statusInProgress = new Button("IN_PROGRESS");
    private final Button statusClosed = new Button("CLOSED");
    private final Button signButton = new Button("Підписати");
    private final H3 headerTitle = new H3("Обрати чат");
    private final Span headerEmail = new Span();

    private final TextArea replyField = new TextArea("Відповідь");
    private final MultiFileMemoryBuffer uploadBuffer = new MultiFileMemoryBuffer();
    private final Upload upload = new Upload(uploadBuffer);
    private final Button sendButton = new Button("Відправити");

    private ThreadListItemDto currentThread;
    private int messagePageSize = 10;
    private java.time.Instant beforeCursor;

    public MailView(ThreadService threadService,
                    MessageService messageService,
                    SendMailService sendMailService) {
        this.threadService = threadService;
        this.messageService = messageService;
        this.sendMailService = sendMailService;
        setSizeFull();
        setPadding(false);
        buildLayout();
        configureGrid();
        configureFilters();
        configureActions();
    }

    private void buildLayout() {
        VerticalLayout left = new VerticalLayout();
        left.setWidth("32%");
        left.setPadding(true);
        left.setSpacing(true);
        left.setHeightFull();

        HorizontalLayout filters = new HorizontalLayout(nameField, emailField, orgField, statusSelect);
        filters.setWidthFull();
        filters.setSpacing(true);
        filters.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        nameField.setWidth("180px");
        emailField.setWidth("150px");
        orgField.setWidth("150px");
        statusSelect.setWidth("140px");

        threadGrid.setHeightFull();

        left.add(filters, threadGrid);

        VerticalLayout right = new VerticalLayout();
        right.setHeightFull();
        right.setWidth("68%");
        right.setPadding(true);
        right.setSpacing(false);
        right.add(buildHeader(), buildMessageArea(), buildComposer());

        add(left, right);
    }

    private Component buildHeader() {
        statusSelect.setItems(MailThreadEntity.ThreadStatus.values());
        statusSelect.setPlaceholder("Статус");

        statusNew.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        statusInProgress.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        statusClosed.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        signButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        signButton.setVisible(false);
        signButton.addClickListener(e -> {
            if (currentThread != null) {
                threadService.signContact(currentThread.getContactId(), currentThread.getDisplayName(), currentThread.getOrgUnitText());
                Notification.show("Контакт підписано");
            }
        });

        HorizontalLayout header = new HorizontalLayout(headerTitle, headerEmail, statusNew, statusInProgress, statusClosed, signButton);
        header.setWidthFull();
        header.setSpacing(true);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        return header;
    }

    private Component buildMessageArea() {
        messageContainer.setHeight("70vh");
        messageContainer.addClassName("mail-thread");
        loadMoreButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        loadMoreButton.setWidthFull();
        loadMoreButton.addClickListener(e -> loadMessagesPage());
        Div wrapper = new Div(messageContainer, loadMoreButton);
        wrapper.addClassName("mail-thread-wrapper");
        return wrapper;
    }

    private Component buildComposer() {
        replyField.setHeight("150px");
        replyField.setWidthFull();
        upload.setDropAllowed(true);
        upload.setMaxFiles(5);
        upload.setWidthFull();
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.setWidthFull();
        sendButton.addClickListener(e -> sendReply());
        VerticalLayout layout = new VerticalLayout(replyField, upload, sendButton);
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.setWidthFull();
        return layout;
    }

    private void configureFilters() {
        nameField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);
        emailField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);
        orgField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);
        nameField.setValueChangeTimeout(400);
        emailField.setValueChangeTimeout(400);
        orgField.setValueChangeTimeout(400);

        Runnable refresh = () -> threadGrid.getDataProvider().refreshAll();
        nameField.addValueChangeListener(e -> refresh.run());
        emailField.addValueChangeListener(e -> refresh.run());
        orgField.addValueChangeListener(e -> refresh.run());
        statusSelect.addValueChangeListener(e -> refresh.run());
    }

    private void configureGrid() {
        threadGrid.addColumn(new ComponentRenderer<>(this::renderThreadItem))
                .setAutoWidth(true)
                .setFlexGrow(1);
        threadGrid.setSelectionMode(com.vaadin.flow.component.grid.Grid.SelectionMode.SINGLE);
        threadGrid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) {
                openThread(e.getValue());
            }
        });

        CallbackDataProvider.FetchCallback<ThreadListItemDto, Void> fetch = query -> threadService.findThreads(
                nameField.getValue(), emailField.getValue(), orgField.getValue(),
                statusSelect.getValue(), query.getOffset(), query.getLimit()).stream();
        CallbackDataProvider.CountCallback<ThreadListItemDto, Void> count = query -> (int) threadService.findThreads(
                nameField.getValue(), emailField.getValue(), orgField.getValue(),
                statusSelect.getValue(), 0, query.getLimit()).getTotalElements();
        threadGrid.setItems(new CallbackDataProvider<>(fetch, count));
    }

    private Component renderThreadItem(ThreadListItemDto dto) {
        Div wrapper = new Div();
        wrapper.addClassName("thread-item");
        Span title = new Span(dto.getDisplayName() + " - " + (dto.getLastIncomingAt() != null ? dto.getLastIncomingAt() : ""));
        Span org = new Span(StringUtils.hasText(dto.getOrgUnitText()) ? dto.getOrgUnitText() : "Невідомий");
        Span subject = new Span(StringUtils.hasText(dto.getLastSubject()) ? "Тема: " + dto.getLastSubject() : "");
        Span status = new Span(dto.getStatus().name());
        status.addClassName("status-badge");
        if (dto.isExternal()) {
            Span external = new Span("!");
            external.getElement().setProperty("title", "Зовнішня пошта");
            wrapper.add(external);
        }
        wrapper.add(title, org, subject, status);
        return wrapper;
    }

    private void configureActions() {
        statusNew.addClickListener(e -> changeStatus(MailThreadEntity.ThreadStatus.NEW));
        statusInProgress.addClickListener(e -> changeStatus(MailThreadEntity.ThreadStatus.IN_PROGRESS));
        statusClosed.addClickListener(e -> changeStatus(MailThreadEntity.ThreadStatus.CLOSED));
    }

    private void changeStatus(MailThreadEntity.ThreadStatus status) {
        if (currentThread == null) {
            return;
        }
        threadService.updateStatus(currentThread.getThreadId(), status);
        currentThread.setStatus(status);
        headerEmail.setText(status.name());
        threadGrid.getDataProvider().refreshAll();
    }

    private void openThread(ThreadListItemDto dto) {
        currentThread = dto;
        headerTitle.setText(dto.getDisplayName());
        headerEmail.setText(dto.getEmail());
        signButton.setVisible(dto.isExternal() && !dto.isSigned());
        beforeCursor = null;
        messageContainer.removeAll();
        threadService.markViewed(dto.getThreadId());
        loadMessagesPage();
    }

    private void loadMessagesPage() {
        if (currentThread == null) {
            return;
        }
        List<MessageDto> batch = messageService.loadMessages(currentThread.getThreadId(), beforeCursor, messagePageSize);
        if (!batch.isEmpty()) {
            beforeCursor = batch.get(0).getSentAt();
        }
        for (MessageDto message : batch) {
            messageContainer.add(renderMessage(message));
        }
    }

    private Component renderMessage(MessageDto message) {
        Div bubble = new Div();
        bubble.addClassNames("message-bubble", message.getDirection() == MailMessageEntity.Direction.IN ? "incoming" : "outgoing");
        Span meta = new Span((message.getSentAt() != null ? message.getSentAt().toString() : "") + " • " + (StringUtils.hasText(message.getSubject()) ? message.getSubject() : ""));
        meta.addClassName("message-meta");
        Div body = new Div();
        if (StringUtils.hasText(message.getBodyHtml())) {
            body.getElement().setProperty("innerHTML", message.getBodyHtml());
        } else if (StringUtils.hasText(message.getBodyText())) {
            body.add(new Text(message.getBodyText()));
        }
        bubble.add(meta, body);

        if (!message.getAttachments().isEmpty()) {
            VerticalLayout attachments = new VerticalLayout();
            attachments.setSpacing(false);
            attachments.setPadding(false);
            attachments.add(new Span("Вкладення:"));
            for (MessageDto.MessageAttachmentDto attachment : message.getAttachments()) {
                Anchor link = new Anchor("/api/mail/v2/attachments/" + attachment.getId() + "/download", attachment.getFilename());
                attachments.add(link);
            }
            bubble.add(attachments);
        }
        return bubble;
    }

    private void sendReply() {
        if (currentThread == null) {
            return;
        }
        String text = replyField.getValue();
        sendMailService.send(threadService.findById(currentThread.getThreadId()).orElseThrow(), text, "Re", List.of());
        replyField.clear();
        Notification.show("Відправлено");
        loadMessagesPage();
    }
}
