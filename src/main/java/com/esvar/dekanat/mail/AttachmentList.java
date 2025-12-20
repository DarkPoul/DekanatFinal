package com.esvar.dekanat.mail;

import com.esvar.dekanat.mail.dto.AttachmentDto;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.springframework.util.unit.DataSize;

import java.util.List;
import java.util.Objects;

public class AttachmentList extends VerticalLayout {

    public AttachmentList(List<AttachmentDto> attachments) {
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        addClassName("attachment-list");

        attachments.stream().filter(Objects::nonNull).forEach(this::addAttachment);
    }

    private void addAttachment(AttachmentDto attachment) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.addClassName("attachment-row");

        Icon icon = iconForMime(attachment.getMimeType());
        icon.addClassName("attachment-icon");

        Text name = new Text(attachment.getFilename() != null ? attachment.getFilename() : "Вкладення");
        Text size = new Text(formatSize(attachment.getSizeBytes()));

        Button download = new Button("Завантажити", new Icon(VaadinIcon.DOWNLOAD_ALT));
        download.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        download.addClickListener(e -> download.getUI().ifPresent(ui -> ui.getPage().open("/mail/attachments/" + attachment.getId())));

        row.add(icon, name, size, download);
        row.setFlexGrow(1, name);
        add(row);
    }

    private Icon iconForMime(String mime) {
        if (mime != null && mime.toLowerCase().contains("pdf")) {
            return VaadinIcon.FILE_PRESENTATION.create();
        }
        if (mime != null && mime.toLowerCase().contains("image")) {
            return VaadinIcon.PICTURE.create();
        }
        if (mime != null && mime.toLowerCase().contains("zip")) {
            return VaadinIcon.ARCHIVE.create();
        }
        return VaadinIcon.FILE_TEXT.create();
    }

    private String formatSize(Long sizeBytes) {
        if (sizeBytes == null || sizeBytes <= 0) {
            return "";
        }
        return DataSize.ofBytes(sizeBytes).toMegabytes() >= 1
                ? DataSize.ofBytes(sizeBytes).toMegabytes() + " MB"
                : DataSize.ofBytes(sizeBytes).toKilobytes() + " KB";
    }
}
