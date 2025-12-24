package com.esvar.dekanat.mail.v2.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AttachmentStorageService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/gif",
            "image/webp"
    );

    private static final DateTimeFormatter FILENAME_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Value("${mail.attachments.dir:uploads/mail}")
    private String attachmentsDir;

    @Value("${mail.attachments.max-size-bytes:5242880}")
    private long maxSizeBytes;

    public StoredAttachment storeImage(MultipartFile file) {
        validateImage(file);
        Path targetDir = prepareDirectory();
        String originalFilename = safeFilename(file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String generatedName = buildGeneratedName(extension);
        Path destination = targetDir.resolve(generatedName);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AttachmentUploadException("Не вдалося зберегти зображення на диск", e);
        }
        String contentType = detectContentType(file);
        long size = file.getSize();
        return new StoredAttachment(originalFilename, contentType, destination.toString(), size);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AttachmentUploadException("Файл зображення відсутній або порожній");
        }
        if (file.getSize() <= 0) {
            throw new AttachmentUploadException("Розмір зображення дорівнює нулю");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new AttachmentUploadException("Зображення перевищує максимальний розмір " + humanReadable(maxSizeBytes));
        }
        String contentType = detectContentType(file);
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new AttachmentUploadException("Підтримуються лише зображення PNG, JPEG, GIF або WEBP");
        }
        ensureImageDecodable(file);
    }

    private void ensureImageDecodable(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            if (ImageIO.read(inputStream) == null) {
                throw new AttachmentUploadException("Файл не схожий на справжнє зображення");
            }
        } catch (IOException e) {
            throw new AttachmentUploadException("Не вдалося перевірити вміст зображення", e);
        }
    }

    private Path prepareDirectory() {
        Path dir = Paths.get(attachmentsDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new AttachmentUploadException("Не вдалося підготувати каталог для збереження зображення", e);
        }
        return dir;
    }

    private String detectContentType(MultipartFile file) {
        String declared = file.getContentType();
        if (StringUtils.hasText(declared) && ALLOWED_IMAGE_TYPES.contains(declared.toLowerCase(Locale.ROOT))) {
            return declared.toLowerCase(Locale.ROOT);
        }
        try (InputStream inputStream = file.getInputStream()) {
            String probed = URLConnection.guessContentTypeFromStream(inputStream);
            if (StringUtils.hasText(probed)) {
                return probed.toLowerCase(Locale.ROOT);
            }
        } catch (IOException ignored) {
        }
        if (StringUtils.hasText(declared)) {
            return declared.toLowerCase(Locale.ROOT);
        }
        return "application/octet-stream";
    }

    private String safeFilename(String filename) {
        String cleaned = StringUtils.cleanPath(StringUtils.hasText(filename) ? filename : "image");
        String onlyName = StringUtils.getFilename(cleaned);
        if (!StringUtils.hasText(onlyName)) {
            return "image";
        }
        return onlyName;
    }

    private String buildGeneratedName(String extension) {
        String timestamp = LocalDateTime.now().format(FILENAME_DATE_FORMATTER);
        String randomPart = UUID.randomUUID().toString().replace("-", "");
        if (StringUtils.hasText(extension)) {
            return timestamp + "-" + randomPart + "." + extension;
        }
        return timestamp + "-" + randomPart;
    }

    private String humanReadable(long bytes) {
        double value = (double) bytes;
        String[] units = {"Б", "КБ", "МБ", "ГБ"};
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unitIndex]);
    }

    public record StoredAttachment(String originalFilename,
                                   String contentType,
                                   String storagePath,
                                   long size) {
    }
}
