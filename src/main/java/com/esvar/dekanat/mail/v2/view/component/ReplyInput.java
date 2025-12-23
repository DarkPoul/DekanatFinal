package com.esvar.dekanat.mail.v2.view.component;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.shared.Registration;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ReplyInput extends Div {

    private TextArea textArea = new TextArea();
    private MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
    private final Upload upload = new Upload(buffer);
    private final Button send = new Button(new Icon(VaadinIcon.PAPERPLANE));

    public ReplyInput() {
        addClassName("reply-input");
        setWidthFull();
        buildLayout();
        configureInteractions();
    }

    private void buildLayout() {
        textArea.setPlaceholder("Відповісти…");
        textArea.setClearButtonVisible(false);
        textArea.setWidthFull();
        textArea.getElement().setAttribute("rows", "2");
        textArea.setMaxHeight("220px");
        textArea.setMinHeight("64px");
        textArea.addClassName("reply-textarea");

        upload.setDropAllowed(true);
        upload.setMaxFiles(8);
        upload.setAutoUpload(true);
        upload.getElement().setAttribute("title", "Додати вкладення або перетягнути файли");
        upload.addClassName("reply-upload");

        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        send.addClickListener(e -> emitSend());
        send.setEnabled(false);
        send.getElement().setProperty("title", "Відправити (Enter)");
        send.addClassName("reply-send");

        FlexLayout layout = new FlexLayout();
        layout.setWidthFull();
        layout.setAlignItems(FlexLayout.Alignment.END);
        layout.setJustifyContentMode(FlexLayout.JustifyContentMode.BETWEEN);
        layout.add(upload, textArea, send);
        layout.addClassName("reply-row");
        layout.setFlexGrow(1, textArea);
        textArea.getStyle().set("flex", "1 1 100%");

        add(layout);
        attachAutoGrow();
    }

    private void configureInteractions() {
        textArea.addValueChangeListener(e -> updateSendEnabled());
        textArea.addKeyDownListener(Key.ENTER, event -> {
            if (event.isShiftKey()) {
                return;
            }
            event.preventDefault();
            emitSend();
        });

        upload.addSucceededListener(e -> updateSendEnabled());
        upload.addFileRemovedListener(e -> updateSendEnabled());
        upload.addFileRejectedListener(e -> Notification.show(e.getErrorMessage()));
    }

    private void emitSend() {
        String value = textArea.getValue();
        List<MultipartFile> files = toMultipartFiles();
        if (!StringUtils.hasText(value) && files.isEmpty()) {
            return;
        }
        fireEvent(new SendEvent(this, value.trim(), files));
    }

    private List<MultipartFile> toMultipartFiles() {
        List<MultipartFile> result = new ArrayList<>();
        for (String filename : buffer.getFiles()) {
            try (InputStream inputStream = buffer.getInputStream(filename)) {
                MockMultipartFile file = new MockMultipartFile(
                        filename,
                        filename,
                        buffer.getFileData(filename).getMimeType(),
                        inputStream.readAllBytes()
                );
                result.add(file);
            } catch (IOException ignored) {
            }
        }
        return result;
    }

    private void updateSendEnabled() {
        boolean hasText = StringUtils.hasText(textArea.getValue());
        boolean hasFiles = !buffer.getFiles().isEmpty();
        send.setEnabled(hasText || hasFiles);
    }

    public void reset() {
        textArea.clear();
        buffer = new MultiFileMemoryBuffer();
        upload.setReceiver(buffer);
        upload.clearFileList();
        updateSendEnabled();
    }

    public Registration addSendListener(ComponentEventListener<SendEvent> listener) {
        return addListener(SendEvent.class, listener);
    }

    private void attachAutoGrow() {
        getElement().executeJs("""
                const el = this.querySelector('vaadin-text-area');
                if(!el) return;
                const textarea = el.inputElement;
                const maxHeight = 220;
                const resize = () => {
                  textarea.style.height = 'auto';
                  textarea.style.height = Math.min(textarea.scrollHeight, maxHeight) + 'px';
                };
                textarea.addEventListener('input', resize);
                requestAnimationFrame(resize);
                """);
    }

    public static class SendEvent extends ComponentEvent<ReplyInput> {
        private final String text;
        private final List<MultipartFile> attachments;

        public SendEvent(ReplyInput source, String text, List<MultipartFile> attachments) {
            super(source, false);
            this.text = text;
            this.attachments = attachments;
        }

        public String getText() {
            return text;
        }

        public List<MultipartFile> getAttachments() {
            return attachments;
        }
    }
}
