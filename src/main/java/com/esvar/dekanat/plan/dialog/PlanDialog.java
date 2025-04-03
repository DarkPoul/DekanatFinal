package com.esvar.dekanat.plan.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class PlanDialog extends Dialog {

    private boolean isUpdateMode = false; // Флаг режиму оновлення

    @Setter
    private SavePlanListener savePlanListener; // Слухач для збереження нового плану
    @Setter
    private UpdatePlanListener updatePlanListener; // Слухач для оновлення існуючого плану
    @Setter
    private RemovePlanListener removePlanListener; // Слухач для видалення плану

    // Компоненти для вибору дисципліни, годин, кафедри тощо
    private final ComboBox<String> discipline = new ComboBox<>("Дисципліни");
    private final TextField hours = new TextField("Години");
    private final Select<String> choiceDiscipline = new Select<>();
    private final Select<String> firstControl = new Select<>();
    private final Select<String> secondControl = new Select<>();
    private final Select<String> parts = new Select<>();
    private final ComboBox<String> department = new ComboBox<>("Кафедра");

    // Кнопки діалогового вікна
    private final Button save = new Button("Зберегти");
    private final Button update = new Button("Оновити");
    private final Button cancel = new Button("Відміна");
    private final Button remove = new Button("Видалити");

    // Компоненти для вибору студентів
    private final Checkbox checkAllStudents = new Checkbox("Обрати всіх");
    private final CheckboxGroup<String> checkboxGroup = new CheckboxGroup<>();
    private final VerticalLayout VLayoutStudent = new VerticalLayout();
    private final Div scrollableDiv = new Div();
    private Long PlanId;

    private List<String> currentStudents; // Поточний список студентів

    public PlanDialog(List<String> disciplines, List<String> departments,
                      List<String> firstControlTypes, List<String> secondControlTypes,
                      List<String> students) {
        this.currentStudents = new ArrayList<>(students); // Зберігаємо поточний список студентів



        // Ініціалізація компонентів
        initializeComponents(disciplines, departments, firstControlTypes, secondControlTypes);

        // Налаштування макету
        setupLayout();

        // Встановлення слухачів для кнопок
        setupButtonListeners();



    }

    private void initializeComponents(List<String> disciplines, List<String> departments,
                                      List<String> firstControlTypes, List<String> secondControlTypes) {
        // Налаштування JComboBox для дисциплін та кафедр
        discipline.setItems(disciplines);
        department.setItems(departments);

        // Налаштування вибору типу дисципліни (вибіркова/обов'язкова)
        choiceDiscipline.setLabel("Вибіркова");
        choiceDiscipline.setItems("Так", "Ні");
        choiceDiscipline.setValue("Ні");

        // Налаштування методів контролю
        firstControl.setLabel("Перший контроль");
        firstControl.setItems(firstControlTypes);
        secondControl.setLabel("Другий контроль");
        secondControl.setItems(secondControlTypes);
        secondControl.setValue("Відсутній"); // За замовчуванням - відсутній

        // Налаштування частин РР/РГР
        parts.setLabel("Частини");
        parts.setItems("1", "2", "4", "6", "8");
        parts.setValue("1");
        parts.setReadOnly(true); // Заблоковано за замовчуванням

        // Налаштування списку студентів
        checkboxGroup.setItems(currentStudents);
        checkboxGroup.setValue(new HashSet<>(currentStudents));
        checkboxGroup.setReadOnly(false); // Спочатку приховано

        // Обробка чекбокса "Обрати всіх"
        checkAllStudents.addValueChangeListener(event -> {
            if (event.getValue()) {
                checkboxGroup.setValue(new HashSet<>(currentStudents));
            } else {
                checkboxGroup.deselectAll();
            }
        });

        checkboxGroup.addValueChangeListener(event -> {
            if (event.getValue().size() == currentStudents.size()) {
                checkAllStudents.setValue(true);
            } else if (event.getValue().isEmpty()) {
                checkAllStudents.setValue(false);
            } else {
                checkAllStudents.setIndeterminate(true);
            }
        });

        // Налаштування кнопок
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        update.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        remove.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        // Логіка для розблокування частин РР/РГР
        secondControl.addValueChangeListener(event -> {
            if ("Розрахункова робота".equals(event.getValue()) || "Розрахунково-графічна робота".equals(event.getValue())) {
                parts.setReadOnly(false);
            } else {
                parts.setReadOnly(true);
                parts.setValue("1");
            }
        });

        // Налаштування скроллбару для списку студентів
        scrollableDiv.setWidth("100%");
        scrollableDiv.setHeight("400px");
        scrollableDiv.getStyle().set("overflow-y", "auto");
        scrollableDiv.add(checkboxGroup);
    }

    private void setupLayout() {
        VerticalLayout VLayoutDisc1 = new VerticalLayout();
        VLayoutDisc1.add(discipline, firstControl, choiceDiscipline);

        VerticalLayout VLayoutDisc2 = new VerticalLayout();
        VLayoutDisc2.add(hours, secondControl, parts);

        VLayoutStudent.add(scrollableDiv, checkAllStudents);
        VLayoutStudent.setWidth("400px");
        VLayoutStudent.getStyle().set("border", "1px solid #e0e0e0");
        VLayoutStudent.getStyle().set("gap", "0px");

        HorizontalLayout HLayoutDisc = new HorizontalLayout();
        HLayoutDisc.add(VLayoutDisc1, VLayoutDisc2);
        HLayoutDisc.setWidth("600px");

        VerticalLayout VLayoutDisc3 = new VerticalLayout();
        department.setLabel("Кафедра");
        department.getStyle().set("padding-left", "14px");
        department.getStyle().set("width", "500px");
        VLayoutDisc3.add(HLayoutDisc, department);

        HorizontalLayout HLayoutAll = new HorizontalLayout();
        HLayoutAll.add(VLayoutDisc3, VLayoutStudent);

        HorizontalLayout HButtonLayout = new HorizontalLayout();
        HButtonLayout.add(save, update, cancel, remove);

        add(HLayoutAll, HButtonLayout);

        update.setVisible(false);
        remove.setVisible(false); // Кнопка "Видалити" прихована за замовчуванням

        // Обробка кнопки "Відміна"
        cancel.addClickListener(event -> close());

        VLayoutStudent.setEnabled(false);

        // Обробка вибору типу дисципліни
        choiceDiscipline.addValueChangeListener(event -> {
            boolean isElective = "Так".equals(event.getValue());
            VLayoutStudent.setEnabled(isElective);
            if (!isElective) {
                checkboxGroup.deselectAll(); // Скидаємо вибір студентів
                checkAllStudents.setValue(false);
            }
        });
    }

    private void setupButtonListeners() {
        save.addClickListener(event -> handleSave());
        update.addClickListener(event -> handleUpdate());
        remove.addClickListener(event -> handleRemove());
    }

    private void handleSave() {
        if (isAllRequiredFieldsFilled()) {
            // Отримуємо дані з форми
            String selectedDiscipline = discipline.getValue();
            int selectedHours = Integer.parseInt(hours.getValue());
            String elective = choiceDiscipline.getValue();
            String firstControlType = firstControl.getValue();
            String secondControlType = secondControl.getValue();
            String selectedDepartment = department.getValue();
            String selectedParts = parts.getValue();

            // Отримуємо список студентів (лише для вибіркових дисциплін)
            List<String> selectedStudents = choiceDiscipline.getValue().equals("Так")
                    ? new ArrayList<>(checkboxGroup.getSelectedItems())
                    : null;

            // Викликаємо слухача збереження
            if (savePlanListener != null) {
                savePlanListener.onSave(selectedDiscipline, selectedHours, elective.equals("Так"),
                        firstControlType, secondControlType, selectedParts, selectedDepartment, selectedStudents);
            }
            close(); // Закриваємо діалог
        } else {
            showValidationError();
        }
    }

    private void handleUpdate() {
        if (isAllRequiredFieldsFilled()) {
            // Отримуємо дані з форми
            String selectedDiscipline = discipline.getValue();
            int selectedHours = Integer.parseInt(hours.getValue());
            boolean isElective = "Так".equals(choiceDiscipline.getValue());
            String firstControlType = firstControl.getValue();
            String secondControlType = secondControl.getValue();
            String selectedDepartment = department.getValue();
            String selectedParts = parts.getValue();
            List<String> selectedStudents = isElective
                    ? new ArrayList<>(checkboxGroup.getSelectedItems())
                    : null;

            // Викликаємо слухача оновлення з передачею planId
            if (updatePlanListener != null) {
                updatePlanListener.onUpdate(PlanId, selectedDiscipline, selectedHours, isElective,
                        firstControlType, secondControlType, selectedParts, selectedDepartment, selectedStudents);
            }
            close();
        } else {
            showValidationError();
        }
    }

    private void handleRemove() {
        if (removePlanListener != null) {
            Long planId = PlanId; // Отримуємо ID вибраного плану
            removePlanListener.onRemove(planId); // Викликаємо слухача видалення
        }
        close(); // Закриваємо діалог
    }



    private boolean isAllRequiredFieldsFilled() {
        return discipline.getValue() != null && !discipline.getValue().isEmpty() &&
                hours.getValue() != null && !hours.getValue().isEmpty() &&
                choiceDiscipline.getValue() != null && !choiceDiscipline.getValue().isEmpty() &&
                firstControl.getValue() != null && !firstControl.getValue().isEmpty() &&
                department.getValue() != null && !department.getValue().isEmpty();
    }

    private void showValidationError() {
        Notification notification = new Notification("Введіть всі обов'язкові поля!", 3000);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.open();
    }

    public void openForCreation() {
        clearFields();
        update.setVisible(false); // Приховуємо кнопку "Оновити"
        save.setVisible(true); // Показуємо кнопку "Зберегти"
        remove.setVisible(false); // Приховуємо кнопку "Видалити"
        PlanId = null; // Скидаємо ID плану
        super.open();
    }

    public void openForUpdate(String disciplineName, int hoursValue, boolean isElective,
                              String firstControlType, String secondControlType, String partsValue,
                              String departmentName, List<String> selectedStudents, Long planId) {
        this.isUpdateMode = true;
        discipline.setValue(disciplineName);
        hours.setValue(String.valueOf(hoursValue));
        choiceDiscipline.setValue(isElective ? "Так" : "Ні");
        firstControl.setValue(firstControlType);
        secondControl.setValue(secondControlType);
        parts.setValue(partsValue);
        department.setValue(departmentName);

        if (isElective) {
            checkboxGroup.setValue(new HashSet<>(selectedStudents));
            checkAllStudents.setValue(!selectedStudents.isEmpty());
        }

        update.setVisible(true);
        save.setVisible(false);
        remove.setVisible(true); // Показуємо кнопку "Видалити" у режимі оновлення
        if (planId != null){
            PlanId = planId; // Зберігаємо ID плану
        }
        super.open();
    }

    private void clearFields() {
        discipline.clear();
        hours.clear();
        choiceDiscipline.setValue("Ні");
        firstControl.clear();
        secondControl.setValue("Відсутній");
        parts.setValue("1");
        department.clear();
        checkboxGroup.deselectAll();
        checkAllStudents.setValue(false);
    }

    public void updateStudentsList(List<String> students) {
        this.currentStudents = new ArrayList<>(students); // Оновлюємо внутрішній список студентів
        checkboxGroup.setItems(currentStudents); // Встановлюємо нові елементи для CheckboxGroup
        checkboxGroup.setValue(new HashSet<>(currentStudents)); // Вибираємо всіх студентів за замовчуванням
        checkAllStudents.setValue(!students.isEmpty()); // Якщо є студенти, встановлюємо чекбокс "Обрати всіх" у true
    }

    public interface SavePlanListener {
        void onSave(String discipline, int hours, boolean isElective, String firstControl,
                    String secondControl, String parts, String department, List<String> students);
    }

    public interface UpdatePlanListener {
        void onUpdate(Long planId, String discipline, int hours, boolean isElective,
                      String firstControl, String secondControl, String parts,
                      String department, List<String> students);
    }

    public interface RemovePlanListener {
        void onRemove(Long planId); // Метод для видалення плану
    }
}