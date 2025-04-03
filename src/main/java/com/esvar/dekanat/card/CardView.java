package com.esvar.dekanat.card;


import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.service.*;
import com.esvar.dekanat.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


//todo: Додати попередження при виході з сторінки чи оновленні сторінки якщо є незбережені дані
//todo обдумати чи потрібно тут працювати з відомостями
//todo розробити створення картки студента
//todo розробити відправку в архів
//todo оновити дизайн та додати обробку додавання відомості для всієї групи

@PageTitle("Перегляд карток | Деканат")
@Route(value = "card", layout = MainLayout.class)
@PermitAll
public class CardView extends Div {

    private final GroupService groupService;
    private final StudentService studentService;
    private final StudentPassportService studentPassportService;
    private final StudentInfoService studentInfoService;
    private final StudentEducationService studentEducationService;
    private final StudentReportService studentReportService;
    private final ReportService reportService;


    private VerticalLayout mainLayout = new VerticalLayout();
    private HorizontalLayout leftLayout1Page = new HorizontalLayout();
    private HorizontalLayout rightLayout1Page = new HorizontalLayout();
    private HorizontalLayout selectors = new HorizontalLayout();
    private Select<String> selectStudent = new Select<>();
    private Select<String> selectGroup = new Select<>();
    private Tabs tabs = new Tabs();

    Grid<ReportEntity> orderGrid = new Grid<>(ReportEntity.class, false);

    // Buttons
    private Button addCardButton = new Button("Додати картку");
    private Button sendToArchiveButton = new Button("Відправити в архів");
    private Button editButton = new Button("Редагувати");
    private Button submitDataButton = new Button("Внести відомість");

    // Additional Selects and Inputs
    private Select<String> typeOfInformationSelect = new Select<>();
    private DatePicker datePicker = new DatePicker("Дата");
    private TextField numberField = new TextField("Номер");
    private Select<String> studentOrGroupSelect = new Select<>();

    private TextField lastNameUkrField = new TextField();
    private TextField firstNameUkrField = new TextField();
    private TextField middleNameUkrField = new TextField();
    private TextField lastNameEngField = new TextField();
    private TextField firstNameEngField = new TextField();
    private Select<String> groupSelect = new Select<>();
    private Select<String> courseSelect = new Select<>();
    private TextField groupNumberField= new TextField();
    private Select<String> admissionYearSelect = new Select<>();
    private TextField recordBookNumberField = new TextField();
    private TextField caseNumberField = new TextField();
    private TextField idCodeField = new TextField();
    private TextField unzrField = new TextField();
    private DatePicker birthDatePicker = new DatePicker();
    private Select<String> nationalityField = new Select<>();
    private Select<String> regionSelect = new Select<>();
    private TextField indexField = new TextField();
    private TextField fullAddressField = new TextField();
    private TextField phoneNumberField = new TextField();
    private TextField emailField = new TextField();
    private MultiSelectComboBox<String> benefitsSelect = new MultiSelectComboBox<>();
    private TextField personNumberEDEBOField = new TextField();
    private TextField studentCardNumberEDEBOField = new TextField();
    private Select<String> genderSelect = new Select<>();
    private TextField passportSeriesField = new TextField();
    private TextField passportNumberField = new TextField();
    private DatePicker passportIssueDatePicker = new DatePicker();
    private TextField passportIssuedByField = new TextField();
    private DatePicker passportExpiryDatePicker = new DatePicker();
    private Select<String> educationFormSelect = new Select<>();
    private Select<String> degreeSelect = new Select<>();
    private Select<String> admissionConditionSelect = new Select<>();
    private Select<String> paymentSourceSelect = new Select<>();
    private TextField contractNumberField = new TextField();
    private TextField amountField = new TextField();
    private TextField documentSeriesField = new TextField();
    private TextField documentNumberField = new TextField();
    private DatePicker documentIssueDatePicker = new DatePicker();
    private TextField institutionNameField = new TextField();
    private TextField institutionNameEngField = new TextField();
    private Checkbox distinctionCheckbox = new Checkbox();
    private Select<String> documentTypeSelect = new Select<>();
    private TextField diplomaSeriesField = new TextField();
    private TextField diplomaNumberField = new TextField();
    private DatePicker graduationDatePicker = new DatePicker();
    private TextField appendixNumberField = new TextField();
    private TextField thesisTitleUkrField = new TextField();
    private TextField thesisTitleEngField = new TextField();



    private StudentEntity studentEntity;
    private StudentPassportEntity studentPassportEntity;
    private StudentInfoEntity studentInfoEntity;
    private StudentEducationEntity studentEducationEntity;


