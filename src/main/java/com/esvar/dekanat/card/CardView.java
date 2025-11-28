package com.esvar.dekanat.card;

// Responsibility groups observed in this view:
// - UI rendering/layout: building tabs, form layouts, and styling for personal, academic, passport, and education sections.
// - Event handling: reacting to selector changes, button clicks, and enabling/disabling form fields.
// - Data coordination: loading student/group data from services, validating selections, and persisting academic changes.
// - Group code management: constructing and interpreting group codes from prefixes, courses, numbers, and graduation years.
// - PDF export: generating printable group student lists.
// - Helper utilities: repetitive field value setters and parsing helpers.

/*
Proposed target structure after refactoring:
- CardView: orchestration layer that wires UI sections, delegates to helpers, and remains the Vaadin route.
- CardFieldUtil: shared helper for safely updating field values and parsing user input (extracted).
- GroupCodeBuilder: encapsulates group code construction logic (extracted).
- StudentListPdfGenerator: encapsulates PDF creation and streaming for student lists (extracted).
Future candidates:
- Dedicated UI section components (personal data, academic data, passport data) to reduce constructor size. // TODO: decouple further
*/


import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.repository.StudentRatingRepository;
import com.esvar.dekanat.service.*;
import com.esvar.dekanat.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
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
import java.text.Collator;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.esvar.dekanat.card.CardFieldUtil.setBenefitsValue;
import static com.esvar.dekanat.card.CardFieldUtil.setDatePickerValue;
import static com.esvar.dekanat.card.CardFieldUtil.setGenderValue;
import static com.esvar.dekanat.card.CardFieldUtil.setSelectValue;
import static com.esvar.dekanat.card.CardFieldUtil.setTextFieldValue;


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
    private final StudentRatingRepository ratingRepository;
    private final GroupCodeBuilder groupCodeBuilder;


    private VerticalLayout mainLayout = new VerticalLayout();
    private HorizontalLayout selectors = new HorizontalLayout();
    private Select<String> selectStudent = new Select<>();
    private ComboBox<String> selectGroup = new ComboBox<>();
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
    private TextField groupNumberField = new TextField();
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
    private Button groupList = new Button("Список групи");


    private StudentEntity studentEntity;
    private StudentPassportEntity studentPassportEntity;
    private StudentInfoEntity studentInfoEntity;
    private StudentEducationEntity studentEducationEntity;

    private final SpecialtyService specialtyService;
    private final Collator ukrainianCollator;
    private Long pendingCreatedGroupId;


    public CardView(GroupService groupService, StudentService studentService, StudentPassportService studentPassportService, StudentInfoService studentInfoService, StudentEducationService studentEducationService, StudentReportService studentReportService, ReportService reportService, StudentRatingRepository ratingRepository, SpecialtyService specialtyService) {
        this.groupService = groupService;
        this.studentService = studentService;
        this.studentPassportService = studentPassportService;
        this.studentInfoService = studentInfoService;
        this.studentEducationService = studentEducationService;
        this.studentReportService = studentReportService;
        this.reportService = reportService;
        this.ratingRepository = ratingRepository;
        this.specialtyService = specialtyService;
        this.groupCodeBuilder = new GroupCodeBuilder(specialtyService);
        this.ukrainianCollator = Collator.getInstance(new Locale("uk", "UA"));
        this.pendingCreatedGroupId = null;


        // Setup selectors
        selectStudent.setReadOnly(true);
        selectStudent.setLabel("Студент");
        selectStudent.setPlaceholder("Оберіть студента");
        selectStudent.setWidth("300px");
        selectStudent.getStyle().set("padding", "0");


        selectGroup.setLabel("Група");
        selectGroup.setItems(
                groupService.getGroupsDTO().stream()
                        .map(GroupDTO::toString)
                        .sorted(ukrainianCollator)
                        .collect(Collectors.toList())
        );

        selectGroup.setPlaceholder("Оберіть групу");
        selectGroup.setWidth("300px");
        selectGroup.getStyle().set("padding", "0");

        groupList.setEnabled(false);
        selectGroup.addValueChangeListener(event -> {
            String selectedGroup = event.getValue();

            if (selectedGroup == null) {
                selectStudent.clear();
                selectStudent.setItems(Collections.emptyList());
                selectStudent.setReadOnly(true);
                groupList.setEnabled(false);
                return;
            }

            List<String> students = fetchStudentNames(selectedGroup);
            if (students.isEmpty()) {
                selectStudent.clear();
                selectStudent.setItems(Collections.emptyList());
                selectStudent.setReadOnly(true);
                groupList.setEnabled(false);
                Notification.show("У вибраній групі немає студентів.");
                return;
            }

            selectStudent.setItems(students);
            selectStudent.clear();
            selectStudent.setReadOnly(false);
            groupList.setEnabled(true);
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
        buttonLayout.add(selectGroup, selectStudent, addCardButton, sendToArchiveButton, editButton, groupList);
        buttonLayout.setWidth("100%");
        buttonLayout.setSpacing(true);
        buttonLayout.getStyle().set("padding", "0");
        buttonLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        addCardButton.addClickListener(event -> {
            AddStudentDialog dialog = new AddStudentDialog(
                    groupService,
                    studentService,
                    studentPassportService,
                    studentInfoService,
                    studentEducationService,
                    ratingRepository,
                    reportService
            );
            dialog.addDialogCloseActionListener(e -> {
                String selectedGroup = selectGroup.getValue();
                if (selectedGroup != null) {
                    List<String> students = fetchStudentNames(selectedGroup);
                    selectStudent.setItems(students);
                    selectStudent.setReadOnly(students.isEmpty());
                    groupList.setEnabled(!students.isEmpty());
                }
            });
            dialog.open();
        });


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
        orderLeftTitle.getStyle().set("z-index", "1");

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
        orderLayout.add(orderGridWrapper, InningLayoutWrapper);
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
        groupSelect.setItems(
                groupService.getGroupsDTO().stream()
                        .map(GroupDTO::getGroupCode)
                        .map(code -> code.split("-")[0])
                        .distinct()
                        .sorted(ukrainianCollator)
                        .collect(Collectors.toList())
        );

        courseSelect = new Select<>();
        courseSelect.setLabel("Курс");
        courseSelect.setWidth("24%");
        courseSelect.setItems("1", "2", "3", "4");

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



        groupList.addClickListener(event -> {
            String selectedGroup = selectGroup.getValue();
            if (selectedGroup == null) {
                Notification.show("Оберіть групу для генерації списку.");
                return;
            }

            List<String> students = fetchStudentNames(selectedGroup);
            if (students.isEmpty()) {
                Notification.show("У вибраній групі немає студентів для генерації списку.");
                groupList.setEnabled(false);
                return;
            }

            StudentListPdfGenerator.generateAndSend(selectedGroup, students);
        });



        admissionYearSelect = new Select<>();
        admissionYearSelect.setLabel("Рік випуску");
        admissionYearSelect.setWidth("24%");
        refreshGraduationYearOptions();


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

        CardMainInfoView mainInfoView = new CardMainInfoView(lastNameUkrField, firstNameUkrField, middleNameUkrField, lastNameEngField, firstNameEngField, groupSelect, courseSelect, groupNumberField, admissionYearSelect, recordBookNumberField);

// Additional info text fields
        caseNumberField = new TextField("Номер справи");
        caseNumberField.setPattern("[0-9]{1,}");
        caseNumberField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value.matches("[0-9]+")) {
                caseNumberField.setErrorMessage(null);
            } else {
                caseNumberField.setErrorMessage("Введіть тільки цифри");
                Notification.show("Неправильний ввід. Введіть тільки цифри.");
            }
        });
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
        phoneNumberField.setPattern("[0-9]{1,}");
        phoneNumberField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value.matches("[0-9]+")) {
                phoneNumberField.setErrorMessage(null);
            } else {
                phoneNumberField.setErrorMessage("Введіть тільки цифри");
                Notification.show("Неправильний ввід. Введіть тільки цифри.");
            }
        });
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



        CardAdditionalInfoView additionalInfoView = new CardAdditionalInfoView(caseNumberField, educationFormSelect, degreeSelect, admissionConditionSelect, paymentSourceSelect, contractNumberField, amountField, benefitsSelect, regionSelect, indexField, fullAddressField, phoneNumberField, emailField);
        CardPassportInfoView passportInfoView = new CardPassportInfoView(passportSeriesField, passportNumberField, passportIssueDatePicker, passportExpiryDatePicker, passportIssuedByField, idCodeField, unzrField, birthDatePicker, nationalityField, genderSelect, personNumberEDEBOField, studentCardNumberEDEBOField);
        CardEducationDocumentsView educationDocumentsView = new CardEducationDocumentsView(documentTypeSelect, distinctionCheckbox, documentSeriesField, documentNumberField, documentIssueDatePicker, institutionNameField, institutionNameEngField, diplomaSeriesField, diplomaNumberField, graduationDatePicker, appendixNumberField, thesisTitleUkrField, thesisTitleEngField);

        tabs.addSelectedChangeListener(event -> {
            mainLayout.removeAll();
            if (tabs.getSelectedTab().equals(mainInfoTab)) {
                mainLayout.add(buttonLayout, tabs, mainInfoView, orderLayout);
            } else if (tabs.getSelectedTab().equals(additionalInfoTab)) {
                mainLayout.add(buttonLayout, tabs, additionalInfoView);
            } else if (tabs.getSelectedTab().equals(passportInfoTab)) {
                mainLayout.add(buttonLayout, tabs, passportInfoView);
            } else if (tabs.getSelectedTab().equals(educationDocumentsTab)) {
                mainLayout.add(buttonLayout, tabs, educationDocumentsView.getGeneralEducationDocumentsWrapper(), educationDocumentsView.getDiplomaDocumentsWrapper());
            }
        });

        mainLayout.add(buttonLayout, tabs, mainInfoView, orderLayout);
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
//                studentPassportEntity = studentPassportService.getPassportByStudentModel(studentEntity);
//                studentInfoEntity = studentInfoService.getInfoByStudentModel(studentEntity);
//                studentEducationEntity = studentEducationService.getEducationByStudentModel(studentEntity);

                //Персональні дані
                setTextFieldValue(lastNameUkrField, studentEntity.getSurname());
                lastNameUkrField.setReadOnly(true);

                setTextFieldValue(firstNameUkrField, studentEntity.getName());
                firstNameUkrField.setReadOnly(true);

                setTextFieldValue(middleNameUkrField, studentEntity.getPatronymic());
                middleNameUkrField.setReadOnly(true);

