package com.esvar.dekanat.view;

import com.esvar.dekanat.entity.UserEntity;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
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
    private final Button addUserButton = new Button("Додати користувача");


    public UsersView() {
        Grid<UserEntity> grid = new Grid<>(UserEntity.class, false);

        // Увімкнення вибору записів (галочки)
        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        // Колонки
        grid.addColumn(UserEntity::getId)
                .setHeader("ID")
                .setAutoWidth(true);

        grid.addColumn(UserEntity::getPib)
                .setHeader("ПІБ")
                .setFlexGrow(1);

        grid.addColumn(new ComponentRenderer<>(user -> {
            Icon icon = user.isActive() ? VaadinIcon.CHECK.create() : VaadinIcon.CLOSE.create();
            icon.setColor(user.isActive() ? "green" : "red");
            return icon;
        })).setHeader("А").setAutoWidth(true);

        grid.addColumn(UserEntity::getRole)
                .setHeader("Роль")
                .setAutoWidth(true);

        grid.addColumn(UserEntity::getRoleType)
                .setHeader("Тип ролі")
                .setAutoWidth(true);

        // Колонка дій (іконки або кнопки)
        grid.addColumn(new ComponentRenderer<>(user -> {
            Button edit = new Button(new Icon(VaadinIcon.EDIT));
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            edit.addClickListener(e -> editUser(user));

            Button delete = new Button(new Icon(VaadinIcon.TRASH));
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
            delete.addClickListener(e -> deleteUser(user));

            return new Span(edit, delete);
        })).setHeader("Дії").setAutoWidth(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        // Демонстраційні дані
        grid.setItems(getDemoUsers());

        add(grid);
        setSizeFull();
    }

    private void editUser(UserEntity user) {
        System.out.println("Редагуємо користувача: " + user.getPib());
    }

    private void deleteUser(UserEntity user) {
        System.out.println("Видаляємо користувача: " + user.getPib());
    }

    private List<UserEntity> getDemoUsers() {
        List<UserEntity> list = new ArrayList<>();
        list.add(new UserEntity(1L, "Іваненко Іван Іванович", true, "Адмін", "Системна"));
        list.add(new UserEntity(2L, "Петренко Петро Петрович", false, "Користувач", "Локальна"));
        list.add(new UserEntity(3L, "Сидоренко Олег Васильович", true, "Модератор", "Проєктна"));
        return list;
    }
}
