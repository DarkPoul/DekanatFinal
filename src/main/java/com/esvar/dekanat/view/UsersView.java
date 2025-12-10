package com.esvar.dekanat.view;

import com.esvar.dekanat.service.DepartmentService;
import com.esvar.dekanat.service.FacultyService;
import com.esvar.dekanat.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Користувачі")
@RolesAllowed("ROLE_ADMIN")
public class UsersView extends VerticalLayout {

    private final UserService userService;
    private final FacultyService facultyService;
    private final DepartmentService departmentService;

    private final TextField firstnameField = new TextField("Ім'я");
    private final TextField lastnameField = new TextField("Прізвище");
    private final TextField patronymicField = new TextField("По батькові");
    private final EmailField emailField = new EmailField("Email");
    private final ComboBox<String> roleField = new ComboBox<>("Роль");
    private final ComboBox<RoleTypeOption> roleTypeField = new ComboBox<>("Тип ролі");
    private final Button saveUserButton = new Button("Створити користувача");
    private final Button cancelUserButton = new Button("Очистити");

    private final Binder<UserFormData> binder = new Binder<>(UserFormData.class);
    private List<RoleTypeOption> availableRoleTypeOptions = new ArrayList<>();

    public UsersView(UserService userService, FacultyService facultyService, DepartmentService departmentService) {
        this.userService = userService;
        this.facultyService = facultyService;
        this.departmentService = departmentService;

        configureForm();
        configureBinder();
    }

    private void configureForm() {
        setSpacing(true);
        setPadding(true);

        firstnameField.setRequiredIndicatorVisible(true);
        lastnameField.setRequiredIndicatorVisible(true);
        patronymicField.setRequiredIndicatorVisible(true);
        emailField.setRequiredIndicatorVisible(true);
        roleField.setRequiredIndicatorVisible(true);
        roleTypeField.setRequiredIndicatorVisible(true);

        roleField.setItems("ADMIN", "DEKANAT", "DEPARTMENT");
        roleField.addValueChangeListener(event -> updateRoleTypeOptions(event.getValue()));

        roleTypeField.setItemLabelGenerator(RoleTypeOption::label);
        roleTypeField.setAllowCustomValue(false);
        roleTypeField.setEnabled(false);

        firstnameField.setWidth("250px");
        lastnameField.setWidth("250px");
        patronymicField.setWidth("250px");
        emailField.setWidth("300px");
        roleField.setWidth("240px");
        roleTypeField.setWidth("240px");

        saveUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveUserButton.addClickListener(event -> saveUser());
        cancelUserButton.addClickListener(event -> resetForm());

        H2 title = new H2("Додати нового користувача (доступно лише адміністраторам)");

        HorizontalLayout firstRow = new HorizontalLayout(firstnameField, lastnameField, patronymicField, emailField);
        HorizontalLayout secondRow = new HorizontalLayout(roleField, roleTypeField);
        HorizontalLayout actions = new HorizontalLayout(saveUserButton, cancelUserButton);

        add(title, firstRow, secondRow, actions);
    }

    private void configureBinder() {
        binder.forField(emailField)
                .asRequired("Email обов'язковий")
                .withValidator(new EmailValidator("Некоректний формат email"))
                .bind(UserFormData::getEmail, UserFormData::setEmail);

        binder.forField(firstnameField)
                .asRequired("Ім'я обов'язкове")
                .bind(UserFormData::getFirstname, UserFormData::setFirstname);

        binder.forField(lastnameField)
                .asRequired("Прізвище обов'язкове")
                .bind(UserFormData::getLastname, UserFormData::setLastname);

        binder.forField(patronymicField)
                .asRequired("По батькові обов'язкове")
                .bind(UserFormData::getPatronymic, UserFormData::setPatronymic);

        binder.forField(roleField)
                .asRequired("Роль обов'язкова")
                .bind(UserFormData::getRole, UserFormData::setRole);

        binder.forField(roleTypeField)
                .asRequired("Тип ролі обов'язковий")
                .withConverter(RoleTypeOption::id, this::findRoleTypeOptionById, "Оберіть тип ролі")
                .bind(UserFormData::getRoleTypeId, UserFormData::setRoleTypeId);

        binder.setBean(new UserFormData());
    }

    private void updateRoleTypeOptions(String role) {
        roleTypeField.clear();
        availableRoleTypeOptions = new ArrayList<>();

        if (role == null) {
            roleTypeField.setItems(Collections.emptyList());
            roleTypeField.setEnabled(false);
            return;
        }

        switch (role) {
            case "DEKANAT" -> availableRoleTypeOptions = facultyService.getAllFaculties().stream()
                    .map(faculty -> new RoleTypeOption(String.valueOf(faculty.getId()),
                            faculty.getId() + ": " + faculty.getTitle()))
                    .toList();
            case "DEPARTMENT" -> availableRoleTypeOptions = departmentService.getAllDepartments().stream()
                    .map(department -> new RoleTypeOption(String.valueOf(department.getId()),
                            department.getId() + ": " + department.getTitle()))
                    .toList();
            default -> availableRoleTypeOptions = List.of(new RoleTypeOption("0", "0: Адміністратор"));
        }

        roleTypeField.setItems(availableRoleTypeOptions);
        roleTypeField.setEnabled(true);
    }

    private RoleTypeOption findRoleTypeOptionById(String id) {
        if (id == null) {
            return null;
        }
        return availableRoleTypeOptions.stream()
                .filter(option -> option.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    private void saveUser() {
        UserFormData formData = binder.getBean();
        if (formData == null) {
            formData = new UserFormData();
            binder.setBean(formData);
        }

        try {
            binder.writeBean(formData);
        } catch (ValidationException e) {
            Notification.show("Перевірте правильність заповнення форми", 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            UserService.CreatedUser createdUser = userService.createUser(
                    formData.getEmail(),
                    formData.getFirstname(),
                    formData.getLastname(),
                    formData.getPatronymic(),
                    formData.getRole(),
                    formData.getRoleTypeId()
            );

            Notification notification = Notification.show(
                    "Користувача створено. Тимчасовий пароль: " + createdUser.rawPassword(),
                    7000,
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            resetForm();
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void resetForm() {
        binder.setBean(new UserFormData());
        roleTypeField.clear();
        roleTypeField.setItems(Collections.emptyList());
        roleTypeField.setEnabled(false);
    }

    private static class UserFormData {
        private String firstname;
        private String lastname;
        private String patronymic;
        private String email;
        private String role;
        private String roleTypeId;

        public String getFirstname() {
            return firstname;
        }

        public void setFirstname(String firstname) {
            this.firstname = firstname;
        }

        public String getLastname() {
            return lastname;
        }

        public void setLastname(String lastname) {
            this.lastname = lastname;
        }

        public String getPatronymic() {
            return patronymic;
        }

        public void setPatronymic(String patronymic) {
            this.patronymic = patronymic;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getRoleTypeId() {
            return roleTypeId;
        }

        public void setRoleTypeId(String roleTypeId) {
            this.roleTypeId = roleTypeId;
        }
    }

    private record RoleTypeOption(String id, String label) { }
}
