package com.esvar.dekanat.view;

import com.esvar.dekanat.DekanatApplication;
import com.esvar.dekanat.mark.EnterMarksView;
import com.esvar.dekanat.plan.PlanView;
import com.esvar.dekanat.card.CardView;
import com.esvar.dekanat.rating.RatingView;
import com.esvar.dekanat.security.SecurityService;
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
    private final Tabs tabs = new Tabs();
    private final Map<Class<? extends Component>, Tab> navigationTargetToTab = new HashMap<>();
    private boolean isDrawerLocked = false;

    private static final Logger log = LoggerFactory.getLogger(MainLayout.class);

    public MainLayout(SecurityService securityService) {
        this.securityService = securityService;
        // Заголовок
        H1 logo = new H1("Dekanat CRM");
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

        System.out.println("isDepartment: " + isDepartment);
        System.out.println("isDekanatGroup: " + isDekanatGroup);
        System.out.println("isAdmin: " + isAdmin);

        if (isDepartment) {
            UI.getCurrent().navigate(EnterMarksView.class);
            event.rerouteTo(EnterMarksView.class);
            return;
        }
        if (isAdmin || isDekanatGroup) {
            tabs.removeAll();
            navigationTargetToTab.clear();
            tabs.add(
                    createTab(VaadinIcon.CLIPBOARD_CHECK, "Навчальні плани", PlanView.class),
                    createTab(VaadinIcon.USER_CARD, "Перегляд карток", CardView.class),
                    createTab(VaadinIcon.PENCIL, "Введення оцінок", EnterMarksView.class),
                    createTab(VaadinIcon.BAR_CHART, "Рейтинг", RatingView.class)
            );
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
        Icon icon = iconType.create();
        icon.getStyle()
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("padding", "var(--lumo-space-xs)");
        RouterLink link = new RouterLink("", target);
        link.add(icon, new Span(title));
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
