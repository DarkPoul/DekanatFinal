package com.esvar.dekanat.mailer;

import com.esvar.dekanat.user.UserModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String defaultFrom;

    public EmailService(JavaMailSender mailSender,
                        @Value("${mail.default-from:}") String defaultFrom) {
        this.mailSender = mailSender;
        this.defaultFrom = defaultFrom;
    }

    public void sendEmail(String recipient, String subject, String body) {
        if (!StringUtils.hasText(recipient)) {
            throw new IllegalArgumentException("Не вказано email одержувача.");
        }
        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("Потрібно вказати тему листа.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(defaultFrom)) {
            message.setFrom(defaultFrom);
        }
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body == null ? "" : body);
        mailSender.send(message);
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

    private String composeFullName(UserModel user) {
        return java.util.List.of(user.getLastname(), user.getFirstname(), user.getPatronymic())
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.joining(" "));
    }
}
