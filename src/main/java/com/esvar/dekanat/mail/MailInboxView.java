package com.esvar.dekanat.mail;

import com.esvar.dekanat.mail.dto.ChatFilter;
import com.esvar.dekanat.mail.dto.ChatListItemDto;
import com.esvar.dekanat.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
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

@Route(value = "mail-legacy", layout = MainLayout.class)
@PageTitle("Пошта як месенджер")
@RolesAllowed({"ROLE_ADMIN", "ROLE_DEKANAT"})
@CssImport("./styles/mail-view.css")
public class MailInboxView extends VerticalLayout {

    private final ChatService chatService;
    private final Grid<ChatListItemDto> chatGrid = new Grid<>(ChatListItemDto.class, false);
    private final ComboBox<ChatStatus> statusFilter = new ComboBox<>();
    private final ConversationView conversationView;

    private ChatListItemDto selectedChat;
    private final TextField searchField = new TextField();

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    public MailInboxView(ChatService chatService) {
        this.chatService = chatService;
        this.conversationView = new ConversationView(chatService);
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(buildToolbar(), buildContent());
    }

    private HorizontalLayout buildToolbar() {
        searchField.setPlaceholder("Email або ім'я");
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

        conversationView.setChatUpdateListener(chat -> chatGrid.getDataProvider().refreshAll());
        splitLayout.addToSecondary(conversationView);
        return splitLayout;
    }

    private void configureChatGrid() {
        CallbackDataProvider<ChatListItemDto, Void> dataProvider = new CallbackDataProvider<>(
                this::fetchChats, this::countChats);
        chatGrid.setDataProvider(dataProvider);
        chatGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        chatGrid.setHeightFull();

        chatGrid.addComponentColumn(this::buildThreadPreview)
                .setHeader("Чати")
                .setFlexGrow(1);
        chatGrid.addColumn(item -> item.getLastMessageAt() != null ? dateTimeFormatter.format(item.getLastMessageAt()) : "")
                .setHeader("Останнє")
                .setWidth("180px")
                .setTextAlign(ColumnTextAlign.END);
        chatGrid.addColumn(item -> item.getStatus().name()).setHeader("Статус").setWidth("140px");

        chatGrid.asSingleSelect().addValueChangeListener(event -> {
            selectedChat = event.getValue();
            conversationView.showChat(selectedChat);
        });
    }

    private Div buildThreadPreview(ChatListItemDto item) {
        Div wrapper = new Div();
        wrapper.addClassName("thread-preview");

        Span title = new Span(item.getDisplayName() != null ? item.getDisplayName() : item.getContactEmail());
        title.addClassName("thread-title");

        Span counterparty = new Span(item.getContactEmail());
        counterparty.addClassName("thread-counterparty");

        Span snippet = new Span(item.getLastSnippet() != null ? item.getLastSnippet() : "");
        snippet.addClassName("thread-snippet");

        HorizontalLayout badges = new HorizontalLayout();
        badges.setSpacing(true);
        badges.setPadding(false);
        badges.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        if (item.getUnreadCount() > 0 || item.isHasUnprocessed()) {
            Span unread = new Span(item.getUnreadCount() > 0 ? item.getUnreadCount() + " нових" : "нове");
            unread.addClassName("thread-unread");
            badges.add(unread);
        }

        if (item.isHasAttachments()) {
            Icon clip = VaadinIcon.PAPERCLIP.create();
            clip.addClassName("thread-attachment");
            badges.add(clip);
        }

        Span status = new Span(item.getStatus().name());
        status.addClassName("thread-status");
        badges.add(status);

        wrapper.add(title, counterparty, snippet, badges);
        return wrapper;
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
}
