package com.esvar.dekanat.view;

import com.esvar.dekanat.service.DepartmentService;
import com.esvar.dekanat.service.FacultyService;
import com.esvar.dekanat.user.UserModel;
import com.esvar.dekanat.user.UserService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.mail.MailException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Користувачі")
@RolesAllowed("ROLE_ADMIN")
public class UsersView extends VerticalLayout {

    private static final Map<String, String> ROLE_LABELS = Map.of(
            "ROLE_ADMIN", "ADMIN",
            "ROLE_DEKANAT", "DEKANAT",
            "ROLE_DEPARTMENT", "DEPARTMENT"
    );

    private final TextField firstnameField = new TextField("Ім'я");
    private final TextField lastnameField = new TextField("Прізвище");
    private final TextField patronymicField = new TextField("По батькові");
    private final TextField emailField = new TextField("Email");
    private final ComboBox<String> roleField = new ComboBox<>("Роль");
    private final ComboBox<String> roleTypeField = new ComboBox<>("Тип ролі");
    private final Button saveUserButton = new Button("Зберегти");
    private final Button cancelUserButton = new Button("Відміна");

    private final Grid<UserModel> grid = new Grid<>(UserModel.class, false);
    private final Binder<UserModel> binder = new Binder<>(UserModel.class);
    private ListDataProvider<UserModel> dataProvider;

    private final Map<String, String> roleTypeLabels = new HashMap<>();

    private final UserService userService;
    private final FacultyService facultyService;
    private final DepartmentService departmentService;

    public UsersView(UserService userService, FacultyService facultyService, DepartmentService departmentService) {
        this.userService = userService;
        this.facultyService = facultyService;
        this.departmentService = departmentService;

        setSpacing(true);
        setPadding(true);
        setWidthFull();

        add(new H2("Додавання користувачів"), createUserForm());
        add(new H2("Список користувачів"), createGrid());
    }

    private Component createUserForm() {
        firstnameField.setWidth("200px");
        lastnameField.setWidth("200px");
        patronymicField.setWidth("200px");
        emailField.setWidth("250px");
        roleField.setWidth("200px");
        roleTypeField.setWidth("200px");

        firstnameField.setRequiredIndicatorVisible(true);
        lastnameField.setRequiredIndicatorVisible(true);
        patronymicField.setRequiredIndicatorVisible(true);
        emailField.setRequiredIndicatorVisible(true);
        roleField.setRequiredIndicatorVisible(true);
        roleTypeField.setRequiredIndicatorVisible(true);

        emailField.setClearButtonVisible(true);
        firstnameField.setClearButtonVisible(true);
        lastnameField.setClearButtonVisible(true);
        patronymicField.setClearButtonVisible(true);
        roleField.setClearButtonVisible(true);
        roleTypeField.setClearButtonVisible(true);

        roleField.setItems(ROLE_LABELS.keySet());
        roleField.setItemLabelGenerator(ROLE_LABELS::get);
        roleField.addValueChangeListener(event -> updateRoleTypeOptions(event.getValue()));

        roleTypeField.setItemLabelGenerator(value -> roleTypeLabels.getOrDefault(value, value));

        binder.forField(firstnameField)
                .asRequired("Вкажіть ім'я")
                .bind(UserModel::getFirstname, UserModel::setFirstname);
        binder.forField(lastnameField)
                .asRequired("Вкажіть прізвище")
                .bind(UserModel::getLastname, UserModel::setLastname);
        binder.forField(patronymicField)
                .asRequired("Вкажіть по батькові")
                .bind(UserModel::getPatronymic, UserModel::setPatronymic);
        binder.forField(emailField)
                .asRequired("Вкажіть email")
                .withValidator(new EmailValidator("Невірний формат email"))
                .bind(UserModel::getEmail, UserModel::setEmail);
        binder.forField(roleField)
                .asRequired("Оберіть роль")
                .bind(UserModel::getRole, UserModel::setRole);
        binder.forField(roleTypeField)
                .asRequired("Оберіть тип ролі")
                .bind(UserModel::getRoleType, UserModel::setRoleType);

        saveUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveUserButton.addClickListener(e -> saveUser());

        cancelUserButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelUserButton.addClickListener(e -> clearForm());

        HorizontalLayout firstRow = new HorizontalLayout(firstnameField, lastnameField, patronymicField, emailField);
        HorizontalLayout secondRow = new HorizontalLayout(roleField, roleTypeField);
        HorizontalLayout thirdRow = new HorizontalLayout(saveUserButton, cancelUserButton);

        firstRow.setSpacing(true);
        secondRow.setSpacing(true);
        thirdRow.setSpacing(true);

        VerticalLayout wrapper = new VerticalLayout(firstRow, secondRow, thirdRow);
        wrapper.setPadding(true);
        wrapper.setWidthFull();
        return wrapper;
    }

