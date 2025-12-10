package com.esvar.dekanat.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Login")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();


    public LoginView() {

        login.setForgotPasswordButtonVisible(false);
        addClassName("login-view");
        setSizeFull();

        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);

        login.setAction("login");

        LoginI18n i18n = LoginI18n.createDefault();

        LoginI18n.Form i18nForm = i18n.getForm();
        i18nForm.setTitle("Вхід");
        i18nForm.setUsername("Ім'я користувача");
        i18nForm.setPassword("Пароль");
        i18nForm.setSubmit("Увійти");
        i18nForm.setForgotPassword("Забули пароль?");
        i18n.setForm(i18nForm);


        LoginI18n.ErrorMessage i18nErrorMessage = i18n.getErrorMessage();
        i18nErrorMessage.setTitle("Неправильне ім'я користувача або пароль");
        i18nErrorMessage.setMessage(
                "Перевірте, чи правильно введені ім'я користувача та пароль, і спробуйте ще раз.");
        i18n.setErrorMessage(i18nErrorMessage);

        login.setI18n(i18n);

//        UI.getCurrent().access(() -> UI.getCurrent().navigate(""));

        Paragraph infoParagraph = new Paragraph(
                "У зв’язку з проведенням технічних робіт можливі тимчасові перебої в роботі системи.\n" +
                        "Рекомендуємо частіше зберігати внесену інформацію."
        );

        infoParagraph.getStyle()
                .set("white-space", "pre-line")
                // фон та кольори в стилі Lumo
                .set("background-color", "var(--lumo-primary-color-10pct)")
                .set("color", "var(--lumo-body-text-color)")
                // відступи та радіус — як у стандартних компонентів
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                // легка рамка + акцентована ліва смуга
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-left", "4px solid var(--lumo-primary-color)")
                // типографіка під Lumo
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "500")
                .set("line-height", "1.4")
                // щоб відокремити від решти контенту
                .set("margin", "var(--lumo-space-m) 0")
                .set("display", "block");

        add(new H1("Dekanat CRM"), infoParagraph, login);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(beforeEnterEvent.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            login.setError(true);
        }
    }
}