    public CardView(GroupService groupService, StudentService studentService, StudentPassportService studentPassportService, StudentInfoService studentInfoService, StudentEducationService studentEducationService, StudentReportService studentReportService, ReportService reportService) {
        this.groupService = groupService;
        this.studentService = studentService;
        this.studentPassportService = studentPassportService;
        this.studentInfoService = studentInfoService;
        this.studentEducationService = studentEducationService;
        this.studentReportService = studentReportService;
        this.reportService = reportService;


        // Setup selectors
        selectStudent.setReadOnly(true);
        selectStudent.setLabel("Студент");
        selectStudent.setPlaceholder("Оберіть студента");
        selectStudent.setWidth("300px");
        selectStudent.getStyle().set("padding", "0");






        selectGroup.setLabel("Група");
        selectGroup.setItems(groupService.getGroupsDTO().stream().map(GroupDTO::toString).collect(Collectors.toList()));
        selectGroup.setPlaceholder("Оберіть групу");
        selectGroup.setWidth("300px");
        selectGroup.getStyle().set("padding", "0");

        selectGroup.addValueChangeListener(selectStringComponentValueChangeEvent -> {
            if (selectGroup.getValue() != null) {
                selectStudent.setItems(studentService.getStudentByGroupId(groupService.getGroupIdByCode(selectGroup.getValue())).stream().map(StudentEntity::getFullName).collect(Collectors.toList()));
                selectStudent.setReadOnly(false);
            }
        });

        selectors.add(selectGroup, selectStudent);
        selectors.setWidth("100%");

        typeOfInformationSelect.setLabel("Тип відомості");
        typeOfInformationSelect.setItems(
                "Зарахований",
                "Відрахований",
                "Академвідпустка",
                "Поновлений",
                "Переведений на наступний курс",
                "Такий що закінчив навчання"
        );

        typeOfInformationSelect.getStyle().set("padding", "0");
        typeOfInformationSelect.setWidth("100%");

        datePicker.getStyle().set("padding", "0");
        datePicker.setWidth("100%");
        datePicker.setI18n(setLocal());

        numberField.getStyle().set("padding", "0");
        numberField.setWidth("100%");

        studentOrGroupSelect.setLabel("Тип");
        studentOrGroupSelect.setItems("Один студент", "Вся група");
        studentOrGroupSelect.setWidth("100%");
        studentOrGroupSelect.getStyle().set("padding", "0");

        submitDataButton.setWidth("100%");
        submitDataButton.getStyle().set("padding", "0");


// Create the additional controls layout
        HorizontalLayout additionalControlsLayout = new HorizontalLayout();
        additionalControlsLayout.add(typeOfInformationSelect, datePicker, numberField, studentOrGroupSelect, submitDataButton);
        additionalControlsLayout.setAlignSelf(FlexComponent.Alignment.END, submitDataButton);
        additionalControlsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        additionalControlsLayout.setWidth("100%");
        additionalControlsLayout.getStyle().set("padding", "0");

        // Button Layout
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(selectGroup, selectStudent, addCardButton, sendToArchiveButton, editButton);
        buttonLayout.setWidth("100%");
        buttonLayout.setSpacing(true);
        buttonLayout.getStyle().set("padding", "0");
        buttonLayout.setAlignItems(FlexComponent.Alignment.BASELINE);


        orderGrid.addColumn(ReportEntity::getOrderNumber).setHeader("№ наказу").setWidth("20%");
        orderGrid.addColumn(ReportEntity::getStatus).setHeader("Стан").setWidth("40%");
        orderGrid.addColumn(ReportEntity::getDate).setHeader("Дата").setWidth("40%");
        orderGrid.getStyle().set("border", "1px solid #ddd");
        orderGrid.getStyle().set("border-radius", "8px");
        orderGrid.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        orderGrid.getStyle().set("padding", "20px");
        orderGrid.getStyle().set("position", "relative");
        orderGrid.getStyle().set("background", "white");
        orderGrid.getStyle().set("min-height", "230px");
        orderGrid.addAttachListener(event -> {
            orderGrid.getElement().executeJs(
                    "this.shadowRoot.querySelector('#table').style.marginTop = '5px'; " +
                            "this.shadowRoot.querySelector('#table').style.marginBottom = '5px'; "
            );
        });

        Div orderGridWrapper = new Div();
        orderGridWrapper.getStyle().set("position", "relative");

        Span orderLeftTitle = new Span("Накази");
        orderLeftTitle.getStyle().set("position", "absolute");
        orderLeftTitle.getStyle().set("top", "-10px");
        orderLeftTitle.getStyle().set("left", "20px");
        orderLeftTitle.getStyle().set("background", "white");
        orderLeftTitle.getStyle().set("padding", "0 10px");
        orderLeftTitle.getStyle().set("font-weight", "bold");
        orderLeftTitle.getStyle().set("z-index", "100000");

        orderGridWrapper.add(orderLeftTitle, orderGrid);
        orderGridWrapper.getStyle().set("width", "100%");

        // Create a main layout for the left and right sections
        HorizontalLayout orderLayout = new HorizontalLayout();
        orderLayout.setWidth("100%");


// Additional Controls Layout on the right side
        VerticalLayout rightColumn = new VerticalLayout();
        rightColumn.add(typeOfInformationSelect, datePicker, numberField, studentOrGroupSelect, submitDataButton);
        rightColumn.setAlignItems(FlexComponent.Alignment.END); // Align items to the end of the column
        rightColumn.setWidth("100%"); // Adjust width as needed
        rightColumn.getStyle().set("padding", "0px");

        Div InningLayoutWrapper = new Div();
        InningLayoutWrapper.getStyle().set("border", "1px solid #ddd");
        InningLayoutWrapper.getStyle().set("border-radius", "8px");
        InningLayoutWrapper.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        InningLayoutWrapper.getStyle().set("padding", "20px");
        InningLayoutWrapper.getStyle().set("position", "relative");
        InningLayoutWrapper.getStyle().set("background", "white");
        InningLayoutWrapper.getStyle().set("width", "30%");

        Span orderTitle = new Span("Внесення");
        orderTitle.getStyle().set("position", "absolute");
        orderTitle.getStyle().set("top", "-10px");
        orderTitle.getStyle().set("left", "20px");
        orderTitle.getStyle().set("background", "white");
        orderTitle.getStyle().set("padding", "0 10px");
        orderTitle.getStyle().set("font-weight", "bold");

        InningLayoutWrapper.add(orderTitle, rightColumn);



// Add the columns to the main layout
        orderLayout.add(orderGridWrapper,InningLayoutWrapper);
        orderLayout.setSpacing(false); // Adjust spacing between columns
        orderLayout.getStyle().set("padding", "0px");
        orderLayout.getStyle().set("gap", "10px");


        // Setup tabs
        Tab mainInfoTab = new Tab("Основна Інформація");
        Tab additionalInfoTab = new Tab("Додаткова Інформація");
        Tab passportInfoTab = new Tab("Паспортна Інформація");
        Tab educationDocumentsTab = new Tab("Документи про освіту");
        tabs.add(mainInfoTab, passportInfoTab, additionalInfoTab, educationDocumentsTab);

        // Main info text fields
        lastNameUkrField = new TextField("Прізвище");
        lastNameUkrField.setWidth("24%");

        firstNameUkrField = new TextField("Ім'я");
        firstNameUkrField.setWidth("24%");

        middleNameUkrField = new TextField("По батькові");
        middleNameUkrField.setWidth("24%");

        lastNameEngField = new TextField("Прізвище (англ)");
        lastNameEngField.setWidth("24%");

        firstNameEngField = new TextField("Ім'я (англ)");
        firstNameEngField.setWidth("24%");

        groupSelect.setLabel("Група");
        groupSelect.setWidth("24%");
        groupSelect.setItems(groupService.getAllGroups().stream().map(StudentGroupEntity::getSpecialty).map(SpecialtyEntity::getAbbreviation).collect(Collectors.toList()));

        courseSelect = new Select<>();
        courseSelect.setLabel("Курс");
        courseSelect.setWidth("24%");
        courseSelect.setItems("1", "2", "3", "4")  ;

        groupNumberField = new TextField("Номер групи");
        groupNumberField.setWidth("24%");
        groupNumberField.setPattern("[1-9]{1,}"); // Дозволяє тільки цифри від 1 до 9
        groupNumberField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value.matches("[1-9]+")) {
                groupNumberField.setErrorMessage(null); // Очистити повідомлення про помилку
            } else {
                groupNumberField.setErrorMessage("Введіть цифру від 1 до 9");
                Notification.show("Неправильний ввід. Введіть тільки цифри від 1 до 9.");
            }
        });

        admissionYearSelect = new Select<>();
        admissionYearSelect.setLabel("Рік вступу");
        admissionYearSelect.setWidth("24%");
        admissionYearSelect.setItems
                (
                        IntStream.rangeClosed(Year.now().getValue() - 10, Year.now().getValue())
                                .mapToObj(String::valueOf)
                                .collect(Collectors.toList())
                ); //todo визначити кількість років


        recordBookNumberField = new TextField("Номер заліковки");
        recordBookNumberField.setWidth("24%");
        recordBookNumberField.setPattern("[0-9]{1,}"); // Дозволяє тільки цифри від 1 до 9
        recordBookNumberField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value.matches("[0-9]+")) {
                recordBookNumberField.setErrorMessage(null); // Очистити повідомлення про помилку
            } else {
                recordBookNumberField.setErrorMessage("Введіть цифри від 0 до 9");
                Notification.show("Неправильний ввід. Введіть тільки цифри від 0 до 9.");
            }
        });

        // Add border and title to leftLayout1Page
        Div leftLayoutWrapper = new Div();
        leftLayoutWrapper.getStyle().set("border", "1px solid #ddd");
        leftLayoutWrapper.getStyle().set("border-radius", "8px");
        leftLayoutWrapper.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        leftLayoutWrapper.getStyle().set("padding", "20px");
        leftLayoutWrapper.getStyle().set("position", "relative");
        leftLayoutWrapper.getStyle().set("background", "white");

        Span leftLayoutTitle = new Span("Персональні дані");
        leftLayoutTitle.getStyle().set("position", "absolute");
        leftLayoutTitle.getStyle().set("top", "-10px");
        leftLayoutTitle.getStyle().set("left", "20px");
        leftLayoutTitle.getStyle().set("background", "white");
        leftLayoutTitle.getStyle().set("padding", "0 10px");
        leftLayoutTitle.getStyle().set("font-weight", "bold");

        leftLayoutWrapper.add(leftLayoutTitle, leftLayout1Page);

        // Add border and title to rightLayout1Page
        Div rightLayoutWrapper = new Div();
        rightLayoutWrapper.getStyle().set("border", "1px solid #ddd");
        rightLayoutWrapper.getStyle().set("border-radius", "8px");
        rightLayoutWrapper.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        rightLayoutWrapper.getStyle().set("padding", "20px");
        rightLayoutWrapper.getStyle().set("position", "relative");
        rightLayoutWrapper.getStyle().set("background", "white");

        Span rightLayoutTitle = new Span("Академічні дані");
        rightLayoutTitle.getStyle().set("position", "absolute");
        rightLayoutTitle.getStyle().set("top", "-10px");
        rightLayoutTitle.getStyle().set("left", "20px");
        rightLayoutTitle.getStyle().set("background", "white");
        rightLayoutTitle.getStyle().set("padding", "0 10px");
        rightLayoutTitle.getStyle().set("font-weight", "bold");

        rightLayoutWrapper.add(rightLayoutTitle, rightLayout1Page);

        leftLayout1Page.add(lastNameUkrField, firstNameUkrField, middleNameUkrField, lastNameEngField, firstNameEngField);
        rightLayout1Page.add(groupSelect, courseSelect, groupNumberField, admissionYearSelect, recordBookNumberField);

        // Layout for main info text fields
        VerticalLayout mainInfoLayout = new VerticalLayout();
        mainInfoLayout.setWidth("100%");
        mainInfoLayout.add(leftLayoutWrapper, rightLayoutWrapper);
        mainInfoLayout.getStyle().set("padding", "0px");
        leftLayoutWrapper.getStyle().set("width", "97%");
        rightLayoutWrapper.getStyle().set("width", "97%");

