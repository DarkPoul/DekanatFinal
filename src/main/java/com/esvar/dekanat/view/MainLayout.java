package com.esvar.dekanat.view;

import com.esvar.dekanat.entity.SessionEntity;
import com.esvar.dekanat.mark.EnterMarksView;
import com.esvar.dekanat.mail.v2.view.MailInboxView;
import com.esvar.dekanat.plan.PlanView;
import com.esvar.dekanat.card.CardView;
import com.esvar.dekanat.progress.SuccessView;
import com.esvar.dekanat.rating.RatingView;
import com.esvar.dekanat.repository.SessionRepository;
import com.esvar.dekanat.security.SecurityService;
import com.esvar.dekanat.service.DepartmentService;
import com.esvar.dekanat.service.FacultyService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.*;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.tabs.*;
import com.vaadin.flow.router.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MainLayout extends AppLayout implements BeforeEnterObserver {

    private final SecurityService securityService;
    private final SessionRepository sessionRepository;
    private final FacultyService facultyService;
    private final DepartmentService departmentService;
    private final Tabs tabs = new Tabs();
    private final Map<Class<? extends Component>, Tab> navigationTargetToTab = new HashMap<>();
    private boolean isDrawerLocked = false;

    private static final Logger log = LoggerFactory.getLogger(MainLayout.class);

    public MainLayout(SecurityService securityService, SessionRepository sessionRepository, FacultyService facultyService, DepartmentService departmentService) {
        this.securityService = securityService;
        this.sessionRepository = sessionRepository;
        this.facultyService = facultyService;
        this.departmentService = departmentService;
        // Заголовок
        String headerText = securityService.getCurrentUserModel()
                .map(u -> {
                    String base = u.getFirstname() + " " + u.getLastname().toUpperCase();
                    String role = u.getRole();
                    String roleType = u.getRoleType();
                    if (role != null && roleType != null) {
                        if (role.startsWith("ROLE_DEKANAT")) {
                            String faculty = facultyService.getFacultyTitleById(Long.valueOf(roleType));
                            if (faculty != null) {
                                base += " (" + faculty + ")";
                            }
                        } else if (role.startsWith("ROLE_DEPARTMENT")) {
                            String dept = departmentService.getDepartmentById(Long.valueOf(roleType));
                            if (dept != null) {
                                base += " (" + dept + ")";
                            }
                        }
                    }
                    return base;
                })
                .orElse("Dekanat CRM");
        H1 logo = new H1(headerText);
//        logo.getStyle().set("font-weight", "normal");
        logo.getStyle().set("font-size", "var(--lumo-font-size-l)");

        Button logout = new Button("Вихід", e -> securityService.logout());
        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, logout);
        header.expand(logo);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        addToNavbar(header);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        UserDetails u = securityService.getAuthenticatedUser();
        if (u == null) return;  // не залогінені → VaadinWebSecurity на /login

        Set<String> roles = u.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        log.info("User roles: " + roles);

        boolean isDepartment   = roles.stream().anyMatch(r -> r.startsWith("ROLE_DEPARTMENT"));
        boolean isDekanatGroup = roles.contains("ROLE_DEKANAT");
        boolean isAdmin        = roles.contains("ROLE_ADMIN");

        if (isDepartment) {
            UI.getCurrent().navigate(EnterMarksView.class);
            event.rerouteTo(EnterMarksView.class);
            return;
        }
        if (isAdmin || isDekanatGroup) {
            boolean isWinter = sessionRepository.findById(1L)
                    .map(SessionEntity::isWinter)
                    .orElse(false);

            // Використовуємо наявні іконки: Asterisk як сніжинку, Sun_O як сонце
            Icon seasonIcon = (isWinter ? VaadinIcon.ASTERISK.create() : VaadinIcon.SUN_O.create());
            seasonIcon.getStyle()
                    .set("margin-inline-start", "var(--lumo-space-m)")
                    .set("padding", "var(--lumo-space-xs)")
                    .set("color", isWinter ? "blue" : "orange");  // синя сніжинка, жовте сонце

            tabs.removeAll();
            navigationTargetToTab.clear();

            tabs.add(
                    createTab(VaadinIcon.CLIPBOARD_CHECK, "Навчальні плани", PlanView.class),
                    createTab(VaadinIcon.USER_CARD, "Перегляд карток", CardView.class),
                    createTab(VaadinIcon.PENCIL, "Введення оцінок", EnterMarksView.class, seasonIcon),
                    createTab(VaadinIcon.BAR_CHART, "Рейтинг", RatingView.class),
                    createTab(VaadinIcon.BOOK, "Успішність", SuccessView.class)
            );

            if (isAdmin) {
                tabs.add(createTab(VaadinIcon.ENVELOPE, "Пошта", MailInboxView.class));
            }

            if (isAdmin) {
                tabs.add(createTab(VaadinIcon.USERS, "Користувачі", UsersView.class));
            }
            tabs.setOrientation(Tabs.Orientation.VERTICAL);
            addToDrawer(tabs);

            Tab selected = navigationTargetToTab.get(event.getNavigationTarget());
            if (selected != null) {
                tabs.setSelectedTab(selected);
            }

            if (event.getLocation().getPath().isEmpty()) {
                UI.getCurrent().navigate(PlanView.class);
            }
            return;
        }
        event.forwardTo("login");
    }

    private Tab createTab(VaadinIcon iconType, String title, Class<? extends Component> target) {
        return createTab(iconType, title, target, (Icon) null);
    }

    private Tab createTab(VaadinIcon iconType, String title, Class<? extends Component> target, Icon trailingIcon) {
        Icon icon = iconType.create();
        icon.getStyle()
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("padding", "var(--lumo-space-xs)");
        RouterLink link = new RouterLink("", target);
        Span text = new Span(title);
        link.add(icon, text);
        if (trailingIcon != null) {
            link.add(trailingIcon); // вже з кольором
        }
        link.setTabIndex(-1);
        Tab tab = new Tab(link);
        navigationTargetToTab.put(target, tab);
        return tab;
    }


    public void setDrawerEnabled(boolean enabled) {
        tabs.setEnabled(enabled);
        isDrawerLocked = !enabled;
    }
}
