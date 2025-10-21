package com.esvar.dekanat.view;

import com.esvar.dekanat.entity.UserEntity;
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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Користувачі")
@PermitAll
public class UsersView extends VerticalLayout {

    private final VerticalLayout addUserLayout = new VerticalLayout();
    private final VerticalLayout allUserLayout = new VerticalLayout();

    private final TextField FirstnameTF = new TextField("Ім'я");
    private final TextField LastnameTF = new TextField("Прізвище");
    private final TextField PatronymicTF = new TextField("По батькові");
    private final TextField EmailTF = new TextField("Email");
    private final ComboBox<String> RoleCB = new ComboBox<>("Роль");
    private final ComboBox<String> RoleTypeCB = new ComboBox<>("Тип ролі");
    private final Button addUserButton = new Button("Додати користувача");
    private final Button saveUserButton = new Button("Зберегти");
    private final Button cancelUserButton = new Button("Відміна");


    public UsersView() {


        createUserForm();
        createGrid();
    }

    public void createUserForm() {
        FirstnameTF.setWidth("200px");
        LastnameTF.setWidth("200px");
        PatronymicTF.setWidth("200px");
        EmailTF.setWidth("250px");
        RoleCB.setWidth("200px");
        RoleTypeCB.setWidth("200px");

        addUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addUserButton.setWidth("180px");
        saveUserButton.setWidth("120px");
        cancelUserButton.setWidth("120px");

        HorizontalLayout firstRow = new HorizontalLayout(FirstnameTF, LastnameTF, PatronymicTF, EmailTF);
        HorizontalLayout secondRow = new HorizontalLayout(RoleCB, RoleTypeCB);
        HorizontalLayout thirdRow = new HorizontalLayout(addUserButton, saveUserButton, cancelUserButton);

        firstRow.setSpacing(true);
        secondRow.setSpacing(true);
        thirdRow.setSpacing(true);

        addUserLayout.add(
                firstRow,
                secondRow,
                thirdRow
        );
        addUserLayout.setPadding(true);
        add(addUserLayout);
    }