// Additional info text fields
        caseNumberField = new TextField("Номер справи");
        idCodeField = new TextField("Ідентифікаційний код");
        idCodeField.setPattern("[0-9]{1,}"); // Дозволяє тільки цифри
        idCodeField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value.matches("[0-9]+")) {
                idCodeField.setErrorMessage(null); // Очистити повідомлення про помилку
            } else {
                idCodeField.setErrorMessage("Введіть тільки цифри");
                Notification.show("Неправильний ввід. Введіть тільки цифри.");
            }
        });
        unzrField = new TextField("УНЗР");
        birthDatePicker = new DatePicker("Дата народження");
        birthDatePicker.setI18n(setLocal());
        nationalityField = new Select<>();
        nationalityField.setLabel("Національність");
        nationalityField.setItems("Україна", "Іноземець");
        regionSelect = new Select<>();
        regionSelect.setLabel("Область");
        regionSelect.setItems(
                "Вінницька область",
                "Волинська область",
                "Дніпропетровська область",
                "Донецька область",
                "Житомирська область",
                "Закарпатська область",
                "Запорізька область",
                "Івано-Франківська область",
                "Київська область",
                "Кіровоградська область",
                "Луганська область",
                "Львівська область",
                "Миколаївська область",
                "Одеська область",
                "Полтавська область",
                "Рівненська область",
                "Сумська область",
                "Тернопільська область",
                "Харківська область",
                "Херсонська область",
                "Хмельницька область",
                "Черкаська область",
                "Чернівецька область",
                "Чернігівська область",
                "Автономна Республіка Крим",
                "м. Київ",
                "м. Севастополь"
        );
        indexField = new TextField("Індекс");
        indexField.setPattern("[0-9]{1,5}"); // Дозволяет только цифры от 0 до 9, максимум 5 цифр
        indexField.setMaxLength(5); // Ограничение на 5 символов

        indexField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value.matches("[0-9]{1,5}")) {
                indexField.setErrorMessage(null); // Очистить сообщение об ошибке
            } else {
                indexField.setErrorMessage("Індекс повинен містити до 5 цифр");
                Notification.show("Неправильний ввід. Введіть до 5 цифр.");
            }
        });
        fullAddressField = new TextField("Повна адреса");
        phoneNumberField = new TextField("Номер телефону");
        emailField = new TextField("E-mail");
        benefitsSelect = new MultiSelectComboBox<>();
        benefitsSelect.setLabel("Пільги");
        benefitsSelect.setItems("Пільга 1", "Пільга 2", "Пільга 3"); // Приклад елементів
        // Text fields for ЄДЕБО numbers
        personNumberEDEBOField = new TextField("Номер фіз. особи ЄДЕБО");
        studentCardNumberEDEBOField = new TextField("Номер картки здобувача ЄДЕБО");

// Set the pattern to allow only digits and enforce a minimum of 7 characters
        personNumberEDEBOField.setPattern("\\d{7,}");
        studentCardNumberEDEBOField.setPattern("\\d{7,}");

// Set error messages and add value change listeners to validate input
        personNumberEDEBOField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value.matches("\\d{7,}")) {
                personNumberEDEBOField.setErrorMessage(null);
            } else {
                personNumberEDEBOField.setErrorMessage("Введіть мінімум 7 цифр");
                Notification.show("Неправильний ввід. Введіть мінімум 7 цифр.");
            }
        });

        studentCardNumberEDEBOField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value.matches("\\d{7,}")) {
                studentCardNumberEDEBOField.setErrorMessage(null);
            } else {
                studentCardNumberEDEBOField.setErrorMessage("Введіть мінімум 7 цифр");
                Notification.show("Неправильний ввід. Введіть мінімум 7 цифр.");
            }
        });

// Add these fields to the appropriate layout
        VerticalLayout edeboFieldsLayout = new VerticalLayout();
        edeboFieldsLayout.add();
        genderSelect = new Select<>();
        genderSelect.setLabel("Стать");
        genderSelect.setItems("Чоловіча", "Жіноча");

        passportSeriesField = new TextField("Серія паспорту");
        passportNumberField = new TextField("№ паспорту");
        passportNumberField.setPattern("[0-9]{1,}"); // Дозволяє тільки цифри
        passportNumberField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value.matches("[0-9]+")) {
                passportNumberField.setErrorMessage(null); // Очистити повідомлення про помилку
            } else {
                passportNumberField.setErrorMessage("Введіть тільки цифри");
                Notification.show("Неправильний ввід. Введіть тільки цифри.");
            }
        });
        passportIssueDatePicker = new DatePicker("Коли виданий");
        passportIssueDatePicker.setI18n(setLocal());
        passportIssuedByField = new TextField("Ким виданий");
        passportExpiryDatePicker = new DatePicker("Коли закінчиться дія паспорту");
        passportExpiryDatePicker.setI18n(setLocal());
        educationFormSelect = new Select<>();
        educationFormSelect.setLabel("Форма навчання");
        educationFormSelect.setItems("Денна", "Заочна");

        degreeSelect = new Select<>();
        degreeSelect.setLabel("Здобуття звання");
        degreeSelect.setItems(
                "Бакалавр",
                "Бакалавр (за скороченим строком)",
                "Спеціаліст",
                "Спеціаліст (за скороченим строком)",
                "Магістр"
        );
        admissionConditionSelect = new Select<>();
        admissionConditionSelect.setLabel("Умови вступу");
        admissionConditionSelect.setItems("За конкурсом", "За конкурсом без стажу", "У порядку переведення", "У порядку позаконкурсного набору", "Як відмінника"); // Example items
        paymentSourceSelect = new Select<>();
        paymentSourceSelect.setLabel("Тип особи");
        paymentSourceSelect.setItems("Фізичних осіб", "Юридичних осіб", "Держбюджет");

        contractNumberField = new TextField("Договір за номером");
        contractNumberField.setPattern("[0-9]{1,}"); // Дозволяє тільки цифри від 0 до 9
        contractNumberField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value.matches("[0-9]+")) {
                contractNumberField.setErrorMessage(null); // Очистити повідомлення про помилку
            } else {
                contractNumberField.setErrorMessage("Введіть цифри від 0 до 9");
                Notification.show("Неправильний ввід. Введіть тільки цифри від 0 до 9.");
            }
        });
        amountField = new TextField("Сума");
        amountField.setPattern("[0-9]{1,}"); // Дозволяє тільки цифри від 0 до 9
        amountField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value.matches("[0-9]+")) {
                amountField.setErrorMessage(null); // Очистити повідомлення про помилку
            } else {
                amountField.setErrorMessage("Введіть цифри від 0 до 9");
                Notification.show("Неправильний ввід. Введіть тільки цифри від 0 до 9.");
            }
        });


// Group 2: Address Details
        Div addressDetailsWrapper = new Div();
        addressDetailsWrapper.getStyle().set("border", "1px solid #ddd");
        addressDetailsWrapper.getStyle().set("border-radius", "8px");
        addressDetailsWrapper.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        addressDetailsWrapper.getStyle().set("padding", "20px");
        addressDetailsWrapper.getStyle().set("position", "relative");
        addressDetailsWrapper.getStyle().set("background", "white");
        addressDetailsWrapper.getStyle().set("width", "97%"); // Set the width to 97%

        Span addressDetailsTitle = new Span("Адреса");
        addressDetailsTitle.getStyle().set("position", "absolute");
        addressDetailsTitle.getStyle().set("top", "-10px");
        addressDetailsTitle.getStyle().set("left", "20px");
        addressDetailsTitle.getStyle().set("background", "white");
        addressDetailsTitle.getStyle().set("padding", "0 10px");
        addressDetailsTitle.getStyle().set("font-weight", "bold");

        FormLayout addressDetailsLayout = new FormLayout();
        addressDetailsLayout.add(regionSelect, indexField, fullAddressField);
        addressDetailsLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1), // 1 column for narrow layout
                new FormLayout.ResponsiveStep("500px", 2) // 2 columns for wider layout
        );
        addressDetailsLayout.setColspan(fullAddressField, 2);

        addressDetailsWrapper.add(addressDetailsTitle, addressDetailsLayout);

