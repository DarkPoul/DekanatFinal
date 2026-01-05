package com.esvar.dekanat.card;

import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.service.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import java.sql.Date;
import java.text.Collator;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AddStudentDialog extends Dialog {

    private final GroupService groupService;
    private final StudentRegistrationService registrationService;

    private final Tabs tabs = new Tabs();
    private final Tab tab1 = new Tab("Персональні дані");
    private final Tab tab2 = new Tab("Паспортні дані");
    private final Tab tab3 = new Tab("Контакти");
    private final Tab tab4 = new Tab("Освіта");

    private final Collator ukrainianCollator = Collator.getInstance(new Locale("uk", "UA"));

    private final VerticalLayout page1 = new VerticalLayout();
    private final VerticalLayout page2 = new VerticalLayout();
    private final VerticalLayout page3 = new VerticalLayout();
    private final VerticalLayout page4 = new VerticalLayout();

    private int index = 0;

    // step 1 fields
    private final TextField lastName = new TextField("Прізвище");
    private final TextField firstName = new TextField("Ім'я");
    private final TextField middleName = new TextField("По батькові");
    private final TextField lastNameEng = new TextField("Прізвище (англ)");
    private final TextField firstNameEng = new TextField("Ім'я (англ)");
    private final Select<String> groupSelect = new Select<>();
    private final TextField recordBook = new TextField("Номер заліковки");
    private final ComboBox<String> reportType = new ComboBox<>();


    // step 2 fields
    private final TextField passportSeries = new TextField("Серія паспорта");
    private final TextField passportNumber = new TextField("Номер паспорта");
    private final DatePicker issueDate = new DatePicker("Дата видачі");
    private final DatePicker expireDate = new DatePicker("Дійсний до");
    private final TextField nationality = new TextField("Національність");
    private final Select<Gender> gender = new Select<>();

    // step 3 fields
    private final TextField phone = new TextField("Телефон");
    private final TextField email = new TextField("Email");
    private final TextField address = new TextField("Адреса");

    // step 4 fields
    private final TextField docSeries = new TextField("Серія документу");
    private final TextField docNumber = new TextField("Номер документу");

    private final Button back = new Button("Назад");
    private final Button next = new Button("Далі");
    private final Button save = new Button("Зберегти");
    private final Button cancel = new Button("Скасувати");
    private final Map<TextField, Pattern> validationPatterns = new LinkedHashMap<>();

    public AddStudentDialog(GroupService groupService,
                            StudentRegistrationService registrationService) {
        this.groupService = groupService;
        this.registrationService = registrationService;

        configureTabs();
        configurePages();
        configureNavigation();
        updateView();
    }

    private void registerPatternValidation(TextField field, String allowedCharPattern, String valueRegex, String errorMessage) {
        field.setAllowedCharPattern(allowedCharPattern);
        field.setErrorMessage(errorMessage);
        if (valueRegex != null) {
            validationPatterns.put(field, Pattern.compile(valueRegex));
            field.setPattern(valueRegex);
        }
    }

    private void configureTabs() {
        tabs.add(tab1, tab2, tab3, tab4);
        tabs.setSelectedIndex(0);
        // allow user to switch pages by clicking on the tabs
        tabs.addSelectedChangeListener(event -> {
            int newIndex = tabs.getSelectedIndex();
            if (newIndex != index) {
                index = newIndex;
                updateView();
            }
        });
        add(tabs);
    }

    private void configurePages() {
        groupSelect.setLabel("Група");
        groupSelect.setItems(groupService.getGroupsDTO().stream()
                .map(GroupDTO::toString)
                .sorted(ukrainianCollator)
                .collect(Collectors.toList()));

        lastName.setRequiredIndicatorVisible(true);
        firstName.setRequiredIndicatorVisible(true);
        groupSelect.setRequiredIndicatorVisible(true);

        reportType.setLabel("Тип відомості");
        reportType.setItems(
                "Зарахований",
                "Відрахований",
                "Академвідпустка",
                "Поновлений",
                "Переведений на наступний курс",
                "Такий що закінчив навчання"
        );
        reportType.setClearButtonVisible(true);

        registerPatternValidation(recordBook, "[0-9]", "\\d+", "Введіть тільки цифри");

        passportSeries.setRequiredIndicatorVisible(true);
        registerPatternValidation(passportSeries, "[\\p{L}0-9]", "[\\p{L}0-9]+", "Введіть серію паспорта");

        passportNumber.setRequiredIndicatorVisible(true);
        registerPatternValidation(passportNumber, "[0-9]", "\\d+", "Введіть номер паспорта");

        issueDate.setRequiredIndicatorVisible(true);
        expireDate.setRequiredIndicatorVisible(true);
        nationality.setRequiredIndicatorVisible(true);

        FormLayout personalForm = new FormLayout();
        personalForm.add(lastName, firstName, middleName,
                lastNameEng, firstNameEng,
                groupSelect, recordBook, reportType);
        personalForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        page1.add(personalForm);

        gender.setLabel("Стать");
        gender.setItems(Gender.values());
        gender.setRequiredIndicatorVisible(true);
        phone.setRequiredIndicatorVisible(true);
        registerPatternValidation(phone, "[0-9]", "\\d+", "Введіть тільки цифри");
        email.setRequiredIndicatorVisible(true);
        registerPatternValidation(email, "[^\\s]", "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", "Некоректний email");
        address.setRequiredIndicatorVisible(true);
        page2.add(passportSeries, passportNumber, issueDate, expireDate, nationality, gender);

        page3.add(phone, email, address);
        registerPatternValidation(docSeries, "[\\p{L}0-9]", "[\\p{L}0-9]+", "Введіть серію документу");
        registerPatternValidation(docNumber, "[0-9]", "\\d+", "Введіть номер документу");
        page4.add(docSeries, docNumber);

        add(page1, page2, page3, page4);
        HorizontalLayout actions = new HorizontalLayout(back, next, save, cancel);
        add(actions);
    }

    private void configureNavigation() {
        back.addClickListener(e -> {
            if (index > 0) {
                index--;
                updateView();
            }
        });
        next.addClickListener(e -> {
            if (index < 3) {
                index++;
                updateView();
            }
        });
        save.addClickListener(e -> saveStudent());
        cancel.addClickListener(e ->
                showCancelConfirmation());
    }

    private void updateView() {
        page1.setVisible(index == 0);
        page2.setVisible(index == 1);
        page3.setVisible(index == 2);
        page4.setVisible(index == 3);
        back.setEnabled(index > 0);
        next.setVisible(index < 3);
        save.setVisible(index == 3);
        tabs.setSelectedIndex(index);
    }

    private void saveStudent() {
        if (!validateForm()) {
            return;
        }
        StudentGroupEntity group = groupService.getGroupByTitle(groupSelect.getValue());
        if (group == null) {
            Notification.show("Оберіть коректну групу.");
            return;
        }
        StudentEntity student = buildStudentEntity(group);
        StudentPassportEntity passport = buildPassportEntity();
        StudentInfoEntity info = buildInfoEntity();
        StudentEducationEntity edu = buildEducationEntity();

        try {
            registrationService.saveStudentWithDetails(student, passport, info, edu, normalize(reportType.getValue()));
            Notification.show("Студента збережено.");
            close();
        } catch (Exception exception) {
            Notification.show("Не вдалося зберегти студента: " + exception.getMessage());
        }
    }

    private StudentEntity buildStudentEntity(StudentGroupEntity group) {
        StudentEntity student = new StudentEntity();
        student.setSurname(normalize(lastName.getValue()));
        student.setName(normalize(firstName.getValue()));
        student.setPatronymic(normalize(middleName.getValue()));
        student.setGroup(group);
        student.setFaculty(group.getSpecialty().getFaculty());
        student.setRecordBookNumber(normalize(recordBook.getValue()));
        return student;
    }

    private StudentPassportEntity buildPassportEntity() {
        StudentPassportEntity passport = new StudentPassportEntity();
        passport.setSeries(normalize(passportSeries.getValue()));
        passport.setNumber(normalize(passportNumber.getValue()));
        passport.setNameEng(normalize(firstNameEng.getValue()));
        passport.setSurnameEng(normalize(lastNameEng.getValue()));
        if (issueDate.getValue() != null) {
            passport.setIssueDate(String.valueOf(Date.valueOf(issueDate.getValue())));
        }
        if (expireDate.getValue() != null) {
            passport.setExpireDate(String.valueOf(Date.valueOf(expireDate.getValue())));
        }
        passport.setNationality(normalize(nationality.getValue()));
        passport.setSex(gender.getValue());
        return passport;
    }

    private StudentInfoEntity buildInfoEntity() {
        StudentInfoEntity info = new StudentInfoEntity();
        info.setAddress(normalize(address.getValue()));
        info.setPhone(normalize(phone.getValue()));
        info.setEmail(normalize(email.getValue()));
        return info;
    }

    private StudentEducationEntity buildEducationEntity() {
        StudentEducationEntity edu = new StudentEducationEntity();
        edu.setSeries(normalize(docSeries.getValue()));
        edu.setNumber(normalize(docNumber.getValue()));
        return edu;
    }

    private boolean validateForm() {
        clearValidation();
        List<String> errors = new ArrayList<>(validatePatterns());
        if (!hasText(lastName.getValue())) {
            lastName.setInvalid(true);
            errors.add("Прізвище обов'язкове");
        }
        if (!hasText(firstName.getValue())) {
            firstName.setInvalid(true);
            errors.add("Ім'я обов'язкове");
        }
        if (groupSelect.getValue() == null) {
            groupSelect.setInvalid(true);
            errors.add("Оберіть групу");
        }
        if (!hasText(passportSeries.getValue())) {
            passportSeries.setInvalid(true);
            errors.add("Серія паспорта обов'язкова");
        }
        if (!hasText(passportNumber.getValue())) {
            passportNumber.setInvalid(true);
            errors.add("Номер паспорта обов'язковий");
        }
        if (issueDate.getValue() == null) {
            issueDate.setInvalid(true);
            errors.add("Вкажіть дату видачі паспорта");
        }
        if (expireDate.getValue() == null) {
            expireDate.setInvalid(true);
            errors.add("Вкажіть термін дії паспорта");
        }
        if (!hasText(nationality.getValue())) {
            nationality.setInvalid(true);
            errors.add("Національність обов'язкова");
        }
        if (gender.getValue() == null) {
            gender.setInvalid(true);
            errors.add("Стать обов'язкова");
        }
        if (!hasText(phone.getValue())) {
            phone.setInvalid(true);
            errors.add("Телефон обов'язковий");
        }
        if (!hasText(email.getValue())) {
            email.setInvalid(true);
            errors.add("Email обов'язковий");
        }
        if (!hasText(address.getValue())) {
            address.setInvalid(true);
            errors.add("Адреса обов'язкова");
        }

        if (!errors.isEmpty()) {
            Notification.show(String.join("; ", errors));
            return false;
        }
        return true;
    }

    private List<String> validatePatterns() {
        List<String> errors = new ArrayList<>();
        validationPatterns.forEach((field, pattern) -> {
            String value = field.getValue();
            if (hasText(value) && !pattern.matcher(value).matches()) {
                field.setInvalid(true);
                String message = field.getErrorMessage();
                if (message == null || message.isBlank()) {
                    message = "Некоректне значення у полі " + field.getLabel();
                }
                errors.add(message);
            }
        });
        return errors;
    }

    private void clearValidation() {
        validationPatterns.keySet().forEach(field -> field.setInvalid(false));
        lastName.setInvalid(false);
        firstName.setInvalid(false);
        groupSelect.setInvalid(false);
        passportSeries.setInvalid(false);
        passportNumber.setInvalid(false);
        issueDate.setInvalid(false);
        expireDate.setInvalid(false);
        nationality.setInvalid(false);
        gender.setInvalid(false);
        phone.setInvalid(false);
        email.setInvalid(false);
        address.setInvalid(false);
        recordBook.setInvalid(false);
        docSeries.setInvalid(false);
        docNumber.setInvalid(false);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void showCancelConfirmation() {
        ConfirmDialog dialog = new ConfirmDialog(
                "Вихід",
                "Незбережені дані буде втрачено. Вийти?",
                "Так", event -> close(),
                "Ні", event -> {});
        dialog.open();
    }
}
