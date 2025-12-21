package com.esvar.dekanat.mail;

import com.esvar.dekanat.mail.dto.ChatListItemDto;
import com.esvar.dekanat.mail.dto.ChatMessageDetailDto;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@CssImport("./styles/conversation-view.css")
public class ConversationView extends VerticalLayout {

    private final ChatService chatService;
    private final ComboBox<ChatStatus> statusComboBox = new ComboBox<>("Статус");
    private final Button markProcessedButton = new Button("Позначити опрацьованим");
    private final Button closeButton = new Button("Закрити");
    private final Span metaInfo = new Span();
    private final Div messagesContainer = new Div();
    private final H3 title = new H3("Діалог");

    private ChatListItemDto currentChat;
    private final List<ChatMessageDetailDto> loadedMessages = new ArrayList<>();
    private Consumer<ChatListItemDto> chatUpdateListener;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public ConversationView(ChatService chatService) {
        this.chatService = chatService;
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        add(buildHeader(), buildMessagesArea());
    }

    public void setChatUpdateListener(Consumer<ChatListItemDto> chatUpdateListener) {
        this.chatUpdateListener = chatUpdateListener;
    }

    public void showChat(ChatListItemDto chat) {
        this.currentChat = chat;
        this.loadedMessages.clear();
        updateHeaderState();
        loadMessages();
    }

    private Component buildHeader() {
        statusComboBox.setItems(ChatStatus.values());
        statusComboBox.addValueChangeListener(e -> {
            if (currentChat != null && e.getValue() != null && e.isFromClient()) {
                chatService.updateStatus(currentChat.getId(), e.getValue());
                currentChat = rebuildChat(currentChat, e.getValue(), false, currentChat.getUnreadCount());
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
            currentChat = rebuildChat(currentChat, newStatus, false, 0);
            notifyChatUpdated();
            updateHeaderState();
        });

        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.addClickListener(e -> {
            if (currentChat == null) {
                return;
            }
            chatService.updateStatus(currentChat.getId(), ChatStatus.CLOSED);
            currentChat = rebuildChat(currentChat, ChatStatus.CLOSED, false, currentChat.getUnreadCount());
            notifyChatUpdated();
            updateHeaderState();
        });

        metaInfo.addClassName("conversation-meta");
        title.addClassName("conversation-title");

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

    private void updateHeaderState() {
        boolean hasChat = currentChat != null;
        statusComboBox.setEnabled(hasChat);
        markProcessedButton.setEnabled(hasChat);
        closeButton.setEnabled(hasChat);
        if (!hasChat) {
            messagesContainer.removeAll();
            messagesContainer.add(new Span("Оберіть діалог"));
            metaInfo.setText("");
            title.setText("Діалог");
            return;
        }
        statusComboBox.setValue(currentChat.getStatus());
        title.setText(StringUtils.hasText(currentChat.getDisplayName()) ? currentChat.getDisplayName() : currentChat.getContactEmail());
        metaInfo.setText(buildMetaText());
    }

    private String buildMetaText() {
        if (CollectionUtils.isEmpty(loadedMessages)) {
            return "";
        }
        ChatMessageDetailDto last = loadedMessages.get(0);
        String lastTime = last.getSentAt() != null ? dateTimeFormatter.format(last.getSentAt()) : "";
        return String.format("Останнє повідомлення: %s • %d повідомлень", lastTime, loadedMessages.size());
    }

    private void loadMessages() {
        if (currentChat == null) {
            return;
        }
        messagesContainer.removeAll();
        messagesContainer.add(new Span("Завантаження..."));
        try {
            List<ChatMessageDetailDto> batch = chatService.findChatMessages(currentChat.getId(), null);
            loadedMessages.clear();
            loadedMessages.addAll(batch);
            renderMessages();
            metaInfo.setText(buildMetaText());
        } catch (MessagingException e) {
            messagesContainer.removeAll();
            messagesContainer.add(new Span("Не вдалося завантажити тему."));
        }
    }

    private void renderMessages() {
        messagesContainer.removeAll();
        if (loadedMessages.isEmpty()) {
            messagesContainer.add(new Span("Немає повідомлень у темі"));
            return;
        }
        LocalDate lastDate = null;
        for (ChatMessageDetailDto message : loadedMessages) {
            LocalDate date = Optional.ofNullable(message.getSentAt())
                    .map(instant -> instant.atZone(ZoneId.systemDefault()).toLocalDate())
                    .orElse(null);
            if (!Objects.equals(date, lastDate)) {
                messagesContainer.add(new MessageDateDivider(date != null ? dateFormatter.format(date) : ""));
                lastDate = date;
            }
            messagesContainer.add(new MessageBubble(message));
        }
    }

    private ChatListItemDto rebuildChat(ChatListItemDto chat, ChatStatus status, boolean hasUnprocessed, int unreadCount) {
        if (chat == null) {
            return null;
        }
        return ChatListItemDto.builder()
                .id(chat.getId())
                .contactEmail(chat.getContactEmail())
                .displayName(chat.getDisplayName())
                .status(status)
                .hasUnprocessed(hasUnprocessed)
                .unreadCount(unreadCount)
                .lastMessageAt(chat.getLastMessageAt())
                .lastSnippet(chat.getLastSnippet())
                .hasAttachments(chat.isHasAttachments())
                .build();
    }

    private void notifyChatUpdated() {
        if (chatUpdateListener != null) {
            chatUpdateListener.accept(currentChat);
        }
    }
}