// Group 3: Passport Details
        Div passportDetailsWrapper = new Div();
        passportDetailsWrapper.getStyle().set("border", "1px solid #ddd");
        passportDetailsWrapper.getStyle().set("border-radius", "8px");
        passportDetailsWrapper.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        passportDetailsWrapper.getStyle().set("padding", "20px");
        passportDetailsWrapper.getStyle().set("position", "relative");
        passportDetailsWrapper.getStyle().set("background", "white");

        Span passportDetailsTitle = new Span("Паспортні дані");
        passportDetailsTitle.getStyle().set("position", "absolute");
        passportDetailsTitle.getStyle().set("top", "-10px");
        passportDetailsTitle.getStyle().set("left", "20px");
        passportDetailsTitle.getStyle().set("background", "white");
        passportDetailsTitle.getStyle().set("padding", "0 10px");
        passportDetailsTitle.getStyle().set("font-weight", "bold");

        FormLayout passportDetailsLayout = new FormLayout();
        passportDetailsLayout.add(passportSeriesField, passportNumberField, passportIssueDatePicker, passportExpiryDatePicker, passportIssuedByField,  idCodeField,unzrField, birthDatePicker, nationalityField, genderSelect,personNumberEDEBOField, studentCardNumberEDEBOField);

        passportDetailsWrapper.add(passportDetailsTitle, passportDetailsLayout);

// Group 4: Education Details
        Div educationDetailsWrapper = new Div();
        educationDetailsWrapper.getStyle().set("border", "1px solid #ddd");
        educationDetailsWrapper.getStyle().set("border-radius", "8px");
        educationDetailsWrapper.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        educationDetailsWrapper.getStyle().set("padding", "20px");
        educationDetailsWrapper.getStyle().set("position", "relative");
        educationDetailsWrapper.getStyle().set("background", "white");
        educationDetailsWrapper.getStyle().set("width", "97%"); // Set the width to 97%

        Span educationDetailsTitle = new Span("Дані про навчання");
        educationDetailsTitle.getStyle().set("position", "absolute");
        educationDetailsTitle.getStyle().set("top", "-10px");
        educationDetailsTitle.getStyle().set("left", "20px");
        educationDetailsTitle.getStyle().set("background", "white");
        educationDetailsTitle.getStyle().set("padding", "0 10px");
        educationDetailsTitle.getStyle().set("font-weight", "bold");

        FormLayout educationDetailsLayout = new FormLayout();
        educationDetailsLayout.add(caseNumberField, educationFormSelect, degreeSelect, admissionConditionSelect, paymentSourceSelect, contractNumberField, amountField, benefitsSelect);

        educationDetailsWrapper.add(educationDetailsTitle, educationDetailsLayout);

        // Group 4: Education Details
        Div contactDetailsWrapper = new Div();
        contactDetailsWrapper.getStyle().set("border", "1px solid #ddd");
        contactDetailsWrapper.getStyle().set("border-radius", "8px");
        contactDetailsWrapper.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        contactDetailsWrapper.getStyle().set("padding", "20px");
        contactDetailsWrapper.getStyle().set("position", "relative");
        contactDetailsWrapper.getStyle().set("background", "white");
        contactDetailsWrapper.getStyle().set("width", "97%"); // Set the width to 97%

        Span contactDetailsTitle = new Span("Контактні дані");
        contactDetailsTitle.getStyle().set("position", "absolute");
        contactDetailsTitle.getStyle().set("top", "-10px");
        contactDetailsTitle.getStyle().set("left", "20px");
        contactDetailsTitle.getStyle().set("background", "white");
        contactDetailsTitle.getStyle().set("padding", "0 10px");
        contactDetailsTitle.getStyle().set("font-weight", "bold");

        FormLayout contactDetailsLayout = new FormLayout();
        contactDetailsLayout.add(phoneNumberField, emailField);

        contactDetailsWrapper.add(contactDetailsTitle, contactDetailsLayout);

// Layout for additional info text fields
        VerticalLayout additionalInfoLayout = new VerticalLayout();
        additionalInfoLayout.setWidth("100%");
        additionalInfoLayout.add(educationDetailsWrapper,contactDetailsWrapper, addressDetailsWrapper);
        additionalInfoLayout.getStyle().set("padding", "0px");

        VerticalLayout passportInfoLayout = new VerticalLayout();
        passportInfoLayout.setWidth("100%");
        passportInfoLayout.add(passportDetailsWrapper);
        passportInfoLayout.getStyle().set("padding", "0px");

// Group 1: General Education Documents
        Div generalEducationDocumentsWrapper = new Div();
        generalEducationDocumentsWrapper.getStyle().set("border", "1px solid #ddd");
        generalEducationDocumentsWrapper.getStyle().set("border-radius", "8px");
        generalEducationDocumentsWrapper.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        generalEducationDocumentsWrapper.getStyle().set("padding", "20px");
        generalEducationDocumentsWrapper.getStyle().set("position", "relative");
        generalEducationDocumentsWrapper.getStyle().set("background", "white");
        generalEducationDocumentsWrapper.getStyle().set("width", "97%"); // Set the width to 97%

        Span generalEducationDocumentsTitle = new Span("Попередня освіта");
        generalEducationDocumentsTitle.getStyle().set("position", "absolute");
        generalEducationDocumentsTitle.getStyle().set("top", "-10px");
        generalEducationDocumentsTitle.getStyle().set("left", "20px");
        generalEducationDocumentsTitle.getStyle().set("background", "white");
        generalEducationDocumentsTitle.getStyle().set("padding", "0 10px");
        generalEducationDocumentsTitle.getStyle().set("font-weight", "bold");

        documentSeriesField = new TextField("Серія документу");
        documentNumberField = new TextField("№ документу");
        documentIssueDatePicker = new DatePicker("Дата видачі");
        documentIssueDatePicker.setI18n(setLocal());
        institutionNameField = new TextField("Назва навчального закладу");
        institutionNameEngField = new TextField("Назва навчального закладу (англ)");
        distinctionCheckbox = new Checkbox("З відзнакою");

// Create the dropdown (select) field for document type
        documentTypeSelect = new Select<>();
        documentTypeSelect.setLabel("Тип документу");
        documentTypeSelect.setItems("Атестат", "Диплом", "Сертифікат", "Інший");
        documentTypeSelect.setPlaceholder("Оберіть тип документу");

// Arrange the fields in a FormLayout
        FormLayout generalEducationDocumentsLayout = new FormLayout();

// Create a horizontal layout for the series, number, and date fields
        HorizontalLayout seriesNumberDateLayout = new HorizontalLayout();
        seriesNumberDateLayout.setWidthFull(); // Make the horizontal layout full width
        seriesNumberDateLayout.setSpacing(true); // Add spacing between the fields
        seriesNumberDateLayout.add(documentSeriesField, documentNumberField, documentIssueDatePicker);
        seriesNumberDateLayout.setFlexGrow(1, documentSeriesField, documentNumberField, documentIssueDatePicker); // Make each field take up equal space

// Add components to the FormLayout
        generalEducationDocumentsLayout.add(
                documentTypeSelect,
                distinctionCheckbox,
                seriesNumberDateLayout,
                institutionNameField,
                institutionNameEngField
        );

// Set responsive steps
        generalEducationDocumentsLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1), // 1 column for narrow layout
                new FormLayout.ResponsiveStep("500px", 1) // 2 columns for wider layout
        );

// Set colspan for distinctionCheckbox to align it properly
        generalEducationDocumentsLayout.setColspan(distinctionCheckbox, 1);

        generalEducationDocumentsWrapper.add(generalEducationDocumentsTitle, generalEducationDocumentsLayout);