    public void createGrid() {
        Grid<UserEntity> grid = new Grid<>(UserEntity.class, false);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        // ======== КОЛОНКИ =========
        Grid.Column<UserEntity> idColumn = grid.addColumn(UserEntity::getId)
                .setHeader("ID")
                .setWidth("120px")
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        Grid.Column<UserEntity> pibColumn = grid.addColumn(UserEntity::getPib)
                .setHeader("Прізвище Ім'я По батькові")
                .setWidth("200px");

        Grid.Column<UserEntity> emailColumn = grid.addColumn(UserEntity::getEmail)
                .setHeader("Пошта")
                .setWidth("180px");

        Grid.Column<UserEntity> activeColumn = grid.addColumn(new ComponentRenderer<>(user -> {
                    Icon icon = user.isActive() ? VaadinIcon.CHECK.create() : VaadinIcon.CLOSE.create();
                    icon.setColor(user.isActive() ? "green" : "red");
                    icon.setSize("18px");
                    return icon;
                }))
                .setHeader("А")
                .setWidth("120px")
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        Grid.Column<UserEntity> roleColumn = grid.addColumn(UserEntity::getRole)
                .setHeader("Роль")
                .setWidth("160px");

        Grid.Column<UserEntity> roleTypeColumn = grid.addColumn(UserEntity::getRoleType)
                .setHeader("Тип ролі")
                .setWidth("160px");

        Grid.Column<UserEntity> actionsColumn = grid.addColumn(new ComponentRenderer<>(user -> {
                    Button edit = new Button(new Icon(VaadinIcon.EDIT));
                    edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                    edit.getElement().getStyle().set("width", "32px").set("height", "32px");
                    edit.addClickListener(e -> editUser(user));

                    Button reset = new Button(new Icon(VaadinIcon.KEY));
                    reset.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                    reset.getElement().getStyle().set("width", "32px").set("height", "32px");
                    reset.addClickListener(e -> resetPassUser(user));

                    Button delete = new Button(new Icon(VaadinIcon.TRASH));
                    delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
                    delete.getElement().getStyle().set("width", "32px").set("height", "32px");
                    delete.addClickListener(e -> deleteUser(user));

                    HorizontalLayout layout = new HorizontalLayout(edit, reset, delete);
                    layout.setSpacing(false);
                    layout.getStyle().set("gap", "6px");
                    layout.setJustifyContentMode(JustifyContentMode.CENTER);
                    return layout;
                }))
                .setHeader("Дії")
                .setWidth("140px")
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        grid.setHeight("500px");

        // ======== ДАНІ =========
        List<UserEntity> allUsers = getDemoUsers();
        ListDataProvider<UserEntity> dataProvider = new ListDataProvider<>(allUsers);
        grid.setDataProvider(dataProvider);

        // ======== ФІЛЬТРИ =========
        HeaderRow filterRow = grid.appendHeaderRow();

        TextField idFilter = createFilterField("ID", dataProvider,
                user -> String.valueOf(user.getId()));
        filterRow.getCell(idColumn).setComponent(idFilter);

        TextField pibFilter = createFilterField("Фільтр за ПІБ", dataProvider,
                UserEntity::getPib);
        filterRow.getCell(pibColumn).setComponent(pibFilter);

        TextField emailFilter = createFilterField("Фільтр за поштою", dataProvider,
                UserEntity::getEmail);
        filterRow.getCell(emailColumn).setComponent(emailFilter);

        Checkbox activeFilter = new Checkbox();
        activeFilter.addValueChangeListener(e -> {
            dataProvider.clearFilters();
            dataProvider.addFilter(user -> {
                if (activeFilter.getValue()) return user.isActive();
                else return true;
            });
        });
        filterRow.getCell(activeColumn).setComponent(activeFilter);

        TextField roleFilter = createFilterField("Фільтр за роллю", dataProvider,
                UserEntity::getRole);
        filterRow.getCell(roleColumn).setComponent(roleFilter);

        TextField roleTypeFilter = createFilterField("Фільтр за типом", dataProvider,
                UserEntity::getRoleType);
        filterRow.getCell(roleTypeColumn).setComponent(roleTypeFilter);

        allUserLayout.setWidthFull();
        allUserLayout.add(grid);
        add(allUserLayout);
    }

    /**
     * Допоміжний метод створення текстового фільтра з live-оновленням.
     */
    private TextField createFilterField(String placeholder,
                                        ListDataProvider<UserEntity> dataProvider,
                                        ValueProvider<UserEntity, String> valueProvider) {
        TextField filter = new TextField();
        filter.setPlaceholder(placeholder);
        filter.setWidthFull();
        filter.setClearButtonVisible(true);
        filter.addValueChangeListener(event ->
                dataProvider.setFilter(user -> {
                    String value = valueProvider.apply(user);
                    if (value == null) return false;
                    return value.toLowerCase().contains(event.getValue().toLowerCase());
                })
        );
        return filter;
    }


    private void editUser(UserEntity user) {
        System.out.println("Редагуємо користувача: " + user.getPib());
    }

    private void deleteUser(UserEntity user) {
        System.out.println("Видаляємо користувача: " + user.getPib());
    }
    private void resetPassUser(UserEntity user) {
        System.out.println("Скидаємо пароль користувача: " + user.getPib());
    }

    private List<UserEntity> getDemoUsers() {
        List<UserEntity> list = new ArrayList<>();
        list.add(new UserEntity(1L, "Іваненко Іван Іванович", "qwerty@gmail.com", true, "Адмін", "Системна"));
        list.add(new UserEntity(2L, "Петренко Петро Петрович", "qwerty@gmail.com", false, "Користувач", "Локальна"));
        list.add(new UserEntity(3L, "Сидоренко Олег Васильович", "qwerty@gmail.com", true ,"Модератор", "Проєктна"));
        return list;
    }
}
