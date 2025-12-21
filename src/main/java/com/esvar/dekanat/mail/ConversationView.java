package com.esvar.dekanat.mail;

import com.esvar.dekanat.mail.dto.ChatListItemDto;
import com.esvar.dekanat.mail.dto.ChatMessageDetailDto;
import com.esvar.dekanat.mail.dto.ChatMessageHeaderDto;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import jakarta.mail.MessagingException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@CssImport("./styles/conversation-view.css")
public class ConversationView extends VerticalLayout {

    private static final int PAGE_SIZE = 30;

    private final ChatService chatService;
    private final ComboBox<ChatStatus> statusComboBox = new ComboBox<>("Статус");
    private final Button markProcessedButton = new Button("Позначити опрацьованим");
    private final Button closeButton = new Button("Закрити");
    private final Button loadMoreButton = new Button("Завантажити попередні");
    private final Span metaInfo = new Span();
    private final Div messagesContainer = new Div();

    private ChatListItemDto currentChat;
    private int currentPage = 0;
    private final List<ChatMessageHeaderDto> loadedMessages = new ArrayList<>();
    private Consumer<ChatListItemDto> chatUpdateListener;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public ConversationView(ChatService chatService) {
        this.chatService = chatService;
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        add(buildHeader(), buildMessagesArea(), buildLoadMore());
    }

    public void setChatUpdateListener(Consumer<ChatListItemDto> chatUpdateListener) {
        this.chatUpdateListener = chatUpdateListener;
    }

    public void showChat(ChatListItemDto chat) {
        this.currentChat = chat;
        this.currentPage = 0;
        this.loadedMessages.clear();
        updateHeaderState();
        loadPage(true);
    }

    private Component buildHeader() {
        H3 title = new H3("Діалог");
        title.addClassName("conversation-title");

        statusComboBox.setItems(ChatStatus.values());
        statusComboBox.addValueChangeListener(e -> {
            if (currentChat != null && e.getValue() != null && e.isFromClient()) {
                chatService.updateStatus(currentChat.getId(), e.getValue());
                currentChat = ChatListItemDto.builder()
                        .id(currentChat.getId())
                        .displayName(currentChat.getDisplayName())
                        .peerEmail(currentChat.getPeerEmail())
                        .orgUnit(currentChat.getOrgUnit())
                        .status(e.getValue())
                        .hasUnprocessed(false)
                        .lastMessageAt(currentChat.getLastMessageAt())
                        .build();
                notifyChatUpdated();
                updateHeaderState();
            }
        });

        markProcessedButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        markProcessedButton.addClickListener(e -> {
            if (currentChat == null) {
                return;
            }
            chatService.markProcessed(currentChat.getId());
            ChatStatus newStatus = currentChat.getStatus() == ChatStatus.NEW ? ChatStatus.IN_PROGRESS : currentChat.getStatus();
            chatService.updateStatus(currentChat.getId(), newStatus);
            currentChat = ChatListItemDto.builder()
                    .id(currentChat.getId())
                    .displayName(currentChat.getDisplayName())
                    .peerEmail(currentChat.getPeerEmail())
                    .orgUnit(currentChat.getOrgUnit())
                    .status(newStatus)
                    .hasUnprocessed(false)
                    .lastMessageAt(currentChat.getLastMessageAt())
                    .build();
            notifyChatUpdated();
            updateHeaderState();
        });

        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.addClickListener(e -> {
            if (currentChat == null) {
                return;
            }
            chatService.updateStatus(currentChat.getId(), ChatStatus.CLOSED);
            currentChat = ChatListItemDto.builder()
                    .id(currentChat.getId())
                    .displayName(currentChat.getDisplayName())
                    .peerEmail(currentChat.getPeerEmail())
                    .orgUnit(currentChat.getOrgUnit())
                    .status(ChatStatus.CLOSED)
                    .hasUnprocessed(false)
                    .lastMessageAt(currentChat.getLastMessageAt())
                    .build();
            notifyChatUpdated();
            updateHeaderState();
        });

        metaInfo.addClassName("conversation-meta");

        HorizontalLayout left = new HorizontalLayout(title, statusComboBox, markProcessedButton, closeButton);
        left.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);
        left.setPadding(false);
        left.setFlexGrow(1, title);

