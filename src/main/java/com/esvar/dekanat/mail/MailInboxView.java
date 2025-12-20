package com.esvar.dekanat.mail;

import com.esvar.dekanat.mail.dto.ChatFilter;
import com.esvar.dekanat.mail.dto.ChatListItemDto;
import com.esvar.dekanat.mail.dto.ChatMessageDto;
import com.esvar.dekanat.view.MainLayout;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Route(value = "mail", layout = MainLayout.class)
@PageTitle("Пошта як месенджер")
@RolesAllowed({"ROLE_ADMIN", "ROLE_DEKANAT"})
@CssImport(value = "./styles/mail-view.css", themeFor = "vaadin-grid")
public class MailInboxView extends VerticalLayout {

    private final ChatService chatService;
    private final Grid<ChatListItemDto> chatGrid = new Grid<>(ChatListItemDto.class, false);
    private final VerticalLayout messagePanel = new VerticalLayout();
    private final Button loadMoreButton = new Button("Завантажити ще");
    private final TextArea replyArea = new TextArea("Відповідь");
    private final ComboBox<ChatStatus> statusComboBox = new ComboBox<>("Статус");
    private final Button markProcessedButton = new Button("Позначити опрацьованим");
    private final ComboBox<ChatStatus> statusFilter = new ComboBox<>();

    private ChatListItemDto selectedChat;
    private final TextField searchField = new TextField();
    private int messagePage = 0;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    public MailInboxView(ChatService chatService) {
        this.chatService = chatService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(buildToolbar(), buildContent());
    }

    private HorizontalLayout buildToolbar() {
        searchField.setPlaceholder("Email або ПІБ");
        searchField.setClearButtonVisible(true);
        searchField.addValueChangeListener(e -> chatGrid.getDataProvider().refreshAll());

        statusFilter.setPlaceholder("Статус");
        statusFilter.setItems(ChatStatus.values());
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> chatGrid.getDataProvider().refreshAll());

