package com.esvar.dekanat.mail;

import com.esvar.dekanat.mail.dto.ChatFilter;
import com.esvar.dekanat.mail.dto.ChatListItemDto;
import com.esvar.dekanat.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
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

@Route(value = "mail", layout = MainLayout.class)
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

        conversationView.setChatUpdateListener(chat -> chatGrid.getDataProvider().refreshAll());
        splitLayout.addToSecondary(conversationView);
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
            conversationView.showChat(selectedChat);
        });
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
