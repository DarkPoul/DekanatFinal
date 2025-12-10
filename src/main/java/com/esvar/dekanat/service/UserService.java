package com.esvar.dekanat.service;

import com.esvar.dekanat.user.UserModel;
import com.esvar.dekanat.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
public class UserService {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String PASSWORD_CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz0123456789";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public record CreatedUser(UserModel user, String rawPassword) { }

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public CreatedUser createUser(String email,
                                  String firstname,
                                  String lastname,
                                  String patronymic,
                                  String role,
                                  String roleType) {
        validateEmailUniqueness(email);

        String generatedPassword = generateTemporaryPassword();

        UserModel userModel = new UserModel();
        userModel.setEmail(email);
        userModel.setFirstname(firstname);
        userModel.setLastname(lastname);
        userModel.setPatronymic(patronymic);
        userModel.setRole(ROLE_PREFIX + role);
        userModel.setRoleType(roleType);
        userModel.setEnabled(true);
        userModel.setPassword(passwordEncoder.encode(generatedPassword));

        return new CreatedUser(userRepository.save(userModel), generatedPassword);
    }

    private void validateEmailUniqueness(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Користувач з такою поштою вже існує");
        }
    }

    private String generateTemporaryPassword() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            int index = secureRandom.nextInt(PASSWORD_CHARSET.length());
            builder.append(PASSWORD_CHARSET.charAt(index));
        }
        return builder.toString();
    }

    public List<UserModel> findAll() {
        return userRepository.findAll();
    }
}
