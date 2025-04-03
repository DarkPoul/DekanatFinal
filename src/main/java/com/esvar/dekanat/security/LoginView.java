package com.esvar.dekanat.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
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

        add(new H1("Dekanat CRM"), login);
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
