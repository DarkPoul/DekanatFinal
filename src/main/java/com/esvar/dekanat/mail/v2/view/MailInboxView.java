package com.esvar.dekanat.mail.v2.view;

import com.esvar.dekanat.mail.v2.dto.MessageDto;
import com.esvar.dekanat.mail.v2.dto.ThreadListItemDto;
import com.esvar.dekanat.mail.v2.entity.MailThreadEntity;
import com.esvar.dekanat.mail.v2.service.AttachmentUploadException;
import com.esvar.dekanat.mail.v2.service.MessageService;
import com.esvar.dekanat.mail.v2.service.SubjectNormalizer;
import com.esvar.dekanat.mail.v2.service.SendMailService;
import com.esvar.dekanat.mail.v2.service.ThreadService;
import com.esvar.dekanat.mail.v2.view.component.ChatHeader;
import com.esvar.dekanat.mail.v2.view.component.ChatListItem;
import com.esvar.dekanat.mail.v2.view.component.MessageBubble;
import com.esvar.dekanat.mail.v2.view.component.ReplyInput;
import com.esvar.dekanat.view.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;

import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Route(value = "mail", layout = MainLayout.class)
@PageTitle("Пошта")
@CssImport("./styles/mail-inbox.css")
@RolesAllowed("ROLE_ADMIN")
public class MailInboxView extends VerticalLayout {

    private final ThreadService threadService;
    private final MessageService messageService;
    private final SendMailService sendMailService;

    private final Div shell = new Div();
    private final VirtualList<ThreadListItemDto> threadList = new VirtualList<>();
    private final TextField nameFilter = new TextField();
    private final TextField emailFilter = new TextField();
    private final TextField orgFilter = new TextField();
    private final Select<MailThreadEntity.ThreadStatus> statusFilter = new Select<>();
    private CallbackDataProvider<ThreadListItemDto, Void> dataProvider;

    private final Div leftPanel = new Div();
    private final Div placeholderCard = new Div();
    private final Div conversationPanel = new Div();
    private final ChatHeader chatHeader = new ChatHeader();
    private final VerticalLayout messagesLayout = new VerticalLayout();
    private final Button loadMoreButton = new Button("Завантажити ще");
    private final Scroller messageScroller = new Scroller();
    private final ReplyInput replyInput = new ReplyInput();

    private final int pageSize = 10;
    private ThreadListItemDto selectedThread;
    private Instant beforeCursor;
    private final AtomicBoolean loadingMessages = new AtomicBoolean(false);
    private final Set<Long> renderedMessageIds = new java.util.LinkedHashSet<>();

    public MailInboxView(ThreadService threadService,
                         MessageService messageService,
                         SendMailService sendMailService) {
        this.threadService = threadService;
        this.messageService = messageService;
        this.sendMailService = sendMailService;
        configureView();
        buildLayout();
        configureFilters();
        configureList();
        configureConversation();
        configureShortcuts();
    }

