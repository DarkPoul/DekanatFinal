package com.esvar.dekanat.mail;

import com.esvar.dekanat.service.DepartmentService;
import com.esvar.dekanat.service.FacultyService;
import com.esvar.dekanat.user.UserModel;
import com.esvar.dekanat.user.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
public class ChatProfileResolver {

    private final UserRepository userRepository;
    private final FacultyService facultyService;
    private final DepartmentService departmentService;

    public ChatProfileResolver(UserRepository userRepository,
                               FacultyService facultyService,
                               DepartmentService departmentService) {
        this.userRepository = userRepository;
        this.facultyService = facultyService;
        this.departmentService = departmentService;
    }

    public ResolvedProfile resolve(String peerEmail) {
        Optional<UserModel> user = userRepository.findByEmail(peerEmail);
        if (user.isEmpty()) {
            return ResolvedProfile.unknown(peerEmail);
        }
        UserModel userModel = user.get();
        String displayName = composeFullName(userModel);
        String orgUnit = resolveOrgUnit(userModel);
        return new ResolvedProfile(displayName, orgUnit);
    }

    private String composeFullName(UserModel user) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(user.getLastname())) {
            builder.append(user.getLastname());
        }
        if (StringUtils.hasText(user.getFirstname())) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(user.getFirstname());
        }
        if (StringUtils.hasText(user.getPatronymic())) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(user.getPatronymic());
        }
        return builder.length() == 0 ? peerEmailFallback(user) : builder.toString();
    }

    private String peerEmailFallback(UserModel userModel) {
        return StringUtils.hasText(userModel.getEmail()) ? userModel.getEmail() : "Невідомий";
    }

    private String resolveOrgUnit(UserModel userModel) {
        String role = userModel.getRole();
        String roleType = userModel.getRoleType();
        if (!StringUtils.hasText(role) || !StringUtils.hasText(roleType)) {
            return null;
        }
        if (role.startsWith("ROLE_DEKANAT")) {
            return facultyService.getFacultyTitleById(parseLongSafe(roleType));
        }
        if (role.startsWith("ROLE_DEPARTMENT")) {
            return departmentService.getDepartmentById(parseLongSafe(roleType));
        }
        return null;
    }

    private Long parseLongSafe(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public record ResolvedProfile(String displayName, String orgUnit) {
        static ResolvedProfile unknown(String email) {
            return new ResolvedProfile("Невідомий", null);
        }
    }
}
