package com.esvar.dekanat.service;

import com.esvar.dekanat.user.UserModel;
import com.esvar.dekanat.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "DEKANAT", "DEPARTMENT");
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
        String normalizedEmail = normalizeEmail(email);

        validateRequiredFields(normalizedEmail, firstname, lastname, patronymic, role, roleType);
        validateRole(role);
        validateRoleType(role, roleType);
        validateEmailUniqueness(normalizedEmail);

        String generatedPassword = generateTemporaryPassword();
        String normalizedRoleType = normalizeRoleType(role, roleType);

        UserModel userModel = new UserModel();
        userModel.setEmail(normalizedEmail);
        userModel.setFirstname(firstname);
        userModel.setLastname(lastname);
        userModel.setPatronymic(patronymic);
        userModel.setRole(ROLE_PREFIX + role);
        userModel.setRoleType(normalizedRoleType);
        userModel.setEnabled(true);
        userModel.setPassword(passwordEncoder.encode(generatedPassword));

        return new CreatedUser(userRepository.save(userModel), generatedPassword);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private void validateRequiredFields(String email,
                                        String firstname,
                                        String lastname,
                                        String patronymic,
                                        String role,
                                        String roleType) {
        Set<String> missingFields = new HashSet<>();
        if (isBlank(email)) missingFields.add("email");
        if (isBlank(firstname)) missingFields.add("ім'я");
        if (isBlank(lastname)) missingFields.add("прізвище");
        if (isBlank(patronymic)) missingFields.add("по батькові");
        if (isBlank(role)) missingFields.add("роль");
        if (isBlank(roleType)) missingFields.add("тип ролі");

        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Заповніть обов'язкові поля: " + String.join(", ", missingFields));
        }
    }

    private void validateRole(String role) {
        if (!ALLOWED_ROLES.contains(role)) {
            throw new IllegalArgumentException("Невідома роль: " + role);
        }
    }

    private void validateRoleType(String role, String roleType) {
        if ("ADMIN".equals(role)) {
            return;
        }
        if (!roleType.matches("\\d+")) {
            throw new IllegalArgumentException("Тип ролі має містити лише номер факультету/кафедри");
        }
    }

    private String normalizeRoleType(String role, String roleType) {
        if ("ADMIN".equals(role)) {
            return "0";
        }
        return roleType.trim();
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
