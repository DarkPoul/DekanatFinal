package com.esvar.dekanat.mailer;

import com.esvar.dekanat.user.UserModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final String defaultFrom;

    public EmailService(JavaMailSender mailSender,
                        MailProperties mailProperties,
                        @Value("${mail.default-from:}") String defaultFrom) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.defaultFrom = defaultFrom;
    }

    public void sendEmail(String recipient, String subject, String body) {
        if (!StringUtils.hasText(recipient)) {
            throw new IllegalArgumentException("Не вказано email одержувача.");
        }
        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("Потрібно вказати тему листа.");
        }

        if (!StringUtils.hasText(mailProperties.getHost())) {
            throw new MailPreparationException("Налаштуйте SMTP сервер: MAIL_HOST не задано.");
        }
        if (mailProperties.getPort() == null) {
            throw new MailPreparationException("Налаштуйте SMTP сервер: MAIL_PORT не задано.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(resolveFromAddress());
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body == null ? "" : body);
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            LOGGER.warn("Не вдалося надіслати email користувачу {}: {}", recipient, ex.getMessage());
            throw ex;
        }
    }

    public void sendWelcomeEmail(UserModel userModel, String rawPassword) {
        String fullName = composeFullName(userModel);
        String body = """
                Доброго дня, %s!

                Вам створено обліковий запис у системі "Деканат".
                Логін: %s
                Пароль: %s

                Після першого входу, будь ласка, змініть пароль у налаштуваннях.
                """.stripIndent().formatted(fullName, userModel.getEmail(), rawPassword);

        sendEmail(userModel.getEmail(), "Доступ до системи Деканат", body);
    }

    private String resolveFromAddress() {
        if (StringUtils.hasText(defaultFrom)) {
            return defaultFrom;
        }
        if (StringUtils.hasText(mailProperties.getUsername())) {
            return mailProperties.getUsername();
        }
        throw new MailPreparationException("Налаштуйте адресу відправника: MAIL_DEFAULT_FROM або MAIL_USERNAME не задано.");
    }

    private String composeFullName(UserModel user) {
        return java.util.List.of(user.getLastname(), user.getFirstname(), user.getPatronymic())
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.joining(" "));
    }
}