        Button refreshButton = new Button("Оновити", e -> chatGrid.getDataProvider().refreshAll());
        HorizontalLayout toolbar = new HorizontalLayout(searchField, statusFilter, refreshButton);
        toolbar.setWidthFull();
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        return toolbar;
    }

    private SplitLayout buildContent() {
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(32);

        configureChatGrid();
        splitLayout.addToPrimary(chatGrid);

        VerticalLayout right = new VerticalLayout();
        right.setSizeFull();
        right.setPadding(false);
        right.setSpacing(false);

        HorizontalLayout header = buildChatHeader();
        right.add(header);

        messagePanel.setSizeFull();
        messagePanel.setPadding(true);
        messagePanel.setSpacing(true);
        right.add(messagePanel);
        right.setFlexGrow(1, messagePanel);

        loadMoreButton.setWidthFull();
        loadMoreButton.addClickListener(e -> loadMessagesPage(messagePage + 1, true));
        loadMoreButton.setVisible(false);
        right.add(loadMoreButton);

        VerticalLayout replyWrapper = buildReplyArea();
        right.add(replyWrapper);
        splitLayout.addToSecondary(right);
        return splitLayout;
    }

    private void configureChatGrid() {
        CallbackDataProvider<ChatListItemDto, Void> dataProvider = new CallbackDataProvider<>(
                this::fetchChats, this::countChats);
        chatGrid.setDataProvider(dataProvider);
        chatGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        chatGrid.setHeightFull();

        chatGrid.addColumn(ChatListItemDto::getDisplayName).setHeader("ПІБ").setFlexGrow(1);
        chatGrid.addColumn(ChatListItemDto::getPeerEmail).setHeader("Email").setFlexGrow(1);
        chatGrid.addColumn(ChatListItemDto::getOrgUnit).setHeader("Факультет/Кафедра").setFlexGrow(1);
        chatGrid.addColumn(item -> item.getStatus().name()).setHeader("Статус").setWidth("140px");
        chatGrid.addColumn(item -> item.isHasUnprocessed() ? "1" : "0").setHeader("Неопрацьовано").setWidth("140px");
        chatGrid.addColumn(item -> item.getLastMessageAt() != null ? dateTimeFormatter.format(item.getLastMessageAt()) : "")
                .setHeader("Останнє").setWidth("180px");

        chatGrid.asSingleSelect().addValueChangeListener(event -> {
            selectedChat = event.getValue();
            loadMessages();
            updateHeader();
        });
    }

    private HorizontalLayout buildChatHeader() {
        H3 title = new H3("Діалог");
        title.getStyle().set("margin", "0");

        statusComboBox.setItems(ChatStatus.values());
        statusComboBox.addValueChangeListener(e -> {
            if (selectedChat != null && e.getValue() != null) {
                chatService.updateStatus(selectedChat.getId(), e.getValue());
                selectedChat = ChatListItemDto.builder()
                        .id(selectedChat.getId())
                        .displayName(selectedChat.getDisplayName())
                        .peerEmail(selectedChat.getPeerEmail())
                        .orgUnit(selectedChat.getOrgUnit())
                        .status(e.getValue())
                        .hasUnprocessed(selectedChat.isHasUnprocessed())
                        .lastMessageAt(selectedChat.getLastMessageAt())
                        .build();
                chatGrid.getDataProvider().refreshAll();
            }
        });

        markProcessedButton.addClickListener(e -> {
            if (selectedChat != null) {
                chatService.markProcessed(selectedChat.getId());
                selectedChat = ChatListItemDto.builder()
                        .id(selectedChat.getId())
                        .displayName(selectedChat.getDisplayName())
                        .peerEmail(selectedChat.getPeerEmail())
                        .orgUnit(selectedChat.getOrgUnit())
                        .status(selectedChat.getStatus())
                        .hasUnprocessed(false)
                        .lastMessageAt(selectedChat.getLastMessageAt())
                        .build();
                chatGrid.getDataProvider().refreshAll();
                updateHeader();
            }
        });
        markProcessedButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout header = new HorizontalLayout(title, statusComboBox, markProcessedButton);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        return header;
    }

    private VerticalLayout buildReplyArea() {
        replyArea.setWidthFull();
        replyArea.setHeight(180, Unit.PIXELS);
        Button sendButton = new Button("Відправити", e -> sendReply());
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        VerticalLayout wrapper = new VerticalLayout(replyArea, sendButton);
        wrapper.setWidthFull();
        wrapper.setPadding(true);
        wrapper.setSpacing(true);
        return wrapper;
    }

    private void sendReply() {
        if (selectedChat == null) {
            return;
        }
        String text = replyArea.getValue();
        if (text == null || text.isBlank()) {
            return;
        }
        chatService.replyToChat(selectedChat.getId(), text, null);
        replyArea.clear();
    }

    private void loadMessages() {
        messagePanel.removeAll();
        if (selectedChat == null) {
            messagePanel.add(new Span("Оберіть чат"));
            loadMoreButton.setVisible(false);
            return;
        }
        messagePage = 0;
        loadMessagesPage(messagePage, false);
    }

    private void loadMessagesPage(int page, boolean append) {
        if (selectedChat == null) {
            return;
        }
        Pageable pageable = PageRequest.of(page, 50, Sort.by(Sort.Direction.DESC, "sentAt"));
        Page<ChatMessageDto> messages = chatService.findMessages(selectedChat.getId(), pageable);
        if (!append) {
            messagePanel.removeAll();
        }
        messages.forEach(this::appendMessage);
        messagePage = page;
        loadMoreButton.setVisible(messages.hasNext());
    }

    private void appendMessage(ChatMessageDto message) {
        Div bubble = new Div();
        bubble.addClassName(message.getDirection() == MessageDirection.IN ? "incoming" : "outgoing");

        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("message-header");
        Span directionBadge = new Span(message.getDirection() == MessageDirection.IN ? "Вхідне" : "Вихідне");
        directionBadge.addClassNames("direction-badge",
                message.getDirection() == MessageDirection.IN ? "incoming-badge" : "outgoing-badge");
        Span time = new Span(metaText(message));
        time.addClassName("message-time");
        header.add(directionBadge, time);
        header.setWidthFull();

        Div addresses = new Div();
        addresses.addClassName("message-addresses");
        addresses.add(buildAddressRow("Від", message.getFrom(), "from"));
        addresses.add(buildAddressRow("Кому", message.getTo(), "to"));

        Paragraph body = new Paragraph(Optional.ofNullable(message.getBodyText()).orElse(""));
        body.addClassName("message-body");

        bubble.add(header, addresses, body);

        if (message.isHasAttachments() && message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            VerticalLayout attachments = new VerticalLayout();
            attachments.addClassName("message-attachments");
            attachments.setPadding(false);
            attachments.setSpacing(false);
            Span label = new Span("Вкладення");
            label.addClassName("attachments-label");
            attachments.add(label);
            message.getAttachments().forEach(att -> {
                Button download = new Button(att.getFilename() != null ? att.getFilename() : "Вкладення");
                download.addClickListener(e -> download.getUI().ifPresent(ui ->
                        ui.getPage().open("/mail/messages/" + message.getMessageId() + "/attachments/" + att.getAttachmentId())));
                download.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                attachments.add(download);
            });
            bubble.add(attachments);
        }

        messagePanel.add(bubble);
    }

    private String metaText(ChatMessageDto message) {
        return message.getSentAt() != null ? dateTimeFormatter.format(message.getSentAt()) : "";
    }

    private Div buildAddressRow(String labelText, String value, String modifier) {
        Div row = new Div();
        row.addClassNames("address-row", modifier + "-address");

        Span label = new Span(labelText);
        label.addClassName("address-label");

        Span valueSpan = new Span(Optional.ofNullable(value).orElse(""));
        valueSpan.addClassName("address-value");

        row.add(label, valueSpan);
        return row;
    }

    private java.util.stream.Stream<ChatListItemDto> fetchChats(Query<ChatListItemDto, Void> query) {
        Pageable pageable = PageRequest.of(query.getPage(), query.getPageSize(), Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        ChatFilter filter = new ChatFilter();
        filter.setQuery(searchField.getValue());
        if (statusFilter.getValue() != null) {
            filter.setStatuses(List.of(statusFilter.getValue()));
        }
        Page<ChatListItemDto> page = chatService.findChats(filter, pageable);
        return page.stream();
    }

    private int countChats(Query<ChatListItemDto, Void> query) {
        Pageable pageable = PageRequest.of(0, 1);
        ChatFilter filter = new ChatFilter();
        filter.setQuery(searchField.getValue());
        if (statusFilter.getValue() != null) {
            filter.setStatuses(List.of(statusFilter.getValue()));
        }
        Page<ChatListItemDto> page = chatService.findChats(filter, pageable);
        return (int) page.getTotalElements();
    }

    private void updateHeader() {
        if (selectedChat != null) {
            statusComboBox.setValue(selectedChat.getStatus());
        }
    }
}