// Group 2: Diploma-Specific Fields
        Div diplomaDocumentsWrapper = new Div();
        diplomaDocumentsWrapper.getStyle().set("border", "1px solid #ddd");
        diplomaDocumentsWrapper.getStyle().set("border-radius", "8px");
        diplomaDocumentsWrapper.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        diplomaDocumentsWrapper.getStyle().set("padding", "20px");
        diplomaDocumentsWrapper.getStyle().set("position", "relative");
        diplomaDocumentsWrapper.getStyle().set("background", "white");
        diplomaDocumentsWrapper.getStyle().set("width", "97%"); // Set the width to 97%

        Span diplomaSectionTitle = new Span("Диплом");
        diplomaSectionTitle.getStyle().set("position", "absolute");
        diplomaSectionTitle.getStyle().set("top", "-10px");
        diplomaSectionTitle.getStyle().set("left", "20px");
        diplomaSectionTitle.getStyle().set("background", "white");
        diplomaSectionTitle.getStyle().set("padding", "0 10px");
        diplomaSectionTitle.getStyle().set("font-weight", "bold");

// Add new fields for the diploma
        diplomaSeriesField = new TextField("Серія диплому");
        diplomaNumberField = new TextField("№ диплому");
        graduationDatePicker = new DatePicker("Дата випуску");
        graduationDatePicker.setI18n(setLocal());
        appendixNumberField = new TextField("Номер додатку");
        thesisTitleUkrField = new TextField("Тема дипломної роботи (укр)");
        thesisTitleEngField = new TextField("Тема дипломної роботи (англ)");

// Create a horizontal layout for the diploma series, number, and graduation date fields
        HorizontalLayout diplomaLayout = new HorizontalLayout();
        diplomaLayout.setWidthFull();
        diplomaLayout.setSpacing(true);
        diplomaLayout.add(diplomaSeriesField, diplomaNumberField, graduationDatePicker);
        diplomaLayout.setFlexGrow(1, diplomaSeriesField, diplomaNumberField, graduationDatePicker); // Equal space for fields

// Arrange diploma-specific fields in a FormLayout
        FormLayout diplomaDocumentsLayout = new FormLayout();
        diplomaDocumentsLayout.add(
                diplomaLayout,
                appendixNumberField,
                thesisTitleUkrField,
                thesisTitleEngField
        );

