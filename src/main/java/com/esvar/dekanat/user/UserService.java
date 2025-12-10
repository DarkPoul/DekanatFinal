package com.esvar.dekanat.user;

import com.esvar.dekanat.mailer.EmailService;
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
    private final EmailService emailService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
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
        emailService.sendWelcomeEmail(savedUser, rawPassword);

        return rawPassword;
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