    private Component createGrid() {
        dataProvider = new ListDataProvider<>(userService.findAll());

        grid.setSelectionMode(Grid.SelectionMode.NONE);

        Grid.Column<UserModel> idColumn = grid.addColumn(UserModel::getId)
                .setHeader("ID")
                .setWidth("120px")
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        Grid.Column<UserModel> pibColumn = grid.addColumn(this::buildPib)
                .setHeader("Прізвище Ім'я По батькові")
                .setWidth("220px");

        Grid.Column<UserModel> emailColumn = grid.addColumn(UserModel::getEmail)
                .setHeader("Пошта")
                .setWidth("220px");

        Grid.Column<UserModel> activeColumn = grid.addColumn(new ComponentRenderer<>(user -> {
                    Icon icon = user.isEnabled() ? VaadinIcon.CHECK.create() : VaadinIcon.CLOSE.create();
                    icon.setColor(user.isEnabled() ? "green" : "red");
                    icon.setSize("18px");
                    return icon;
                }))
                .setHeader("А")
                .setWidth("80px")
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        Grid.Column<UserModel> roleColumn = grid.addColumn(user -> ROLE_LABELS.getOrDefault(user.getRole(), ""))
                .setHeader("Роль")
                .setWidth("160px");

        Grid.Column<UserModel> roleTypeColumn = grid.addColumn(this::resolveRoleTypeLabel)
                .setHeader("Тип ролі")
                .setWidth("240px");

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        grid.setHeight("500px");
        grid.setDataProvider(dataProvider);

        HeaderRow filterRow = grid.appendHeaderRow();

        TextField idFilter = createFilterField("ID", dataProvider, user -> String.valueOf(user.getId()));
        filterRow.getCell(idColumn).setComponent(idFilter);

        TextField pibFilter = createFilterField("Фільтр за ПІБ", dataProvider, this::buildPib);
        filterRow.getCell(pibColumn).setComponent(pibFilter);

        TextField emailFilter = createFilterField("Фільтр за поштою", dataProvider, UserModel::getEmail);
        filterRow.getCell(emailColumn).setComponent(emailFilter);

        Checkbox activeFilter = new Checkbox();
        activeFilter.addValueChangeListener(e -> {
            dataProvider.clearFilters();
            dataProvider.addFilter(user -> !activeFilter.getValue() || user.isEnabled());
        });
        filterRow.getCell(activeColumn).setComponent(activeFilter);

        TextField roleFilter = createFilterField("Фільтр за роллю", dataProvider,
                user -> ROLE_LABELS.getOrDefault(user.getRole(), ""));
        filterRow.getCell(roleColumn).setComponent(roleFilter);

        TextField roleTypeFilter = createFilterField("Фільтр за типом", dataProvider, this::resolveRoleTypeLabel);
        filterRow.getCell(roleTypeColumn).setComponent(roleTypeFilter);

        grid.setWidthFull();

        return grid;
    }

    private TextField createFilterField(String placeholder,
                                        ListDataProvider<UserModel> dataProvider,
                                        ValueProvider<UserModel, String> valueProvider) {
        TextField filter = new TextField();
        filter.setPlaceholder(placeholder);
        filter.setWidthFull();
        filter.setClearButtonVisible(true);
        filter.addValueChangeListener(event -> dataProvider.setFilter(user -> {
            String value = valueProvider.apply(user);
            if (value == null) {
                return false;
            }
            return value.toLowerCase().contains(event.getValue().toLowerCase());
        }));
        return filter;
    }

    private void saveUser() {
        UserModel userModel = new UserModel();
        if (!binder.writeBeanIfValid(userModel)) {
            Notification.show("Заповніть обов'язкові поля.", 4000, Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            userService.createUser(userModel);
            Notification.show("Користувача додано, лист відправлено на пошту.", 5000, Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshGrid();
            clearForm();
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage(), 5000, Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (MailException ex) {
            Notification.show("Користувача створено, але не вдалося надіслати лист.", 6000, Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            refreshGrid();
            clearForm();
        }
    }

    private void clearForm() {
        binder.readBean(new UserModel());
        roleTypeLabels.clear();
        roleTypeField.setItems(Collections.emptyList());
        roleTypeField.clear();
        roleField.clear();
    }

    private void refreshGrid() {
        List<UserModel> users = userService.findAll();
        dataProvider.getItems().clear();
        dataProvider.getItems().addAll(users);
        dataProvider.refreshAll();
    }

    private void updateRoleTypeOptions(String role) {
        roleTypeLabels.clear();
        if (role == null) {
            roleTypeField.setItems(Collections.emptyList());
            roleTypeField.clear();
            return;
        }

        if (role.equals("ROLE_ADMIN")) {
            roleTypeLabels.put("0", "admin");
        } else if (role.equals("ROLE_DEKANAT")) {
            facultyService.getAllFaculties().forEach(faculty ->
                    roleTypeLabels.put(String.valueOf(faculty.getId()), faculty.getTitle()));
        } else if (role.equals("ROLE_DEPARTMENT")) {
            departmentService.getAllDepartments().forEach(department ->
                    roleTypeLabels.put(String.valueOf(department.getId()), department.getTitle()));
        }

        roleTypeField.setItems(roleTypeLabels.keySet());
        roleTypeField.setValue(roleTypeLabels.keySet().stream().findFirst().orElse(null));
    }

    private String buildPib(UserModel user) {
        return List.of(user.getLastname(), user.getFirstname(), user.getPatronymic())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" ")).trim();
    }

    private String resolveRoleTypeLabel(UserModel user) {
        if (user.getRole() == null || user.getRoleType() == null) {
            return "";
        }

        String baseLabel = roleTypeLabels.getOrDefault(user.getRoleType(), user.getRoleType());
        try {
            if (user.getRole().startsWith("ROLE_DEKANAT")) {
                String faculty = facultyService.getFacultyTitleById(Long.valueOf(user.getRoleType()));
                return formatTypeLabel(user.getRoleType(), faculty);
            }
            if (user.getRole().startsWith("ROLE_DEPARTMENT")) {
                String department = departmentService.getDepartmentById(Long.valueOf(user.getRoleType()));
                return formatTypeLabel(user.getRoleType(), department);
            }
        } catch (NumberFormatException ignored) {
            return baseLabel;
        }
        return baseLabel;
    }

    private String formatTypeLabel(String code, String title) {
        if (title == null || title.isBlank()) {
            return code;
        }
        return code + " - " + title;
    }
}