        HorizontalLayout wrapper = new HorizontalLayout(left, metaInfo);
        wrapper.setWidthFull();
        wrapper.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        wrapper.setSpacing(true);
        wrapper.setPadding(true);
        wrapper.addClassName("conversation-header");
        return wrapper;
    }

    private Component buildMessagesArea() {
        messagesContainer.setHeightFull();
        messagesContainer.addClassName("conversation-messages");
        return messagesContainer;
    }

    private Component buildLoadMore() {
        loadMoreButton.setWidthFull();
        loadMoreButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        loadMoreButton.addClickListener(e -> loadPage(false));
        loadMoreButton.setVisible(false);
        Div wrapper = new Div(loadMoreButton);
        wrapper.addClassName("conversation-load-more");
        return wrapper;
    }

    private void updateHeaderState() {
        boolean hasChat = currentChat != null;
        statusComboBox.setEnabled(hasChat);
        markProcessedButton.setEnabled(hasChat);
        closeButton.setEnabled(hasChat);
        loadMoreButton.setEnabled(hasChat);
        if (!hasChat) {
            messagesContainer.removeAll();
            messagesContainer.add(new Span("Оберіть діалог"));
            metaInfo.setText("");
            return;
        }
        statusComboBox.setValue(currentChat.getStatus());
        metaInfo.setText(buildMetaText());
    }

    private String buildMetaText() {
        if (CollectionUtils.isEmpty(loadedMessages)) {
            return "";
        }
        ChatMessageHeaderDto last = loadedMessages.get(loadedMessages.size() - 1);
        String lastTime = last.getSentAt() != null ? dateTimeFormatter.format(last.getSentAt()) : "";
        return String.format("Останнє повідомлення: %s • %d повідомлень", lastTime, loadedMessages.size());
    }

    private void loadPage(boolean initial) {
        if (currentChat == null) {
            return;
        }
        Pageable pageable = PageRequest.of(currentPage, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "sentAt"));
        Page<ChatMessageHeaderDto> page = chatService.findMessageHeaders(currentChat.getId(), pageable);
        List<ChatMessageHeaderDto> batch = new ArrayList<>(page.getContent());
        batch.sort(Comparator.comparing(ChatMessageHeaderDto::getSentAt, Comparator.nullsLast(Comparator.naturalOrder())));

        loadedMessages.addAll(batch);
        loadedMessages.sort(Comparator.comparing(ChatMessageHeaderDto::getSentAt, Comparator.nullsLast(Comparator.naturalOrder())));

        renderMessages(initial);

        loadMoreButton.setVisible(page.hasNext());
        if (page.hasNext()) {
            currentPage++;
        }
        metaInfo.setText(buildMetaText());
        if (initial) {
            messagesContainer.getElement().executeJs("this.scrollTop = this.scrollHeight;");
        }
    }

    private void renderMessages(boolean initial) {
        messagesContainer.removeAll();
        LocalDate lastDate = null;
        for (ChatMessageHeaderDto message : loadedMessages) {
            LocalDate date = Optional.ofNullable(message.getSentAt())
                    .map(instant -> instant.atZone(ZoneId.systemDefault()).toLocalDate())
                    .orElse(null);
            if (!Objects.equals(date, lastDate)) {
                messagesContainer.add(new MessageDateDivider(date != null ? dateFormatter.format(date) : ""));
                lastDate = date;
            }
            messagesContainer.add(new MessageBubble(message, this::loadMessageDetails));
        }
        if (!initial) {
            messagesContainer.getElement().executeJs("this.scrollTop = this.scrollHeight;");
        }
    }

    private ChatMessageDetailDto loadMessageDetails(Long messageId) {
        try {
            return chatService.getMessageDetails(messageId);
        } catch (MessagingException e) {
            throw new IllegalStateException("Не вдалося завантажити повідомлення", e);
        }
    }

    private void notifyChatUpdated() {
        if (chatUpdateListener != null) {
            chatUpdateListener.accept(currentChat);
        }
    }
}
