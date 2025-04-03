package com.esvar.dekanat.view;

import com.esvar.dekanat.card.CardView;
import com.esvar.dekanat.mark.EnterMarksView;
import com.esvar.dekanat.plan.PlanView;
import com.esvar.dekanat.security.SecurityService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;


public class MainLayout extends AppLayout {

    private final Tabs tabs;
    private boolean isDrawerLocked = false; // Статус блокування

    public MainLayout(SecurityService securityService) {
        H1 logo = new H1("Dekanat CRM");
        logo.addClassNames("text-l", "m-m");
        Button logout = new Button("Вихід", e -> securityService.logout());

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, logout);



        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);


        header.setWidth("100%");
        header.addClassNames("py-0", "px-m");

        addToNavbar(header);

        tabs = new Tabs();
        tabs.add(
                createTab(VaadinIcon.CLIPBOARD_CHECK, "Навчальні плани", PlanView.class),
//                createTab(VaadinIcon.LINE_BAR_CHART, "Успішність", SuccessView.class),
//                createTab(VaadinIcon.ABACUS, "Боржники", DebtorView.class),
//                createTab(VaadinIcon.LIST_SELECT, "Друк інформації", StudentCardView.class),
                createTab(VaadinIcon.USER_CARD, "Перегляд карток", CardView.class),
                createTab(VaadinIcon.PENCIL, "Введення оцінок", EnterMarksView.class)
//                createTab(VaadinIcon.ARCHIVE, "Архів", ArchiveView.class),
//                createTab(VaadinIcon.BOOK, "Довідники", HandbookView.class)
//                ,
//                createTab(VaadinIcon.USER, "Адмін", AdminView.class)
        );
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        addToDrawer(tabs);

        UI.getCurrent().access(() -> UI.getCurrent().navigate(PlanView.class));

    }

    private Tab createTab(VaadinIcon viewIcon, String viewName, Class<? extends Component> navigationTarget) {
        Icon icon = viewIcon.create();
        icon.getStyle()
                .set("box-sizing", "border-box")
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("margin-inline-start", "var(--lumo-space-xs)")
                .set("padding", "var(--lumo-space-xs)");

        RouterLink link = new RouterLink();
        link.add(icon, new Span(viewName));
        link.setRoute(navigationTarget);
        link.setTabIndex(-1);

        return new Tab(link);
    }

    // Метод для блокування/розблокування меню
    public void setDrawerEnabled(boolean enabled) {
        tabs.setEnabled(enabled);
        isDrawerLocked = !enabled;
    }

    // Метод для перевірки стану меню
    public boolean isDrawerLocked() {
        return isDrawerLocked;
    }

}
