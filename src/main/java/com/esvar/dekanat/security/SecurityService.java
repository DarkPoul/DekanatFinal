package com.esvar.dekanat.security;

import com.esvar.dekanat.user.UserModel;
import com.esvar.dekanat.user.UserRepository;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityService {

    private static final String LOGOUT_SUCCESS_URL = "/";
    private final UserRepository userRepository;

    public SecurityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDetails getAuthenticatedUser() {
        Object principal = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return (principal instanceof UserDetails)
                ? (UserDetails) principal
                : null;
    }

    public Optional<UserModel> getCurrentUserModel() {
        UserDetails ud = getAuthenticatedUser();
        if (ud == null) return Optional.empty();
        return userRepository.findByEmail(ud.getUsername());
    }

    public void logout() {
        UI.getCurrent().getPage().setLocation(LOGOUT_SUCCESS_URL);
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(
                VaadinServletRequest.getCurrent().getHttpServletRequest(), null,
                null);
    }
    public String getCurrentRoleType() {
        return getCurrentUserModel()
                .map(UserModel::getRoleType)
                .orElse("");
    }
}
