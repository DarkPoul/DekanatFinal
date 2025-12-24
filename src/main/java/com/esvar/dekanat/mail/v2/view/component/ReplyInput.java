package com.esvar.dekanat.mail.v2.view.component;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.shared.Registration;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ReplyInput extends Div {

    private static final int MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final String[] ACCEPTED_IMAGE_TYPES = new String[]{
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/gif",
            "image/webp"
    };

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
        upload.setMaxFileSize(MAX_IMAGE_SIZE_BYTES);
        upload.setAcceptedFileTypes(ACCEPTED_IMAGE_TYPES);
        upload.getElement().setAttribute("title", "Додати вкладення або перетягнути файли");
        upload.addClassName("reply-upload");
        upload.setUploadButton(createUploadButton());
        upload.setDropLabel(new Span("Перетягніть зображення (PNG/JPG/GIF/WEBP) " + readableMaxSize()));

        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        send.addClickListener(e -> emitSend());
        send.setEnabled(false);
        send.getElement().setProperty("title", "Відправити (Enter)");
        send.addClassName("reply-send");

        FlexLayout layout = new FlexLayout();
        layout.setWidthFull();
        layout.setAlignItems(FlexLayout.Alignment.CENTER);
        layout.setJustifyContentMode(FlexLayout.JustifyContentMode.START);
        layout.add(upload, textArea, send);
        layout.addClassName("reply-row");
        layout.setFlexGrow(1, textArea);
        textArea.getStyle().set("flex", "1 1 100%");

        add(layout);
        attachAutoGrow();
    }

    private Button createUploadButton() {
        Button button = new Button("Додати файл", new Icon(VaadinIcon.PAPERCLIP));
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        button.setWidthFull();
        return button;
    }

    private void configureInteractions() {
        textArea.addValueChangeListener(e -> updateSendEnabled());

        // Enter => send, Shift+Enter => newline
        textArea.getElement().executeJs("""
        const host = this;
        const ta = host.inputElement; // vaadin-text-area internal textarea
        if (!ta) return;

        ta.addEventListener('keydown', (ev) => {
          if (ev.key === 'Enter' && !ev.shiftKey && !ev.isComposing) {
            ev.preventDefault();
            host.$server.onEnterSend();
          }
        });
    """);

        upload.addSucceededListener(e -> updateSendEnabled());
        upload.addFileRemovedListener(e -> updateSendEnabled());
        upload.addFileRejectedListener(e -> {
            String error = e.getErrorMessage();
            if (!StringUtils.hasText(error)) {
                error = "Невдале завантаження зображення. Перевірте формат та розмір (" + readableMaxSize() + ")";
            }
            Notification.show(error);
        });
    }


    @ClientCallable
    private void onEnterSend() {
        // Це викликається з браузера -> ми вже на UI thread
        emitSend();
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
                byte[] content = inputStream.readAllBytes();
                MultipartFile multipartFile = new InMemoryMultipartFile(
                        filename,
                        filename,
                        buffer.getFileData(filename).getMimeType(),
                        content
                );
                result.add(multipartFile);
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

    private String readableMaxSize() {
        double value = MAX_IMAGE_SIZE_BYTES;
        String[] units = {"Б", "КБ", "МБ"};
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return String.format("%.0f %s", value, units[unit]);
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

    private static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content != null ? content : new byte[0];
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), content);
        }
    }
}