//                setTextFieldValue(firstNameEngField, studentPassportEntity.getNameEng());
//                firstNameEngField.setReadOnly(true);
//
//                setTextFieldValue(lastNameEngField, studentPassportEntity.getSurnameEng());
//                lastNameEngField.setReadOnly(true);

//                //Академічні дані
                String groupCode = studentEntity.getGroup() != null ? studentEntity.getGroup().getGroupCode() : null;
                String[] groupParts = groupCode != null ? groupCode.split("-") : new String[0];
                setSelectValue(groupSelect, getGroupPart(groupParts, 0));
                groupSelect.setReadOnly(true);

                setSelectValue(courseSelect, getGroupPart(groupParts, 1));
                courseSelect.setReadOnly(true);

                setTextFieldValue(groupNumberField, getGroupPart(groupParts, 2));
                groupNumberField.setReadOnly(true);

                setSelectValue(admissionYearSelect, getGroupPart(groupParts, 3));
                admissionYearSelect.setReadOnly(true);

                setTextFieldValue(recordBookNumberField, studentEntity.getRecordBookNumber());
                recordBookNumberField.setReadOnly(true);
//
////                //Відомості
//                orderGrid.setItems(studentReportService.getReportsByStudentId(studentEntity.getId()));
//
//
////                //Паспортні дані
//                setTextFieldValue(passportSeriesField, studentPassportEntity.getSeries());
//                passportSeriesField.setReadOnly(true);
//
//                setTextFieldValue(passportNumberField, studentPassportEntity.getNumber());
//                passportNumberField.setReadOnly(true);
//
//                setDatePickerValue(passportIssueDatePicker, studentPassportEntity.getIssueDate());
//                passportIssueDatePicker.setReadOnly(true);
//
//                setTextFieldValue(passportIssuedByField, studentPassportEntity.getIssuedBy());
//                passportIssuedByField.setReadOnly(true);
//
//                setDatePickerValue(passportExpiryDatePicker, studentPassportEntity.getExpireDate());
//                passportExpiryDatePicker.setReadOnly(true);
//
//                setTextFieldValue(idCodeField, studentPassportEntity.getIdentificationNumber());
//                idCodeField.setReadOnly(true);
//
//                setTextFieldValue(unzrField, studentPassportEntity.getUnzrCode());
//                unzrField.setReadOnly(true);
//
//                setDatePickerValue(birthDatePicker, studentPassportEntity.getBirthdate());
//                birthDatePicker.setReadOnly(true);
//
//                setSelectValue(nationalityField, studentPassportEntity.getNationality());
//                nationalityField.setReadOnly(true);
//
//                setGenderValue(studentPassportEntity.getSex());
//                genderSelect.setReadOnly(true);
//
//                setTextFieldValue(personNumberEDEBOField, studentPassportEntity.getEdboNumberPhis());
//                personNumberEDEBOField.setReadOnly(true);
//
//                setTextFieldValue(studentCardNumberEDEBOField, studentPassportEntity.getEdboNumberZdob());
//                studentCardNumberEDEBOField.setReadOnly(true);
////
////                //Дані про наввчання
//                setTextFieldValue(caseNumberField, studentInfoEntity.getCaseNumber());
//                caseNumberField.setReadOnly(true);
//
//                setSelectValue(educationFormSelect, studentInfoEntity.getFormStudy());
//                educationFormSelect.setReadOnly(true);
//
//                setSelectValue(degreeSelect, studentInfoEntity.getDegree());
//                degreeSelect.setReadOnly(true);
//
//                setSelectValue(admissionConditionSelect, studentInfoEntity.getEntryRequirements());
//                admissionConditionSelect.setReadOnly(true);
//
//                setSelectValue(paymentSourceSelect, studentInfoEntity.getTypeOfIndividual());
//                paymentSourceSelect.setReadOnly(true);
//
//                setTextFieldValue(contractNumberField, studentInfoEntity.getContractNumber());
//                contractNumberField.setReadOnly(true);
//
//                setTextFieldValue(amountField, studentInfoEntity.getTotal());
//                amountField.setReadOnly(true);
//
//                setBenefitsValue(studentInfoEntity.getBenefits());
//                benefitsSelect.setReadOnly(true);
//
//                //Контактні дані
//                setTextFieldValue(phoneNumberField, studentInfoEntity.getPhone());
//                phoneNumberField.setReadOnly(true);
//
//                setTextFieldValue(emailField, studentInfoEntity.getEmail());
//                emailField.setReadOnly(true);
//
//                //Адреса
//                setSelectValue(regionSelect, studentInfoEntity.getRegion());
//                regionSelect.setReadOnly(true);
//
//                setTextFieldValue(indexField, studentInfoEntity.getIndex());
//                indexField.setReadOnly(true);
//
//                setTextFieldValue(fullAddressField, studentInfoEntity.getAddress());
//                fullAddressField.setReadOnly(true);
//
//                //Попередня освіта
//                setSelectValue(documentTypeSelect, studentEducationEntity.getTypeOfDocument());
//                documentTypeSelect.setReadOnly(true);
//
//                distinctionCheckbox.setValue(studentEducationEntity.getHonors() == 1);
//                distinctionCheckbox.setReadOnly(true);
//
//                setTextFieldValue(documentSeriesField, studentEducationEntity.getSeries());
//                documentSeriesField.setReadOnly(true);
//
//                setTextFieldValue(documentNumberField, studentEducationEntity.getNumber());
//                documentNumberField.setReadOnly(true);
//
//                setDatePickerValue(documentIssueDatePicker, studentEducationEntity.getDateOfIssue());
//                documentIssueDatePicker.setReadOnly(true);
//
//                setTextFieldValue(institutionNameField, studentEducationEntity.getIssuedBy());
//                institutionNameField.setReadOnly(true);
//
//                setTextFieldValue(institutionNameEngField, studentEducationEntity.getIssuedByEng());
//                institutionNameEngField.setReadOnly(true);
//
//                //Диплом
//                setTextFieldValue(diplomaSeriesField, studentEducationEntity.getDiplomaSeries());
//                diplomaSeriesField.setReadOnly(true);
//
//                setTextFieldValue(diplomaNumberField, studentEducationEntity.getDiplomaNumber());
//                diplomaNumberField.setReadOnly(true);
//
//                setDatePickerValue(graduationDatePicker, studentEducationEntity.getDateOfIssueDiploma());
//                graduationDatePicker.setReadOnly(true);
//
//                setTextFieldValue(appendixNumberField, studentEducationEntity.getNumberOfDodatok());
//                appendixNumberField.setReadOnly(true);
//
//                setTextFieldValue(thesisTitleUkrField, studentEducationEntity.getThemeOfWork());
//                thesisTitleUkrField.setReadOnly(true);
//
//                setTextFieldValue(thesisTitleEngField, studentEducationEntity.getThemeOfWorkEng());
//                thesisTitleEngField.setReadOnly(true);
            }
        });

        //Обробка додавання нової відомості
        submitDataButton.addClickListener(buttonClickEvent -> {

            StudentEntity studentEntityMain = studentService.getStudentForCard(selectGroup.getValue(), selectStudent.getValue());

            if (studentOrGroupSelect.getValue().equals("Один студент")) {

                ReportEntity reportEntity = new ReportEntity();
                reportEntity.setStudent(studentEntityMain);
                reportEntity.setStatus(typeOfInformationSelect.getValue());
                reportEntity.setDate(Date.valueOf(datePicker.getValue()));
                reportEntity.setOrderNumber(Long.valueOf(numberField.getValue()));

                reportService.saveReport(reportEntity);

            } else if (studentOrGroupSelect.getValue().equals("Вся група")) {

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
            MainLayout mainLayout = findMainLayout();
            if (editButton.getText().equals("Редагувати")) {
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


                //Порівняння моделей на відповідність


                StudentGroupEntity selectedGroupEntity = resolveSelectedGroup();
                if (selectedGroupEntity == null) {
                    if (isGroupSelectionComplete()) {
                        Optional<StudentGroupEntity> legacyCandidate = findLegacyGroupCandidate();
                        if (legacyCandidate.isPresent()) {
                            promptLegacyGroupDecision(legacyCandidate.get());
                        } else {
                            promptGroupCreation();
                        }
                    } else {
                        Notification.show("Заповніть курс, номер групи та рік випуску.");
                    }
                    return;
                }

                pendingCreatedGroupId = null;
                processSave(selectedGroupEntity);
            }
        });
    }


    private StudentGroupEntity resolveSelectedGroup() {
        if (!isGroupSelectionComplete()) {
            return null;
        }

        String groupPrefix = groupSelect.getValue();
        String course = courseSelect.getValue();
        String groupNumber = groupNumberField.getValue();
        String graduationYear = admissionYearSelect.getValue();

        SpecialtyEntity specialty = specialtyService.getSpecialtyByAbbreviation(groupPrefix);

        String eduProgramCode = buildGroupCodeWithEduProgram(groupPrefix, course, groupNumber, graduationYear, specialty);
        if (eduProgramCode != null) {
            StudentGroupEntity group = groupService.getGroupByTitle(eduProgramCode);
            if (group != null) {
                return group;
            }
        }

        String specialtyCode = buildGroupCodeWithSpecialtySuffix(groupPrefix, course, groupNumber, graduationYear, specialty);
        if (specialtyCode != null) {
            StudentGroupEntity group = groupService.getGroupByTitle(specialtyCode);
            if (group != null) {
                return group;
            }
        }

        String legacyGroupCode = buildLegacyGroupCode(groupPrefix, course, groupNumber, graduationYear);
        StudentGroupEntity legacyGroup = groupService.getGroupByTitle(legacyGroupCode);
        if (legacyGroup != null && isCurrentStudentGroup(legacyGroup)) {
            return legacyGroup;
        }

        return null;
    }

    private Optional<StudentGroupEntity> findLegacyGroupCandidate() {
        if (!isGroupSelectionComplete()) {
            return Optional.empty();
        }

        String legacyGroupCode = buildLegacyGroupCode(
                groupSelect.getValue(),
                courseSelect.getValue(),
                groupNumberField.getValue(),
                admissionYearSelect.getValue()
        );

        StudentGroupEntity legacyGroup = groupService.getGroupByTitle(legacyGroupCode);
        if (legacyGroup != null && !isCurrentStudentGroup(legacyGroup)) {
            return Optional.of(legacyGroup);
        }
        return Optional.empty();
    }

    private boolean isCurrentStudentGroup(StudentGroupEntity group) {
        if (studentEntity == null || studentEntity.getGroup() == null) {
            return false;
        }
        return Objects.equals(studentEntity.getGroup().getId(), group.getId());
    }

    private void promptLegacyGroupDecision(StudentGroupEntity legacyGroup) {
        String legacyGroupCode = legacyGroup.getGroupCode();
        ConfirmDialog dialog = new ConfirmDialog(
                "Групу знайдено",
                "Група " + legacyGroupCode + " вже існує у старому форматі. Перенести студента до неї?",
                "Перенести",
                event -> {
                    pendingCreatedGroupId = null;
                    processSave(legacyGroup);
                },
                "Створити нову",
                cancelEvent -> promptGroupCreation()
        );
        dialog.setCancelable(true);
        dialog.open();
    }

    private boolean isGroupSelectionComplete() {
        String groupPrefix = groupSelect.getValue();
        String course = courseSelect.getValue();
        String groupNumber = groupNumberField.getValue();
        String graduationYear = admissionYearSelect.getValue();

        return groupPrefix != null && !groupPrefix.isBlank()
                && course != null && !course.isBlank()
                && groupNumber != null && !groupNumber.isBlank()
                && graduationYear != null && !graduationYear.isBlank();
    }

    private String buildGroupCode(String groupPrefix, String course, String groupNumber, String graduationYear) {
        return groupCodeBuilder.buildGroupCode(groupPrefix, course, groupNumber, graduationYear);
    }

    private String buildGroupCode(String groupPrefix, String course, String groupNumber, String graduationYear, SpecialtyEntity specialty) {
        return groupCodeBuilder.buildGroupCode(groupPrefix, course, groupNumber, graduationYear, specialty);
    }

    private String buildGroupCodeWithEduProgram(String groupPrefix, String course, String groupNumber, String graduationYear, SpecialtyEntity specialty) {
        return groupCodeBuilder.buildGroupCodeWithEduProgram(groupPrefix, course, groupNumber, graduationYear, specialty);
    }

    private String buildGroupCodeWithSpecialtySuffix(String groupPrefix, String course, String groupNumber, String graduationYear, SpecialtyEntity specialty) {
        return groupCodeBuilder.buildGroupCodeWithSpecialtySuffix(groupPrefix, course, groupNumber, graduationYear, specialty);
    }

    private String buildLegacyGroupCode(String groupPrefix, String course, String groupNumber, String graduationYear) {
        return groupCodeBuilder.buildLegacyGroupCode(groupPrefix, course, groupNumber, graduationYear);
    }

    private void promptGroupCreation() {
        String groupPrefix = groupSelect.getValue();
        String course = courseSelect.getValue();
        String groupNumber = groupNumberField.getValue();
        String graduationYear = admissionYearSelect.getValue();

        String groupCode = buildGroupCode(groupPrefix, course, groupNumber, graduationYear);

        ConfirmDialog dialog = new ConfirmDialog(
                "Групу не знайдено",
                "Групу " + groupCode + " не знайдено. Створити нову?",
                "Створити", event -> {
            StudentGroupEntity createdGroup = createGroupForSelection(groupPrefix, course, groupNumber, graduationYear, selectGroup.getValue());
            if (createdGroup != null) {
                pendingCreatedGroupId = createdGroup.getId();
                refreshGraduationYearOptions();
                updateGroupSelectorsItems();
                processSave(createdGroup);
            }
        },
                "Скасувати", cancelEvent -> Notification.show("Створення групи скасовано."));
        dialog.open();
    }

    private void processSave(StudentGroupEntity selectedGroupEntity) {
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

        boolean academicDataChanged = hasAcademicDataChanges(selectedGroupEntity);
        if (academicDataChanged) {
            applyAcademicDataChanges(selectedGroupEntity);
        } else {
            Notification.show("Зміни академічних даних відсутні.");
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

        MainLayout mainLayout = findMainLayout();
        if (mainLayout != null) {
            mainLayout.setDrawerEnabled(true);
        }

        //Зміна кнопки
        editButton.setText("Редагувати");
        //Встановлення кольору тексту кнопки на стандартний
        editButton.getStyle().set("color", "#0056b3");
    }

    private boolean hasAcademicDataChanges(StudentGroupEntity selectedGroupEntity) {
        if (selectedGroupEntity == null) {
            return false;
        }
        if (studentEntity == null) {
            return true;
        }
        StudentGroupEntity currentGroup = studentEntity.getGroup();
        if (currentGroup == null) {
            return true;
        }

        SpecialtyEntity currentSpecialty = currentGroup.getSpecialty();
        SpecialtyEntity targetSpecialty = selectedGroupEntity.getSpecialty();
        String currentAbbreviation = currentSpecialty != null ? currentSpecialty.getAbbreviation() : null;
        String targetAbbreviation = targetSpecialty != null ? targetSpecialty.getAbbreviation() : null;

        if (!Objects.equals(currentAbbreviation, targetAbbreviation)) {
            return true;
        }
        if (currentGroup.getCourse() != selectedGroupEntity.getCourse()) {
            return true;
        }
        if (currentGroup.getGroupNumber() != selectedGroupEntity.getGroupNumber()) {
            return true;
        }
        return normalizeYearValue(currentGroup.getYear()) != normalizeYearValue(selectedGroupEntity.getYear());
    }

    private void applyAcademicDataChanges(StudentGroupEntity selectedGroupEntity) {
        studentEntity.setGroup(selectedGroupEntity);
        studentEntity.setFaculty(selectedGroupEntity.getSpecialty().getFaculty());
        studentService.save(studentEntity);

        ratingRepository.findById(studentEntity.getId()).ifPresent(ratingEntity -> {
            ratingEntity.setStudent(studentEntity);
            ratingEntity.setFaculty(selectedGroupEntity.getSpecialty().getFaculty());
            ratingEntity.setSpecialty(selectedGroupEntity.getSpecialty());
            ratingEntity.setCourse(selectedGroupEntity.getCourse());
            ratingEntity.setGroup(selectedGroupEntity);
            ratingRepository.save(ratingEntity);
        });

        selectGroup.setValue(selectedGroupEntity.getGroupCode());
        setSelectValue(groupSelect, selectedGroupEntity.getSpecialty().getAbbreviation());
        setSelectValue(courseSelect, String.valueOf(selectedGroupEntity.getCourse()));
        setTextFieldValue(groupNumberField, String.valueOf(selectedGroupEntity.getGroupNumber()));
        setSelectValue(admissionYearSelect, formatGraduationYearValue(selectedGroupEntity.getYear()));

        updateGroupSelectorsItems();
        refreshGraduationYearOptions();
        pendingCreatedGroupId = null;

        Notification.show("Академічні дані оновлено.");
    }

    private String formatGraduationYearValue(int year) {
        String yearValue = String.valueOf(year);
        if (yearValue.length() > 2) {
            yearValue = yearValue.substring(yearValue.length() - 2);
        }
        return yearValue;
    }

    private int normalizeYearValue(int year) {
        String formatted = formatGraduationYearValue(year);
        try {
            return Integer.parseInt(formatted);
        } catch (NumberFormatException ignored) {
            return year;
        }
    }

    private StudentGroupEntity createGroupForSelection(String groupPrefix, String course, String groupNumber, String graduationYear, String group) {

        StudentGroupEntity oldGroup = groupService.getGroupByTitle(group);
        SpecialtyEntity targetSpecialty = specialtyService.getSpecialtyByAbbreviation(groupPrefix);
        if (targetSpecialty == null && oldGroup != null) {
            targetSpecialty = oldGroup.getSpecialty();
        }

        String groupCode = buildGroupCode(groupPrefix, course, groupNumber, graduationYear, targetSpecialty);

        StudentGroupEntity newGroup = new StudentGroupEntity();
        newGroup.setSpecialty(targetSpecialty);
        newGroup.setCourse(Integer.parseInt(course));
        newGroup.setGroupNumber(Integer.parseInt(groupNumber));
        newGroup.setYear(Integer.parseInt(graduationYear));
        newGroup.setGroupCode(groupCode);

        try {
            StudentGroupEntity savedGroup = groupService.save(newGroup);
            Notification.show("Групу " + savedGroup.getGroupCode() + " створено.");
            return savedGroup;
        } catch (Exception exception) {
            Notification.show("Не вдалося створити групу " + groupCode + ".");
            return null;
        }
    }

    private void updateGroupSelectorsItems() {
        List<GroupDTO> groups = groupService.getGroupsDTO();

        List<String> groupCodes = groups.stream()
                .map(GroupDTO::toString)
                .sorted(ukrainianCollator)
                .collect(Collectors.toList());
        String currentGroupValue = selectGroup.getValue();
        selectGroup.setItems(groupCodes);
        if (currentGroupValue != null && groupCodes.contains(currentGroupValue)) {
            selectGroup.setValue(currentGroupValue);
        }

        List<String> groupPrefixes = groups.stream()
                .map(GroupDTO::getGroupCode)
                .map(code -> code.split("-")[0])
                .distinct()
                .sorted(ukrainianCollator)
                .collect(Collectors.toList());
        String currentPrefixValue = groupSelect.getValue();
        groupSelect.setItems(groupPrefixes);
        if (currentPrefixValue != null && groupPrefixes.contains(currentPrefixValue)) {
            groupSelect.setValue(currentPrefixValue);
        }
    }

    private void refreshGraduationYearOptions() {
        String currentValue = admissionYearSelect.getValue();
        List<String> years = buildGraduationYearOptions();
        admissionYearSelect.setItems(years);
        if (currentValue != null && years.contains(currentValue)) {
            admissionYearSelect.setValue(currentValue);
        }
    }

    private List<String> buildGraduationYearOptions() {
        int currentYear = Year.now().getValue();
        TreeSet<Integer> years = new TreeSet<>();
        IntStream.rangeClosed(currentYear, currentYear + 6).forEach(years::add);
        groupService.getGroupsDTO().stream()
                .map(GroupDTO::getYear)
                .forEach(years::add);
        return years.stream()
                .map(String::valueOf)
                .map(s -> s.length() > 2 ? s.substring(s.length() - 2) : s)
                .collect(Collectors.toList());

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


    private void showConfirmationDialog(StudentGroupEntity selectedGroupEntity) {
        ConfirmDialog dialog = new ConfirmDialog(
                "Підтвердження змін",
                "Ви впевнені, що хочете зберегти зміни?",
                "Так", (event) -> {


            //Збереження змін StudentModel
            StudentEntity studentEntitySave = new StudentEntity();
            studentEntitySave.setId(studentEntity.getId());
            studentEntitySave.setName(resolveTextFieldValue(firstNameUkrField, studentEntity.getName()));
            studentEntitySave.setSurname(resolveTextFieldValue(lastNameUkrField, studentEntity.getSurname()));
            studentEntitySave.setPatronymic(resolveTextFieldValue(middleNameUkrField, studentEntity.getPatronymic()));
            studentEntitySave.setGroup(selectedGroupEntity);

            studentEntitySave.setFaculty(selectedGroupEntity.getSpecialty().getFaculty());
            studentEntitySave.setRecordBookNumber(resolveTextFieldValue(recordBookNumberField, studentEntity.getRecordBookNumber()));
            studentService.save(studentEntitySave);

            ratingRepository.findById(studentEntitySave.getId()).ifPresent(ratingEntity -> {
                ratingEntity.setStudent(studentEntitySave);
                ratingEntity.setFaculty(selectedGroupEntity.getSpecialty().getFaculty());
                ratingEntity.setSpecialty(selectedGroupEntity.getSpecialty());
                ratingEntity.setCourse(selectedGroupEntity.getCourse());
                ratingEntity.setGroup(selectedGroupEntity);
                ratingRepository.save(ratingEntity);
            });

            //Збереження змін PassportEntity
//            StudentPassportEntity passportEntitySave = new StudentPassportEntity();
//            passportEntitySave.setId(studentPassportEntity.getId());
//            passportEntitySave.setSeries(resolveTextFieldValue(passportSeriesField, studentPassportEntity.getSeries()));
//            passportEntitySave.setNumber(resolveTextFieldValue(passportNumberField, studentPassportEntity.getNumber()));
//            passportEntitySave.setIssueDate(resolveDatePickerValue(passportIssueDatePicker, studentPassportEntity.getIssueDate()));
//            passportEntitySave.setExpireDate(resolveDatePickerValue(passportExpiryDatePicker, studentPassportEntity.getExpireDate()));
//            passportEntitySave.setIssuedBy(resolveTextFieldValue(passportIssuedByField, studentPassportEntity.getIssuedBy()));
//            passportEntitySave.setIdentificationNumber(resolveTextFieldValue(idCodeField, studentPassportEntity.getIdentificationNumber()));
//            passportEntitySave.setUnzrCode(resolveTextFieldValue(unzrField, studentPassportEntity.getUnzrCode()));
//            passportEntitySave.setBirthdate(resolveDatePickerValue(birthDatePicker, studentPassportEntity.getBirthdate()));
//            passportEntitySave.setNationality(resolveSelectValue(nationalityField, studentPassportEntity.getNationality()));
//            passportEntitySave.setSex(resolveGenderValue(genderSelect, studentPassportEntity.getSex()));
//            passportEntitySave.setEdboNumberPhis(resolveTextFieldValue(personNumberEDEBOField, studentPassportEntity.getEdboNumberPhis()));
//            passportEntitySave.setEdboNumberZdob(resolveTextFieldValue(studentCardNumberEDEBOField, studentPassportEntity.getEdboNumberZdob()));
//            passportEntitySave.setNameEng(resolveTextFieldValue(firstNameEngField, studentPassportEntity.getNameEng()));
//            passportEntitySave.setSurnameEng(resolveTextFieldValue(lastNameEngField, studentPassportEntity.getSurnameEng()));
//            passportEntitySave.setStudent(studentEntitySave);
//            studentPassportService.save(passportEntitySave);
//
//
//            //Збереження змін InfoEntity
//            StudentInfoEntity infoEntitySave = new StudentInfoEntity();
//            infoEntitySave.setId(studentInfoEntity.getId());
//            infoEntitySave.setCaseNumber(resolveTextFieldValue(caseNumberField, studentInfoEntity.getCaseNumber()));
//            infoEntitySave.setRegion(resolveSelectValue(regionSelect, studentInfoEntity.getRegion()));
//            infoEntitySave.setIndex(resolveTextFieldValue(indexField, studentInfoEntity.getIndex()));
//            infoEntitySave.setAddress(resolveTextFieldValue(fullAddressField, studentInfoEntity.getAddress()));
//            infoEntitySave.setStudent(studentEntitySave);
//            studentInfoService.save(infoEntitySave);
//
//                //Збереження змін EducationEntity
//                StudentEducationEntity educationEntitySave = new StudentEducationEntity();
//            educationEntitySave.setId(studentEducationEntity.getId());
//            educationEntitySave.setTypeOfDocument(resolveSelectValue(documentTypeSelect, studentEducationEntity.getTypeOfDocument()));
//            educationEntitySave.setHonors(distinctionCheckbox.getValue() ? 1 : 0);
//            educationEntitySave.setSeries(resolveTextFieldValue(documentSeriesField, studentEducationEntity.getSeries()));
//            educationEntitySave.setNumber(resolveTextFieldValue(documentNumberField, studentEducationEntity.getNumber()));
//            educationEntitySave.setDateOfIssue(resolveDatePickerValue(documentIssueDatePicker, studentEducationEntity.getDateOfIssue()));
//            educationEntitySave.setIssuedBy(resolveTextFieldValue(institutionNameField, studentEducationEntity.getIssuedBy()));
//            educationEntitySave.setIssuedByEng(resolveTextFieldValue(institutionNameEngField, studentEducationEntity.getIssuedByEng()));
//            educationEntitySave.setDiplomaSeries(resolveTextFieldValue(diplomaSeriesField, studentEducationEntity.getDiplomaSeries()));
//            educationEntitySave.setDiplomaNumber(resolveTextFieldValue(diplomaNumberField, studentEducationEntity.getDiplomaNumber()));
//            educationEntitySave.setDateOfIssueDiploma(resolveDatePickerValue(graduationDatePicker, studentEducationEntity.getDateOfIssueDiploma()));
//            educationEntitySave.setNumberOfDodatok(resolveTextFieldValue(appendixNumberField, studentEducationEntity.getNumberOfDodatok()));
//            educationEntitySave.setThemeOfWork(resolveTextFieldValue(thesisTitleUkrField, studentEducationEntity.getThemeOfWork()));
//            educationEntitySave.setThemeOfWorkEng(resolveTextFieldValue(thesisTitleEngField, studentEducationEntity.getThemeOfWorkEng()));
//            educationEntitySave.setStudent(studentEntitySave);
//            studentEducationService.save(educationEntitySave);

            studentEntity = studentEntitySave;
//                studentPassportEntity = passportEntitySave;
//                studentInfoEntity = infoEntitySave;
//                studentEducationEntity = educationEntitySave;

            selectGroup.setValue(selectedGroupEntity.getGroupCode());
            selectStudent.setValue(studentEntitySave.getFullName());

            updateGroupSelectorsItems();
            refreshGraduationYearOptions();
            pendingCreatedGroupId = null;

        },
                "Ні", (event) -> {

            //Відміна змін
            setTextFieldValue(lastNameUkrField, studentEntity.getSurname());
            setTextFieldValue(firstNameUkrField, studentEntity.getName());
            setTextFieldValue(middleNameUkrField, studentEntity.getPatronymic());
            setTextFieldValue(firstNameEngField, studentPassportEntity.getNameEng());
            setTextFieldValue(lastNameEngField, studentPassportEntity.getSurnameEng());
            String originalGroupCode = studentEntity.getGroup() != null ? studentEntity.getGroup().getGroupCode() : null;
            String[] originalGroupParts = originalGroupCode != null ? originalGroupCode.split("-") : new String[0];
            setSelectValue(groupSelect, getGroupPart(originalGroupParts, 0));
            setSelectValue(courseSelect, getGroupPart(originalGroupParts, 1));
            setTextFieldValue(groupNumberField, getGroupPart(originalGroupParts, 2));
            setSelectValue(admissionYearSelect, getGroupPart(originalGroupParts, 3));
            setTextFieldValue(recordBookNumberField, studentEntity.getRecordBookNumber());
            setTextFieldValue(passportSeriesField, studentPassportEntity.getSeries());
            setTextFieldValue(passportNumberField, studentPassportEntity.getNumber());
            setDatePickerValue(passportIssueDatePicker, studentPassportEntity.getIssueDate());
            setTextFieldValue(passportIssuedByField, studentPassportEntity.getIssuedBy());
            setDatePickerValue(passportExpiryDatePicker, studentPassportEntity.getExpireDate());
            setTextFieldValue(idCodeField, studentPassportEntity.getIdentificationNumber());
            setTextFieldValue(unzrField, studentPassportEntity.getUnzrCode());
            setDatePickerValue(birthDatePicker, studentPassportEntity.getBirthdate());
            setSelectValue(nationalityField, studentPassportEntity.getNationality());
            setGenderValue(studentPassportEntity.getSex());
            setTextFieldValue(personNumberEDEBOField, studentPassportEntity.getEdboNumberPhis());
            setSelectValue(degreeSelect, studentInfoEntity.getDegree());
            setSelectValue(admissionConditionSelect, studentInfoEntity.getEntryRequirements());
            setSelectValue(paymentSourceSelect, studentInfoEntity.getTypeOfIndividual());
            setTextFieldValue(contractNumberField, studentInfoEntity.getContractNumber());
            setTextFieldValue(amountField, studentInfoEntity.getTotal());
            setBenefitsValue(studentInfoEntity.getBenefits());
            setTextFieldValue(phoneNumberField, studentInfoEntity.getPhone());
            setTextFieldValue(emailField, studentInfoEntity.getEmail());
            setSelectValue(regionSelect, studentInfoEntity.getRegion());
            setTextFieldValue(indexField, studentInfoEntity.getIndex());
            setTextFieldValue(fullAddressField, studentInfoEntity.getAddress());
            setSelectValue(documentTypeSelect, studentEducationEntity.getTypeOfDocument());
            distinctionCheckbox.setValue(studentEducationEntity.getHonors() == 1);
            setTextFieldValue(documentSeriesField, studentEducationEntity.getSeries());
            setTextFieldValue(documentNumberField, studentEducationEntity.getNumber());
            setDatePickerValue(documentIssueDatePicker, studentEducationEntity.getDateOfIssue());
            setTextFieldValue(institutionNameField, studentEducationEntity.getIssuedBy());
            setTextFieldValue(institutionNameEngField, studentEducationEntity.getIssuedByEng());
            setTextFieldValue(diplomaSeriesField, studentEducationEntity.getDiplomaSeries());
            setTextFieldValue(diplomaNumberField, studentEducationEntity.getDiplomaNumber());
            setDatePickerValue(graduationDatePicker, studentEducationEntity.getDateOfIssueDiploma());
            setTextFieldValue(appendixNumberField, studentEducationEntity.getNumberOfDodatok());
            setTextFieldValue(thesisTitleUkrField, studentEducationEntity.getThemeOfWork());
            setTextFieldValue(thesisTitleEngField, studentEducationEntity.getThemeOfWorkEng());

            if (pendingCreatedGroupId != null) {
                groupService.deleteById(pendingCreatedGroupId);
                pendingCreatedGroupId = null;
                updateGroupSelectorsItems();
                refreshGraduationYearOptions();
            }

        });
        dialog.open();
    }


    private String resolveTextFieldValue(TextField field, String fallback) {
        String value = field.getValue();
        if (value != null) {
            value = value.trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return fallback;
    }


    private String resolveSelectValue(Select<String> select, String fallback) {
        String value = select.getValue();
        if (value != null) {
            value = value.trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return fallback;
    }

    private String resolveDatePickerValue(DatePicker picker, String fallback) {
        LocalDate value = picker.getValue();
        if (value != null) {
            return value.toString();
        }
        return fallback;
    }

    private Date resolveDatePickerValue(DatePicker picker, Date fallback) {
        LocalDate value = picker.getValue();
        if (value != null) {
            return Date.valueOf(value);
        }
        return fallback;
    }

    private String getGroupPart(String[] parts, int index) {
        if (parts.length <= index) {
            return null;
        }
        String part = parts[index];
        if (index == parts.length - 1) {
            int suffixIndex = part.indexOf('(');
            if (suffixIndex > -1) {
                return part.substring(0, suffixIndex);
            }
        }
        return part;
    }

    private MainLayout findMainLayout() {
        UI current = UI.getCurrent();
        if (current == null) {
            return null;
        }

        return current.getChildren()
                .filter(component -> component instanceof MainLayout)
                .map(MainLayout.class::cast)
                .findFirst()
                .orElse(null);
    }

    private List<String> fetchStudentNames(String groupCode) {
        if (groupCode == null || groupCode.isBlank()) {
            return Collections.emptyList();
        }

        Long groupId = groupService.getGroupIdByCode(groupCode);
        if (groupId == null) {
            return Collections.emptyList();
        }

        return studentService.getStudentByGroupId(groupId)
                .stream()
                .map(StudentEntity::getFullName)
                .toList();
    }
}
