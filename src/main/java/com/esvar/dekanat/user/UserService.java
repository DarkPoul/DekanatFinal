package com.esvar.dekanat.user;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
public class UserService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int PASSWORD_LENGTH = 10;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    public List<UserModel> findAll() {
        return userRepository.findAll();
    }

    public String createUser(UserModel userModel) {
        userRepository.findByEmail(userModel.getEmail()).ifPresent(existing -> {
            throw new IllegalArgumentException("Користувач з такою поштою вже існує");
        });

        String rawPassword = generatePassword();
        userModel.setPassword(passwordEncoder.encode(rawPassword));
        userModel.setEnabled(true);

        UserModel savedUser = userRepository.save(userModel);
        sendWelcomeEmail(savedUser, rawPassword);

        return rawPassword;
    }

    private void sendWelcomeEmail(UserModel userModel, String rawPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userModel.getEmail());
        message.setSubject("Доступ до системи Деканат");
        message.setText(String.format("""
                Доброго дня, %s!

                Вам створено обліковий запис у системі "Деканат".
                Логін: %s
                Пароль: %s

                Після першого входу, будь ласка, змініть пароль у налаштуваннях.
                """.stripIndent(),
                formatFullName(userModel),
                userModel.getEmail(),
                rawPassword));
        mailSender.send(message);
    }

    private String formatFullName(UserModel userModel) {
        return String.format("%s %s %s",
                defaultString(userModel.getLastname()),
                defaultString(userModel.getFirstname()),
                defaultString(userModel.getPatronymic())
        ).trim();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String generatePassword() {
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int index = RANDOM.nextInt(PASSWORD_ALPHABET.length());
            password.append(PASSWORD_ALPHABET.charAt(index));
        }
        return password.toString();
    }
}