// Set responsive steps
        diplomaDocumentsLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 1)
        );

        diplomaDocumentsWrapper.add(diplomaSectionTitle, diplomaDocumentsLayout);


        // Update tab selection listener to include the new tab
        tabs.addSelectedChangeListener(event -> {
            mainLayout.removeAll();
            if (tabs.getSelectedTab().equals(mainInfoTab)) {
                mainLayout.add(buttonLayout, tabs, mainInfoLayout, orderLayout);
            } else if (tabs.getSelectedTab().equals(additionalInfoTab)) {
                mainLayout.add(buttonLayout, tabs, additionalInfoLayout);
            } else if (tabs.getSelectedTab().equals(passportInfoTab)) {
                mainLayout.add(buttonLayout, tabs, passportInfoLayout);
            } else if (tabs.getSelectedTab().equals(educationDocumentsTab)) {
                mainLayout.add(buttonLayout, tabs, generalEducationDocumentsWrapper, diplomaDocumentsWrapper);
            }
        });

        mainLayout.add(buttonLayout, tabs, mainInfoLayout, orderLayout);
        mainLayout.setWidth("100%");
        mainLayout.setHeight("100%");
        mainLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        add(mainLayout);
        setHeight("100%");


        //Вимкнення можливості редагування відомостей
        typeOfInformationSelect.setReadOnly(true);
        datePicker.setReadOnly(true);
        numberField.setReadOnly(true);
        studentOrGroupSelect.setReadOnly(true);
        submitDataButton.setEnabled(false);
        orderGrid.setEnabled(false);

        //Обробка вибору студента
        selectStudent.addValueChangeListener(selectStringComponentValueChangeEvent -> {
            if (selectStudent.getValue() != null) {



                studentEntity = studentService.getStudentForCard(selectGroup.getValue(), selectStudent.getValue());
                studentPassportEntity = studentPassportService.getPassportByStudentModel(studentEntity);
                studentInfoEntity = studentInfoService.getInfoByStudentModel(studentEntity);
                studentEducationEntity = studentEducationService.getEducationByStudentModel(studentEntity);

                //Персональні дані
                lastNameUkrField.setValue(studentEntity.getSurname());
                lastNameUkrField.setReadOnly(true);

                firstNameUkrField.setValue(studentEntity.getName());
                firstNameUkrField.setReadOnly(true);

                middleNameUkrField.setValue(studentEntity.getPatronymic());
                middleNameUkrField.setReadOnly(true);

                firstNameEngField.setValue(studentPassportEntity.getNameEng());
                firstNameEngField.setReadOnly(true);

                lastNameEngField.setValue(studentPassportEntity.getSurnameEng());
                lastNameEngField.setReadOnly(true);

//                //Академічні дані
                groupSelect.setValue(studentEntity.getGroup().getGroupCode().split("-")[0]);
                groupSelect.setReadOnly(true);

                courseSelect.setValue(studentEntity.getGroup().getGroupCode().split("-")[1]);
                courseSelect.setReadOnly(true);

                groupNumberField.setValue(studentEntity.getGroup().getGroupCode().split("-")[2]);
                groupNumberField.setReadOnly(true);

                admissionYearSelect.setValue(studentEntity.getGroup().getGroupCode().split("-")[3]);
                admissionYearSelect.setReadOnly(true);

                recordBookNumberField.setValue(studentEntity.getRecordBookNumber());
                recordBookNumberField.setReadOnly(true);

//                //Відомості
                orderGrid.setItems(studentReportService.getReportsByStudentId(studentEntity.getId()));


//                //Паспортні дані
                passportSeriesField.setValue(studentPassportEntity.getSeries());
                passportSeriesField.setReadOnly(true);

                passportNumberField.setValue(studentPassportEntity.getNumber());
                passportNumberField.setReadOnly(true);

                passportIssueDatePicker.setValue(studentPassportEntity.getIssueDate().toLocalDate());
                passportIssueDatePicker.setReadOnly(true);

                passportIssuedByField.setValue(studentPassportEntity.getIssuedBy());
                passportIssuedByField.setReadOnly(true);

                passportExpiryDatePicker.setValue(studentPassportEntity.getExpireDate().toLocalDate());
                passportExpiryDatePicker.setReadOnly(true);

                idCodeField.setValue(studentPassportEntity.getIdentificationNumber());
                idCodeField.setReadOnly(true);

                unzrField.setValue(studentPassportEntity.getUnzrCode());
                unzrField.setReadOnly(true);

                birthDatePicker.setValue(studentPassportEntity.getBirthdate().toLocalDate());
                birthDatePicker.setReadOnly(true);

                nationalityField.setValue(studentPassportEntity.getNationality());
                nationalityField.setReadOnly(true);

                genderSelect.setValue(studentPassportEntity.getSex().name());
                genderSelect.setReadOnly(true);

                personNumberEDEBOField.setValue(studentPassportEntity.getEdboNumberPhis());
                personNumberEDEBOField.setReadOnly(true);

                studentCardNumberEDEBOField.setValue(studentPassportEntity.getEdboNumberZdob());
                studentCardNumberEDEBOField.setReadOnly(true);
//
//                //Дані про наввчання
                caseNumberField.setValue(studentInfoEntity.getCaseNumber());
                caseNumberField.setReadOnly(true);

                educationFormSelect.setValue(studentInfoEntity.getFormStudy());
                educationFormSelect.setReadOnly(true);

                degreeSelect.setValue(studentInfoEntity.getDegree());
                degreeSelect.setReadOnly(true);

                admissionConditionSelect.setValue(studentInfoEntity.getEntryRequirements());
                admissionConditionSelect.setReadOnly(true);

                paymentSourceSelect.setValue(studentInfoEntity.getTypeOfIndividual());
                paymentSourceSelect.setReadOnly(true);

                contractNumberField.setValue(studentInfoEntity.getContractNumber());
                contractNumberField.setReadOnly(true);

                amountField.setValue(studentInfoEntity.getTotal());
                amountField.setReadOnly(true);

                benefitsSelect.setValue(Arrays.asList(studentInfoEntity.getBenefits().split(", ")));
                benefitsSelect.setReadOnly(true);

                //Контактні дані
                phoneNumberField.setValue(studentInfoEntity.getPhone());
                phoneNumberField.setReadOnly(true);

                emailField.setValue(studentInfoEntity.getEmail());
                emailField.setReadOnly(true);

                //Адреса
                regionSelect.setValue(studentInfoEntity.getRegion());
                regionSelect.setReadOnly(true);

                indexField.setValue(studentInfoEntity.getIndex());
                indexField.setReadOnly(true);

                fullAddressField.setValue(studentInfoEntity.getAddress());
                fullAddressField.setReadOnly(true);

                //Попередня освіта
                documentTypeSelect.setValue(studentEducationEntity.getTypeOfDocument());
                documentTypeSelect.setReadOnly(true);

                distinctionCheckbox.setValue(studentEducationEntity.getHonors() == 1);
                distinctionCheckbox.setReadOnly(true);

                documentSeriesField.setValue(studentEducationEntity.getSeries());
                documentSeriesField.setReadOnly(true);

                documentNumberField.setValue(studentEducationEntity.getNumber());
                documentNumberField.setReadOnly(true);

                documentIssueDatePicker.setValue(studentEducationEntity.getDateOfIssue().toLocalDate());
                documentIssueDatePicker.setReadOnly(true);

                institutionNameField.setValue(studentEducationEntity.getIssuedBy());
                institutionNameField.setReadOnly(true);

                institutionNameEngField.setValue(studentEducationEntity.getIssuedByEng());
                institutionNameEngField.setReadOnly(true);

                //Диплом
                diplomaSeriesField.setValue(studentEducationEntity.getDiplomaSeries());
                diplomaSeriesField.setReadOnly(true);

                diplomaNumberField.setValue(studentEducationEntity.getDiplomaNumber());
                diplomaNumberField.setReadOnly(true);

                graduationDatePicker.setValue(studentEducationEntity.getDateOfIssueDiploma().toLocalDate());
                graduationDatePicker.setReadOnly(true);

                appendixNumberField.setValue(studentEducationEntity.getNumberOfDodatok());
                appendixNumberField.setReadOnly(true);

                thesisTitleUkrField.setValue(studentEducationEntity.getThemeOfWork());
                thesisTitleUkrField.setReadOnly(true);

                thesisTitleEngField.setValue(studentEducationEntity.getThemeOfWorkEng());
                thesisTitleEngField.setReadOnly(true);
            }
        });

        //Обробка додавання нової відомості
        submitDataButton.addClickListener(buttonClickEvent -> {

            StudentEntity studentEntityMain = studentService.getStudentForCard(selectGroup.getValue(), selectStudent.getValue());

            if (studentOrGroupSelect.getValue().equals("Один студент")){

                ReportEntity reportEntity = new ReportEntity();
                reportEntity.setStudent(studentEntityMain);
                reportEntity.setStatus(typeOfInformationSelect.getValue());
                reportEntity.setDate(Date.valueOf(datePicker.getValue()));
                reportEntity.setOrderNumber(Long.valueOf(numberField.getValue()));

                reportService.saveReport(reportEntity);

            } else if (studentOrGroupSelect.getValue().equals("Вся група")){

                List<StudentEntity> studentModels = studentService.getStudentsForCard(selectGroup.getValue());

                studentModels.forEach(studentModel -> {

                    ReportEntity reportEntity = new ReportEntity();
                    reportEntity.setStudent(studentModel);
                    reportEntity.setStatus(typeOfInformationSelect.getValue());
                    reportEntity.setDate(Date.valueOf(datePicker.getValue()));
                    reportEntity.setOrderNumber(Long.valueOf(numberField.getValue()));

                    reportService.saveReport(reportEntity);

                });
            }

            orderGrid.setItems(reportService.getReports(studentEntityMain));

        });

        //обробка режиму редагування
        editButton.addClickListener(buttonClickEvent -> {
            MainLayout mainLayout = (MainLayout) UI.getCurrent().getChildren()
                    .filter(component -> component instanceof MainLayout)
                    .findFirst()
                    .orElse(null);
            if (editButton.getText().equals("Редагувати")){
                //Ввімкнення можливості редагування Відомостей
                typeOfInformationSelect.setReadOnly(false);
                datePicker.setReadOnly(false);
                numberField.setReadOnly(false);
                studentOrGroupSelect.setReadOnly(false);
                submitDataButton.setEnabled(true);
//                orderGrid.setEnabled(true);
                //Ввімкнення можливості редагування Персональних даних
                lastNameUkrField.setReadOnly(false);
                firstNameUkrField.setReadOnly(false);
                middleNameUkrField.setReadOnly(false);
                lastNameEngField.setReadOnly(false);
                firstNameEngField.setReadOnly(false);
                //Ввімкнення можливості редагування Академічних даних
                groupSelect.setReadOnly(false);
                courseSelect.setReadOnly(false);
                groupNumberField.setReadOnly(false);
                admissionYearSelect.setReadOnly(false);
                recordBookNumberField.setReadOnly(false);
                //Ввімкнення можливості редагування Паспортних даних
                passportSeriesField.setReadOnly(false);
                passportNumberField.setReadOnly(false);
                passportIssueDatePicker.setReadOnly(false);
                passportIssuedByField.setReadOnly(false);
                passportExpiryDatePicker.setReadOnly(false);
                idCodeField.setReadOnly(false);
                unzrField.setReadOnly(false);
                birthDatePicker.setReadOnly(false);
                nationalityField.setReadOnly(false);
                genderSelect.setReadOnly(false);
                personNumberEDEBOField.setReadOnly(false);
                studentCardNumberEDEBOField.setReadOnly(false);
                //Ввімкнення можливості редагування Даних про навчання
                caseNumberField.setReadOnly(false);
                educationFormSelect.setReadOnly(false);
                degreeSelect.setReadOnly(false);
                admissionConditionSelect.setReadOnly(false);
                paymentSourceSelect.setReadOnly(false);
                contractNumberField.setReadOnly(false);
                amountField.setReadOnly(false);
                benefitsSelect.setReadOnly(false);
                //Ввімкнення можливості редагування Контактних даних
                phoneNumberField.setReadOnly(false);
                emailField.setReadOnly(false);
                //Ввімкнення можливості редагування Адреси
                regionSelect.setReadOnly(false);
                indexField.setReadOnly(false);
                fullAddressField.setReadOnly(false);
                //Ввімкнення можливості редагування Попередньої освіти
                documentTypeSelect.setReadOnly(false);
                distinctionCheckbox.setReadOnly(false);
                documentSeriesField.setReadOnly(false);
                documentNumberField.setReadOnly(false);
                documentIssueDatePicker.setReadOnly(false);
                institutionNameField.setReadOnly(false);
                institutionNameEngField.setReadOnly(false);
                //Ввімкнення можливості редагування Диплому
                diplomaSeriesField.setReadOnly(false);
                diplomaNumberField.setReadOnly(false);
                graduationDatePicker.setReadOnly(false);
                appendixNumberField.setReadOnly(false);
                thesisTitleUkrField.setReadOnly(false);
                thesisTitleEngField.setReadOnly(false);
                //Зміна кнопки
                editButton.setText("Зберегти");
                //Встановлення кольору тексту кнопки на зелений
                editButton.getStyle().set("color", "green");

                //Блокування всіх інших кнопок поки активна кнопка "Зберегти"
                selectStudent.setEnabled(false);
                selectGroup.setEnabled(false);
                addCardButton.setEnabled(false);
                sendToArchiveButton.setEnabled(false);

                if (mainLayout != null) {
                    mainLayout.setDrawerEnabled(false);
                }


            } else if (editButton.getText().equals("Зберегти")) {

                //Вимкнення можливості редагування Відомостей
                typeOfInformationSelect.setReadOnly(true);
                datePicker.setReadOnly(true);
                numberField.setReadOnly(true);
                studentOrGroupSelect.setReadOnly(true);
                submitDataButton.setEnabled(false);
//                orderGrid.setEnabled(false);



                //Вимкнення можливості редагування Персональних даних
                lastNameUkrField.setReadOnly(true);
                firstNameUkrField.setReadOnly(true);
                middleNameUkrField.setReadOnly(true);
                lastNameEngField.setReadOnly(true);
                firstNameEngField.setReadOnly(true);
                //Вимкнення можливості редагування Академічних даних
                groupSelect.setReadOnly(true);
                courseSelect.setReadOnly(true);
                groupNumberField.setReadOnly(true);
                admissionYearSelect.setReadOnly(true);
                recordBookNumberField.setReadOnly(true);

                //Порівняння моделей на відповідність
                StudentEntity studentEntityCheck = new StudentEntity(
                        studentEntity.getId(),
                        lastNameUkrField.getValue(),
                        firstNameUkrField.getValue(),
                        middleNameUkrField.getValue(),
                        studentEntity.getFaculty(),
                        groupService.getGroupByTitle
                                (
                                        groupSelect.getValue() + "-" +
                                        courseSelect.getValue() + "-" +
                                        groupNumberField.getValue() + "-" +
                                        admissionYearSelect.getValue()
                                ),
                        studentEntity.getRecordBookNumber()
                );

                StudentPassportEntity passportEntityCheck = new StudentPassportEntity
                        (
                                studentPassportEntity.getId(),
                                studentEntity,
                                firstNameEngField.getValue(),
                                lastNameEngField.getValue(),
                                nationalityField.getValue(),
                                Gender.valueOf(genderSelect.getValue()),
                                Date.valueOf(passportIssueDatePicker.getValue()),
                                passportIssuedByField.getValue(),
                                Date.valueOf(passportExpiryDatePicker.getValue()),
                                passportSeriesField.getValue(),
                                passportNumberField.getValue(),
                                idCodeField.getValue(),
                                unzrField.getValue(),
                                Date.valueOf(birthDatePicker.getValue()),
                                personNumberEDEBOField.getValue(),
                                studentCardNumberEDEBOField.getValue()


                        );


                StudentInfoEntity infoModelCheck = new StudentInfoEntity(
                        studentInfoEntity.getId(),
                        studentEntity,
                        fullAddressField.getValue(),
                        phoneNumberField.getValue(),
                        emailField.getValue(),
                        caseNumberField.getValue(),
                        educationFormSelect.getValue(),
                        degreeSelect.getValue(),
                        admissionConditionSelect.getValue(),
                        paymentSourceSelect.getValue(),
                        contractNumberField.getValue(),
                        amountField.getValue(),
                        String.join(", ", benefitsSelect.getValue()),
                        regionSelect.getValue(),
                        indexField.getValue()

                );

                StudentEducationEntity educationEntityCheck = new StudentEducationEntity
                        (
                                studentEducationEntity.getId(),
                                studentEntity,
                                documentTypeSelect.getValue(),
                                distinctionCheckbox.getValue() ? 1 : 0,
                                documentSeriesField.getValue(),
                                documentNumberField.getValue(),
                                Date.valueOf(documentIssueDatePicker.getValue()),
                                institutionNameField.getValue(),
                                institutionNameEngField.getValue(),

                                diplomaSeriesField.getValue(),
                                diplomaNumberField.getValue(),
                                Date.valueOf(graduationDatePicker.getValue()),
                                appendixNumberField.getValue(),
                                thesisTitleUkrField.getValue(),
                                thesisTitleEngField.getValue()
                        );

                if (
                        studentEntity.equals(studentEntityCheck)
                        && studentPassportEntity.equals(passportEntityCheck)
                        && studentInfoEntity.equals(infoModelCheck)
                        && studentEducationEntity.equals(educationEntityCheck)
                )
                {
                    System.out.println("Дані не були змінені");
                } else {
                    showConfirmationDialog();
                }




                //Вимкнення можливості редагування Паспортних даних
                passportSeriesField.setReadOnly(true);
                passportNumberField.setReadOnly(true);
                passportIssueDatePicker.setReadOnly(true);
                passportIssuedByField.setReadOnly(true);
                passportExpiryDatePicker.setReadOnly(true);
                idCodeField.setReadOnly(true);
                unzrField.setReadOnly(true);
                birthDatePicker.setReadOnly(true);
                nationalityField.setReadOnly(true);
                genderSelect.setReadOnly(true);
                personNumberEDEBOField.setReadOnly(true);
                studentCardNumberEDEBOField.setReadOnly(true);
                //Вимкнення можливості редагування Даних про навчання
                caseNumberField.setReadOnly(true);
                educationFormSelect.setReadOnly(true);
                degreeSelect.setReadOnly(true);
                admissionConditionSelect.setReadOnly(true);
                paymentSourceSelect.setReadOnly(true);
                contractNumberField.setReadOnly(true);
                amountField.setReadOnly(true);
                benefitsSelect.setReadOnly(true);
                //Вимкнення можливості редагування Контактних даних
                phoneNumberField.setReadOnly(true);
                emailField.setReadOnly(true);
                //Вимкнення можливості редагування Адреси
                regionSelect.setReadOnly(true);
                indexField.setReadOnly(true);
                fullAddressField.setReadOnly(true);
                //Вимкнення можливості редагування Попередньої освіти
                documentTypeSelect.setReadOnly(true);
                distinctionCheckbox.setReadOnly(true);
                documentSeriesField.setReadOnly(true);
                documentNumberField.setReadOnly(true);
                documentIssueDatePicker.setReadOnly(true);
                institutionNameField.setReadOnly(true);
                institutionNameEngField.setReadOnly(true);
                //Вимкнення можливості редагування Диплому
                diplomaSeriesField.setReadOnly(true);
                diplomaNumberField.setReadOnly(true);
                graduationDatePicker.setReadOnly(true);
                appendixNumberField.setReadOnly(true);
                thesisTitleUkrField.setReadOnly(true);
                thesisTitleEngField.setReadOnly(true);
                //Розблокування всіх інших кнопок після збереження
                selectStudent.setEnabled(true);
                selectGroup.setEnabled(true);
                addCardButton.setEnabled(true);
                sendToArchiveButton.setEnabled(true);

                if (mainLayout != null) {
                    mainLayout.setDrawerEnabled(true);
                }

                //Зміна кнопки
                editButton.setText("Редагувати");
                //Встановлення кольору тексту кнопки на стандартний
                editButton.getStyle().set("color", "#0056b3");
            }
        });




    }

    private DatePicker.DatePickerI18n setLocal() {
        DatePicker.DatePickerI18n ukrainian = new DatePicker.DatePickerI18n();
        ukrainian.setMonthNames(List.of("Січень", "Лютий", "Березень", "Квітень",
                "Травень", "Червень", "Липень", "Серпень", "Вересень", "Жовтень",
                "Листопад", "Грудень"));
        ukrainian.setWeekdays(List.of("Неділя", "Понеділок", "Вівторок",
                "Середа", "Четвер", "П'ятниця", "Субота"));
        ukrainian.setWeekdaysShort(
                List.of("Нд", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"));
        ukrainian.setToday("Сьогодні");
        ukrainian.setCancel("Скасувати");

        return ukrainian;
    }


    private void showConfirmationDialog() {
        ConfirmDialog dialog = new ConfirmDialog(
                "Підтвердження змін",
                "Ви впевнені, що хочете зберегти зміни?",
                "Так", (event) -> {

            System.out.println("Зміни збережено");
            //Збереження змін StudentModel
            StudentEntity studentEntitySave = new StudentEntity();
            studentEntitySave.setId(studentEntity.getId());
            studentEntitySave.setName(firstNameUkrField.getValue());
            studentEntitySave.setSurname(lastNameUkrField.getValue());
            studentEntitySave.setPatronymic(middleNameUkrField.getValue());
            studentEntitySave.setGroup(groupService.getGroupByTitle
                    (
                            groupSelect.getValue() + "-" +
                            courseSelect.getValue() + "-" +
                            groupNumberField.getValue() + "-" +
                            admissionYearSelect.getValue()
                    )
            );

            studentEntitySave.setFaculty(studentEntity.getFaculty());
            studentEntitySave.setRecordBookNumber(recordBookNumberField.getValue());
            studentService.save(studentEntitySave);

            //Збереження змін PassportEntity
            StudentPassportEntity passportEntitySave = new StudentPassportEntity();
            passportEntitySave.setId(studentPassportEntity.getId());
            passportEntitySave.setSeries(passportSeriesField.getValue());
            passportEntitySave.setNumber(passportNumberField.getValue());
            passportEntitySave.setIssueDate(Date.valueOf(passportIssueDatePicker.getValue()));
            passportEntitySave.setExpireDate(Date.valueOf(passportExpiryDatePicker.getValue()));
            passportEntitySave.setIssuedBy(passportIssuedByField.getValue());
            passportEntitySave.setIdentificationNumber(idCodeField.getValue());
            passportEntitySave.setUnzrCode(unzrField.getValue());
            passportEntitySave.setBirthdate(Date.valueOf(birthDatePicker.getValue()));
            passportEntitySave.setNationality(nationalityField.getValue());
            passportEntitySave.setSex(Gender.valueOf(genderSelect.getValue()));
            passportEntitySave.setEdboNumberPhis(personNumberEDEBOField.getValue());
            passportEntitySave.setEdboNumberZdob(studentCardNumberEDEBOField.getValue());
            passportEntitySave.setNameEng(firstNameEngField.getValue());
            passportEntitySave.setSurnameEng(lastNameEngField.getValue());
            passportEntitySave.setStudent(studentEntitySave);
            studentPassportService.save(passportEntitySave);


            //Збереження змін InfoEntity
            StudentInfoEntity infoEntitySave = new StudentInfoEntity();
            infoEntitySave.setId(studentInfoEntity.getId());
            infoEntitySave.setCaseNumber(caseNumberField.getValue());
            infoEntitySave.setFormStudy(educationFormSelect.getValue());
            infoEntitySave.setDegree(degreeSelect.getValue());
            infoEntitySave.setEntryRequirements(admissionConditionSelect.getValue());
            infoEntitySave.setTypeOfIndividual(paymentSourceSelect.getValue());
            infoEntitySave.setContractNumber(contractNumberField.getValue());
            infoEntitySave.setTotal(amountField.getValue());
            infoEntitySave.setBenefits(String.join(", ", benefitsSelect.getValue()));
            infoEntitySave.setPhone(phoneNumberField.getValue());
            infoEntitySave.setEmail(emailField.getValue());
            infoEntitySave.setRegion(regionSelect.getValue());
            infoEntitySave.setIndex(indexField.getValue());
            infoEntitySave.setAddress(fullAddressField.getValue());
            infoEntitySave.setStudent(studentEntitySave);
            studentInfoService.save(infoEntitySave);

            //Збереження змін EducationEntity
            StudentEducationEntity educationEntitySave = new StudentEducationEntity();
            educationEntitySave.setId(studentEducationEntity.getId());
            educationEntitySave.setTypeOfDocument(documentTypeSelect.getValue());
            educationEntitySave.setHonors(distinctionCheckbox.getValue() ? 1 : 0);
            educationEntitySave.setSeries(documentSeriesField.getValue());
            educationEntitySave.setNumber(documentNumberField.getValue());
            educationEntitySave.setDateOfIssue(Date.valueOf(documentIssueDatePicker.getValue()));
            educationEntitySave.setIssuedBy(institutionNameField.getValue());
            educationEntitySave.setIssuedByEng(institutionNameEngField.getValue());
            educationEntitySave.setDiplomaSeries(diplomaSeriesField.getValue());
            educationEntitySave.setDiplomaNumber(diplomaNumberField.getValue());
            educationEntitySave.setDateOfIssueDiploma(Date.valueOf(graduationDatePicker.getValue()));
            educationEntitySave.setNumberOfDodatok(appendixNumberField.getValue());
            educationEntitySave.setThemeOfWork(thesisTitleUkrField.getValue());
            educationEntitySave.setThemeOfWorkEng(thesisTitleEngField.getValue());
            educationEntitySave.setStudent(studentEntitySave);
            studentEducationService.save(educationEntitySave);

        },
                "Ні", (event) -> {
            System.out.println("Ви відмінили зміни");
            //Відміна змін
            lastNameUkrField.setValue(studentEntity.getSurname());
            firstNameUkrField.setValue(studentEntity.getName());
            middleNameUkrField.setValue(studentEntity.getPatronymic());
            firstNameEngField.setValue(studentPassportEntity.getNameEng());
            lastNameEngField.setValue(studentPassportEntity.getSurnameEng());
            groupSelect.setValue(studentEntity.getGroup().getGroupCode().split("-")[0]);
            courseSelect.setValue(studentEntity.getGroup().getGroupCode().split("-")[1]);
            groupNumberField.setValue(studentEntity.getGroup().getGroupCode().split("-")[2]);
            admissionYearSelect.setValue(studentEntity.getGroup().getGroupCode().split("-")[3]);
            recordBookNumberField.setValue(studentEntity.getRecordBookNumber());
            passportSeriesField.setValue(studentPassportEntity.getSeries());
            passportNumberField.setValue(studentPassportEntity.getNumber());
            passportIssueDatePicker.setValue(studentPassportEntity.getIssueDate().toLocalDate());
            passportIssuedByField.setValue(studentPassportEntity.getIssuedBy());
            passportExpiryDatePicker.setValue(studentPassportEntity.getExpireDate().toLocalDate());
            idCodeField.setValue(studentPassportEntity.getIdentificationNumber());
            unzrField.setValue(studentPassportEntity.getUnzrCode());
            birthDatePicker.setValue(studentPassportEntity.getBirthdate().toLocalDate());
            nationalityField.setValue(studentPassportEntity.getNationality());
            genderSelect.setValue(studentPassportEntity.getSex().name());
            personNumberEDEBOField.setValue(studentPassportEntity.getEdboNumberPhis());
            studentCardNumberEDEBOField.setValue(studentPassportEntity.getEdboNumberZdob());
            caseNumberField.setValue(studentInfoEntity.getCaseNumber());
            educationFormSelect.setValue(studentInfoEntity.getFormStudy());
            degreeSelect.setValue(studentInfoEntity.getDegree());
            admissionConditionSelect.setValue(studentInfoEntity.getEntryRequirements());
            paymentSourceSelect.setValue(studentInfoEntity.getTypeOfIndividual());
            contractNumberField.setValue(studentInfoEntity.getContractNumber());
            amountField.setValue(studentInfoEntity.getTotal());
            benefitsSelect.setValue(Arrays.asList(studentInfoEntity.getBenefits().split(", ")));
            phoneNumberField.setValue(studentInfoEntity.getPhone());
            emailField.setValue(studentInfoEntity.getEmail());
            regionSelect.setValue(studentInfoEntity.getRegion());
            indexField.setValue(studentInfoEntity.getIndex());
            fullAddressField.setValue(studentInfoEntity.getAddress());
            documentTypeSelect.setValue(studentEducationEntity.getTypeOfDocument());
            distinctionCheckbox.setValue(studentEducationEntity.getHonors() == 1);
            documentSeriesField.setValue(studentEducationEntity.getSeries());
            documentNumberField.setValue(studentEducationEntity.getNumber());
            documentIssueDatePicker.setValue(studentEducationEntity.getDateOfIssue().toLocalDate());
            institutionNameField.setValue(studentEducationEntity.getIssuedBy());
            institutionNameEngField.setValue(studentEducationEntity.getIssuedByEng());
            diplomaSeriesField.setValue(studentEducationEntity.getDiplomaSeries());
            diplomaNumberField.setValue(studentEducationEntity.getDiplomaNumber());
            graduationDatePicker.setValue(studentEducationEntity.getDateOfIssueDiploma().toLocalDate());
            appendixNumberField.setValue(studentEducationEntity.getNumberOfDodatok());
            thesisTitleUkrField.setValue(studentEducationEntity.getThemeOfWork());
            thesisTitleEngField.setValue(studentEducationEntity.getThemeOfWorkEng());

        });
        dialog.open();
    }
}