    private void configureView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("mail-view");
    }

    private void buildLayout() {
        shell.addClassNames("mail-shell", "collapsed");
        shell.setWidthFull();
        shell.setHeightFull();
        shell.add(buildLeftPanel(), buildConversationPanel());
        add(shell);
    }

    private Component buildLeftPanel() {
        nameFilter.setPlaceholder("ПІБ/Назва");
        emailFilter.setPlaceholder("Email");
        orgFilter.setPlaceholder("Каф/Фак/Інст");
        Stream.of(nameFilter, emailFilter, orgFilter).forEach(field -> {
            field.addThemeVariants(TextFieldVariant.LUMO_SMALL);
            field.setClearButtonVisible(true);
            field.setValueChangeMode(ValueChangeMode.LAZY);
            field.setValueChangeTimeout(400);
            field.setHeight("36px");
            field.setWidthFull();
        });

        statusFilter.setItems((MailThreadEntity.ThreadStatus[]) MailThreadEntity.ThreadStatus.values());
        statusFilter.setPlaceholder("Status");
        statusFilter.setEmptySelectionAllowed(true);
        statusFilter.setWidth("140px");
        statusFilter.setHeight("36px");

        HorizontalLayout filters = new HorizontalLayout(nameFilter, emailFilter, orgFilter, statusFilter);
        filters.setPadding(false);
        filters.setSpacing(true);
        filters.setWidthFull();
        filters.addClassName("filter-row");
        filters.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        threadList.setRenderer(new ComponentRenderer<>(this::renderThread));
        threadList.addClassName("thread-virtual-list");
        threadList.setHeightFull();
        threadList.setWidthFull();
        threadList.getElement().setAttribute("tabindex", "0");

        placeholderCard.addClassName("mail-placeholder");
        placeholderCard.add(new Span("💬"), new Span("Оберіть чат для перегляду листування"));

        leftPanel.addClassName("mail-left");
        leftPanel.setHeightFull();
        leftPanel.add(filters, threadList, placeholderCard);
        return leftPanel;
    }

    private Component buildConversationPanel() {
        conversationPanel.addClassName("mail-conversation");
        conversationPanel.setHeightFull();
        conversationPanel.setVisible(false);

        messagesLayout.setPadding(false);
        messagesLayout.setSpacing(false);
        messagesLayout.setWidthFull();
        messagesLayout.addClassName("message-stack");

        loadMoreButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        loadMoreButton.addClassName("load-more");
        loadMoreButton.setWidthFull();
        loadMoreButton.addClickListener(e -> loadMessages(true));

        Div messagesWrapper = new Div();
        messagesWrapper.addClassName("messages-wrapper");
        messagesWrapper.add(loadMoreButton, messagesLayout);

        messageScroller.setContent(messagesWrapper);
        messageScroller.addClassName("message-scroller");

        VerticalLayout conversation = new VerticalLayout(chatHeader, messageScroller, replyInput);
        conversation.setPadding(false);
        conversation.setSpacing(false);
        conversation.setSizeFull();
        conversation.setFlexGrow(1, messageScroller);
        conversation.setAlignItems(FlexComponent.Alignment.STRETCH);

        conversationPanel.add(conversation);
        return conversationPanel;
    }

    private void configureFilters() {
        Runnable refresher = () -> {
            if (dataProvider != null) {
                dataProvider.refreshAll();
            }
        };
        nameFilter.addValueChangeListener(e -> refresher.run());
        emailFilter.addValueChangeListener(e -> refresher.run());
        orgFilter.addValueChangeListener(e -> refresher.run());
        statusFilter.addValueChangeListener(e -> refresher.run());
    }

    private void configureList() {
        dataProvider = new CallbackDataProvider<>(this::fetchThreads, this::countThreads);
        threadList.setDataProvider(dataProvider);
    }



    private void configureConversation() {
        chatHeader.addStatusChangeListener(event -> changeStatus(event.getStatus()));
        chatHeader.addRenameListener(event -> openRenameDialog());
        replyInput.addSendListener(event -> {
            if (selectedThread == null) {
                Notification.show("Обери чат, щоб відповісти");
                return;
            }
            sendReply(event.getText(), event.getAttachments());
        });
        loadMoreButton.setVisible(false);
    }

    private void configureShortcuts() {
        Shortcuts.addShortcutListener(threadList, () -> navigateList(-1), Key.ARROW_UP);
        Shortcuts.addShortcutListener(threadList, () -> navigateList(1), Key.ARROW_DOWN);
        Shortcuts.addShortcutListener(threadList, this::openSelectedFromKeyboard, Key.ENTER);
    }

    private Stream<ThreadListItemDto> fetchThreads(Query<ThreadListItemDto, Void> query) {
        int limit = query.getLimit() > 0 ? query.getLimit() : pageSize;
        return threadService.findThreads(
                        nameFilter.getValue(),
                        emailFilter.getValue(),
                        orgFilter.getValue(),
                        statusFilter.getValue(),
                        query.getOffset(),
                        limit)
                .stream();
    }

    private int countThreads(Query<ThreadListItemDto, Void> query) {
        return (int) threadService.countThreads(
                nameFilter.getValue(),
                emailFilter.getValue(),
                orgFilter.getValue(),
                statusFilter.getValue());
    }

    private Component renderThread(ThreadListItemDto thread) {
        boolean active = selectedThread != null && selectedThread.getThreadId().equals(thread.getThreadId());
        ChatListItem item = new ChatListItem(thread, active);
        item.addClickListener(e -> openThread(thread));
        item.addRenameListener(e -> {
            openThread(thread);
            openRenameDialog();
        });
        return item;
    }

    private void openThread(ThreadListItemDto thread) {
        selectedThread = thread;
        beforeCursor = null;
        renderedMessageIds.clear();
        messagesLayout.removeAll();
        loadMoreButton.setVisible(false);
        chatHeader.setThread(thread);
        threadService.markViewed(thread.getThreadId());
        getShell().removeClassName("collapsed");
        conversationPanel.setVisible(true);
        placeholderCard.setVisible(false);
        loadMessages(false);
        dataProvider.refreshAll();
    }

    private void loadMessages(boolean prepend) {
        if (selectedThread == null || !loadingMessages.compareAndSet(false, true)) {
            return;
        }
        try {
            Instant cursor = prepend ? beforeCursor : null;
            List<MessageDto> batch = messageService.loadMessages(selectedThread.getThreadId(), cursor, pageSize);
            if (!batch.isEmpty()) {
                beforeCursor = batch.get(0).getSentAt();
            }
            boolean moreAvailable = batch.size() == pageSize && beforeCursor != null;
            loadMoreButton.setVisible(moreAvailable);

            if (prepend) {
                preserveScrollWhile(() -> prependMessages(batch));
            } else {
                appendMessages(batch);
                scrollToBottom();
            }
        } finally {
            loadingMessages.set(false);
        }
    }

    private void appendMessages(List<MessageDto> messages) {
        for (MessageDto message : messages) {
            if (message.getId() != null && renderedMessageIds.contains(message.getId())) {
                continue;
            }
            MessageBubble bubble = new MessageBubble(message);
            messagesLayout.add(bubble);
            if (message.getId() != null) {
                renderedMessageIds.add(message.getId());
            }
        }
    }

    private void prependMessages(List<MessageDto> messages) {
        int index = 0;
        for (MessageDto message : messages) {
            if (message.getId() != null && renderedMessageIds.contains(message.getId())) {
                continue;
            }
            MessageBubble bubble = new MessageBubble(message);
            messagesLayout.addComponentAtIndex(index, bubble);
            if (message.getId() != null) {
                renderedMessageIds.add(message.getId());
            }
            index++;
        }
    }

    private void sendReply(String text, List<org.springframework.web.multipart.MultipartFile> attachments) {
        if (!StringUtils.hasText(text) && attachments.isEmpty()) {
            return;
        }
        try {
            sendMailService.send(threadService.findById(selectedThread.getThreadId()).orElseThrow(),
                    text,
                    SubjectNormalizer.buildReplySubject(selectedThread.getLastSubject()),
                    attachments);
        } catch (AttachmentUploadException ex) {
            Notification.show(ex.getMessage());
            return;
        }
        replyInput.reset();
        Notification.show("Відправлено");
        List<MessageDto> latest = messageService.loadMessages(selectedThread.getThreadId(), null, 1);
        appendMessages(latest);
        scrollToBottom();
    }

    private void changeStatus(MailThreadEntity.ThreadStatus status) {
        if (selectedThread == null) {
            return;
        }
        threadService.updateStatus(selectedThread.getThreadId(), status);
        selectedThread.setStatus(status);
        chatHeader.setThread(selectedThread);
        dataProvider.refreshAll();
    }

    private void openRenameDialog() {
        if (selectedThread == null) {
            return;
        }
        chatHeader.openRenameDialog(selectedThread, (displayName, orgUnit) -> {
            threadService.signContact(selectedThread.getContactId(), displayName, orgUnit);
            selectedThread.setDisplayName(displayName);
            selectedThread.setOrgUnitText(orgUnit);
            selectedThread.setSigned(true);
            chatHeader.setThread(selectedThread);
            dataProvider.refreshAll();
        });
    }

    private void clearConversation() {
        selectedThread = null;
        beforeCursor = null;
        renderedMessageIds.clear();
        messagesLayout.removeAll();
        loadMoreButton.setVisible(false);
        conversationPanel.setVisible(false);
        placeholderCard.setVisible(true);
        getShell().addClassName("collapsed");
    }

    private void navigateList(int delta) {
        int count = dataProvider.size(new Query<>());
        if (count == 0) {
            return;
        }
        int currentIndex = selectedThread != null ? findThreadIndex(selectedThread.getThreadId(), count) : -1;
        int nextIndex = Math.min(Math.max(currentIndex + delta, 0), count - 1);
        ThreadListItemDto next = fetchSingle(nextIndex);
        if (next != null) {
            openThread(next);
            threadList.scrollToIndex(nextIndex);
        }
    }

    private int findThreadIndex(Long threadId, int count) {
        for (int i = 0; i < count; i++) {
            ThreadListItemDto dto = fetchSingle(i);
            if (dto != null && dto.getThreadId().equals(threadId)) {
                return i;
            }
        }
        return -1;
    }

    private ThreadListItemDto fetchSingle(int index) {
        return dataProvider.fetch(new Query<>(index, 1, new ArrayList<>(), null, null)).findFirst().orElse(null);
    }

    private void openSelectedFromKeyboard() {
        if (selectedThread == null) {
            ThreadListItemDto first = fetchSingle(0);
            if (first != null) {
                openThread(first);
                threadList.scrollToIndex(0);
            }
        } else {
            openThread(selectedThread);
        }
    }

    private void preserveScrollWhile(Runnable action) {
        messageScroller.getElement().executeJs("return this.scrollTop;")
                .then(Double.class, top -> {
                    messageScroller.getElement().executeJs("return this.scrollHeight;")
                            .then(Double.class, height -> {

                                action.run();

                                getUI().ifPresent(ui -> ui.beforeClientResponse(messageScroller, ctx ->
                                        messageScroller.getElement().executeJs(
                                                """
                                                const prevTop = $0;
                                                const prevHeight = $1;
                                                const delta = this.scrollHeight - prevHeight;
                                                this.scrollTop = prevTop + delta;
                                                """,
                                                top, height
                                        )));
                            });
                });
    }



    private void scrollToBottom() {
        getUI().ifPresent(ui -> ui.beforeClientResponse(messageScroller,
                ctx -> messageScroller.getElement().executeJs("this.scrollTop = this.scrollHeight;")));
    }

    private Div getShell() {
        return shell;
    }
}
