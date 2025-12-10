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
        String subject = "Доступ до системи Деканат";
        String role = userModel.getRole();
        String body = switch (role == null ? "" : role) {
            case "ROLE_DEPARTMENT" -> buildTeacherEmailBody(userModel, rawPassword);
            case "ROLE_ADMIN" -> buildAdminEmailBody(userModel, rawPassword);
            default -> buildDekanatEmailBody(userModel, rawPassword);
        };

        sendEmail(userModel.getEmail(), subject, body);
    }

    private String buildDekanatEmailBody(UserModel userModel, String rawPassword) {
        String fullName = composeFullName(userModel);
        return """
        Шановний(-а) %s!

        Вас підключено до інформаційної системи АСУ «Деканат» у якості методиста.
        Для входу в систему, будь ласка, скористайтеся наступним посиланням:
        https://dekanat.ntu.edu.ua/

        Ваші облікові дані для входу:
        Логін: %s
        Пароль: %s

        У разі виникнення запитань або технічних труднощів
        звертайтеся до команди підтримки АСУ «Деканат»
        за контактами, наданими у системі.

        З повагою,
        Команда АСУ «Деканат»
        dekanat.support@ntu.edu.ua
        """.stripIndent().formatted(resolveDisplayName(fullName, userModel), userModel.getEmail(), rawPassword);
    }

    private String buildTeacherEmailBody(UserModel userModel, String rawPassword) {
        String fullName = composeFullName(userModel);
        return """
        Шановний(а) %s!

        Вам надано доступ до АСУ «Деканат» у ролі викладача для внесення результатів оцінювання.

        Дані для входу:

            Адреса системи: https://dekanat.ntu.edu.ua/

            Логін: %s

            Пароль: %s

        Порядок роботи з системою

            Увійдіть до системи, використовуючи надані облікові дані.

            Оберіть спеціальність, курс та групу, у якій Ви викладаєте.

            Оберіть дисципліну та тип контролю.

            Після завантаження відомості внесіть оцінки у колонку «Оцінка»:

                0–30 балів — для першого та другого модульного контролю,

                60–100 балів — для заліку, екзамену та інших підсумкових форм контролю.

            Після внесення оцінок натисніть «Зберегти».

        Увага!

        Для генерування PDF-файлу відомості з метою її подальшого друку необхідно затвердити оцінки, натиснувши кнопку «Затвердити».

        Після затвердження оцінок стане доступною можливість сформувати відомість у форматі PDF — активується кнопка «Друк відомості».

        При натисканні кнопки «Друк відомості» з’явиться вікно для внесення даних про викладача, який здійснював поточне оцінювання (другого викладача, що вів лабораторні, практичні, семінарські заняття тощо).

        Звертаємо увагу, що першим (ГОЛОВНИМ) викладачем у відомості буде той, хто безпосередньо генерує документ.

        Наполегливо рекомендуємо не передавати лаври першості своїм менш іменитим колегам 😉

        Внесення змін до вже затверджених оцінок неможливе. Якщо вам необхідно внести коригування після затвердження, зверніться до методиста факультету/інституту, до якого належить група — вони мають можливість розблокувати оцінки.

        Звертаємо вашу увагу!
        З міркувань інформаційної безпеки категорично заборонено передавати свої облікові дані стороннім особам або надавати доступ до системи під власним акаунтом.

        У разі виявлення таких випадків доступ до облікового запису буде негайно заблоковано до моменту з’ясування всіх обставин.
        Повторні порушення можуть призвести до повного анулювання доступу до системи та подальшої службової перевірки.

        Просимо поставитися до цього з максимальною відповідальністю. Система фіксує всі дії користувачів і уникнути ідентифікації порушника неможливо.

        У разі виникнення запитань чи технічних складнощів звертайтесь на адресу підтримки:
        dekanat.support@ntu.edu.ua
        З повагою,
        Команда системи «Деканат»
        """.stripIndent().formatted(resolveDisplayName(fullName, userModel), userModel.getEmail(), rawPassword);
    }

    private String buildAdminEmailBody(UserModel userModel, String rawPassword) {
        String fullName = composeFullName(userModel);
        return """
        Шановний(а) %s!

        Для вас створено адміністраторський обліковий запис у системі «Деканат».

        Дані для входу:
        Адреса системи: https://dekanat.ntu.edu.ua/
        Логін: %s
        Пароль: %s

        З міркувань безпеки не передавайте ваші облікові дані третім особам.
        """.stripIndent().formatted(resolveDisplayName(fullName, userModel), userModel.getEmail(), rawPassword);
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

    private String resolveDisplayName(String fullName, UserModel userModel) {
        return StringUtils.hasText(fullName) ? fullName : userModel.getEmail();
    }
}
