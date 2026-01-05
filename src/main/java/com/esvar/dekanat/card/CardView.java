package com.esvar.dekanat.card;


import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.repository.StudentRatingRepository;
import com.esvar.dekanat.service.*;
import com.esvar.dekanat.view.MainLayout;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceWriter;
import jakarta.annotation.security.PermitAll;

import java.io.*;
import java.sql.Date;
import java.text.Collator;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
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
    private final StudentRatingRepository ratingRepository;
    private final GroupCodeService groupCodeService;
    private final StudentRegistrationService studentRegistrationService;


    private VerticalLayout mainLayout = new VerticalLayout();
    private HorizontalLayout leftLayout1Page = new HorizontalLayout();
    private HorizontalLayout rightLayout1Page = new HorizontalLayout();
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
    private final Map<TextField, Pattern> validationPatterns = new LinkedHashMap<>();

    private final SpecialtyService specialtyService;
    private final Collator ukrainianCollator;
    private Long pendingCreatedGroupId;


    public CardView(GroupService groupService, StudentService studentService, StudentPassportService studentPassportService, StudentInfoService studentInfoService, StudentEducationService studentEducationService, StudentReportService studentReportService, ReportService reportService, StudentRatingRepository ratingRepository, SpecialtyService specialtyService, GroupCodeService groupCodeService, StudentRegistrationService studentRegistrationService) {
        this.groupService = groupService;
        this.studentService = studentService;
        this.studentPassportService = studentPassportService;
        this.studentInfoService = studentInfoService;
        this.studentEducationService = studentEducationService;
        this.studentReportService = studentReportService;
        this.reportService = reportService;
        this.ratingRepository = ratingRepository;
        this.specialtyService = specialtyService;
        this.groupCodeService = groupCodeService;
        this.studentRegistrationService = studentRegistrationService;
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
        registerPatternValidation(numberField, "[0-9]", "\\d+", "Номер наказу має містити цифри");

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
                    studentRegistrationService
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


        orderGrid.addColumn(report -> formatOrderNumber(report.getOrderNumber())).setHeader("№ наказу").setAutoWidth(true);
        orderGrid.addColumn(ReportEntity::getStatus).setHeader("Стан").setAutoWidth(true);
        orderGrid.addColumn(report -> formatDateValue(report.getDate())).setHeader("Дата").setAutoWidth(true);
        orderGrid.getStyle().set("border", "1px solid #ddd");
        orderGrid.getStyle().set("border-radius", "8px");
        orderGrid.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        orderGrid.getStyle().set("padding", "20px");
        orderGrid.getStyle().set("position", "relative");
        orderGrid.getStyle().set("background", "white");
        orderGrid.getStyle().set("min-height", "230px");
        orderGrid.setSelectionMode(Grid.SelectionMode.NONE);
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
                        .map(code -> groupCodeService.parseGroupParts(code).groupPrefix())
                        .filter(Objects::nonNull)
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
        registerPatternValidation(groupNumberField, "[0-9]", "[1-9]+", "Введіть цифру від 1 до 9");



        groupList.addClickListener(event -> {
            String selectedGroup = selectGroup.getValue();
            if (selectedGroup == null || selectedGroup.isBlank()) {
                Notification.show("Оберіть групу для генерації списку.");
                return;
            }

            List<String> students = fetchStudentNames(selectedGroup);
            if (students.isEmpty()) {
                Notification.show("У вибраній групі немає студентів для генерації списку.");
                groupList.setEnabled(false);
                return;
            }

            Dialog progressDialog = buildProgressDialog("Генерація PDF...");
            progressDialog.open();
            try {
                generateAndSend(selectedGroup, students);
            } catch (Exception exception) {
                Notification.show("Не вдалося згенерувати PDF: " + exception.getMessage());
            } finally {
                progressDialog.close();
            }
        });



        admissionYearSelect = new Select<>();
        admissionYearSelect.setLabel("Рік випуску");
        admissionYearSelect.setWidth("24%");
        refreshGraduationYearOptions();


        recordBookNumberField = new TextField("Номер заліковки");
        recordBookNumberField.setWidth("24%");
        registerPatternValidation(recordBookNumberField, "[0-9]", "\\d+", "Введіть цифри від 0 до 9");

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
        registerPatternValidation(caseNumberField, "[0-9]", "\\d+", "Введіть тільки цифри");
        idCodeField = new TextField("Ідентифікаційний код");
        registerPatternValidation(idCodeField, "[0-9]", "\\d+", "Введіть тільки цифри");
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
        registerPatternValidation(indexField, "[0-9]", "\\d{1,5}", "Індекс повинен містити до 5 цифр");
        indexField.setMaxLength(5);
        fullAddressField = new TextField("Повна адреса");
        phoneNumberField = new TextField("Номер телефону");
        registerPatternValidation(phoneNumberField, "[0-9]", "\\d+", "Номер телефону має містити лише цифри");
        emailField = new TextField("E-mail");
        benefitsSelect = new MultiSelectComboBox<>();
        benefitsSelect.setLabel("Пільги");
        benefitsSelect.setItems("Пільга 1", "Пільга 2", "Пільга 3"); // Приклад елементів
// Text fields for ЄДЕБО numbers
        personNumberEDEBOField = new TextField("Номер фіз. особи ЄДЕБО");
        studentCardNumberEDEBOField = new TextField("Номер картки здобувача ЄДЕБО");

        registerPatternValidation(personNumberEDEBOField, "[0-9]", "\\d{7,}", "Введіть мінімум 7 цифр");
        registerPatternValidation(studentCardNumberEDEBOField, "[0-9]", "\\d{7,}", "Введіть мінімум 7 цифр");

// Add these fields to the appropriate layout
        VerticalLayout edeboFieldsLayout = new VerticalLayout();
        edeboFieldsLayout.add();
        genderSelect = new Select<>();
        genderSelect.setLabel("Стать");
        genderSelect.setItems("Чоловіча", "Жіноча");

        passportSeriesField = new TextField("Серія паспорту");
        passportNumberField = new TextField("№ паспорту");
        registerPatternValidation(passportSeriesField, "[\\p{L}0-9]", "[\\p{L}0-9]+", "Серія паспорту має містити літери або цифри");
        registerPatternValidation(passportNumberField, "[0-9]", "\\d+", "Введіть тільки цифри");
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
        registerPatternValidation(contractNumberField, "[0-9]", "\\d+", "Введіть цифри від 0 до 9");
        amountField = new TextField("Сума");
        registerPatternValidation(amountField, "[0-9]", "\\d+", "Введіть цифри від 0 до 9");


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
        passportDetailsLayout.add(passportSeriesField, passportNumberField, passportIssueDatePicker, passportExpiryDatePicker, passportIssuedByField, idCodeField, unzrField, birthDatePicker, nationalityField, genderSelect, personNumberEDEBOField, studentCardNumberEDEBOField);

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
        additionalInfoLayout.add(educationDetailsWrapper, contactDetailsWrapper, addressDetailsWrapper);
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
        registerPatternValidation(documentSeriesField, "[\\p{L}0-9]", "[\\p{L}0-9]+", "Серія документу має містити літери або цифри");
        registerPatternValidation(documentNumberField, "[0-9]", "\\d+", "Введіть тільки цифри");
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
        registerPatternValidation(diplomaSeriesField, "[\\p{L}0-9]", "[\\p{L}0-9]+", "Серія диплому має містити літери або цифри");
        registerPatternValidation(diplomaNumberField, "[0-9]", "\\d+", "Введіть тільки цифри");
        graduationDatePicker = new DatePicker("Дата випуску");
        graduationDatePicker.setI18n(setLocal());
        appendixNumberField = new TextField("Номер додатку");
        registerPatternValidation(appendixNumberField, "[0-9]", "\\d+", "Введіть тільки цифри");
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
        setOrderSectionReadOnly(true);

        //Обробка вибору студента
        selectStudent.addValueChangeListener(event -> {
            if (event.getValue() == null) {
                clearStudentContext();
                return;
            }
            studentEntity = studentService.getStudentForCard(selectGroup.getValue(), event.getValue());
            reloadRelatedEntities();
            populateViewFromEntities();
            resetEditingState();
        });

        //Обробка додавання нової відомості
        submitDataButton.addClickListener(buttonClickEvent -> handleReportSubmission());

        //обробка режиму редагування
        editButton.addClickListener(buttonClickEvent -> {
            MainLayout mainLayout = findMainLayout();
            if (editButton.getText().equals("Редагувати")) {
                if (studentEntity == null) {
                    Notification.show("Спочатку оберіть студента.");
                    return;
                }
                enterEditMode(mainLayout);
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
                if (!validateBeforeSave(selectedGroupEntity).isEmpty()) {
                    return;
                }
                showConfirmationDialog(selectedGroupEntity);
            }
        });

        sendToArchiveButton.addClickListener(event -> {
            if (studentEntity == null) {
                Notification.show("Спочатку оберіть студента.");
                return;
            }
            ConfirmDialog dialog = new ConfirmDialog(
                    "Відправити в архів",
                    "Картку буде позначено як архівну та недоступну для редагування. Продовжити?",
                    "Так",
                    confirmEvent -> handleArchiveAction(),
                    "Скасувати",
                    cancelEvent -> {
                    }
            );
            dialog.setCancelable(true);
            dialog.open();
        });
    }

    private void handleReportSubmission() {
        if (!validateReportInputs()) {
            return;
        }

        boolean wholeGroup = "Вся група".equals(studentOrGroupSelect.getValue());
        if (wholeGroup) {
            List<StudentEntity> studentModels = studentService.getStudentsForCard(selectGroup.getValue());
            if (studentModels.isEmpty()) {
                Notification.show("У вибраній групі немає студентів.");
                return;
            }
            ConfirmDialog dialog = new ConfirmDialog(
                    "Підтвердження відомості",
                    "Додати відомість для " + studentModels.size() + " студентів групи?",
                    "Додати",
                    confirmEvent -> createReportsWithUndo(studentModels),
                    "Скасувати",
                    cancelEvent -> {
                    }
            );
            dialog.setCancelable(true);
            dialog.open();
        } else {
            try {
                StudentEntity studentEntityMain = studentService.getStudentForCard(selectGroup.getValue(), selectStudent.getValue());
                createReportsWithUndo(List.of(studentEntityMain));
            } catch (Exception exception) {
                Notification.show("Не вдалося знайти студента: " + exception.getMessage());
            }
        }
    }

    private void createReportsWithUndo(List<StudentEntity> students) {
        if (students == null || students.isEmpty()) {
            Notification.show("Не знайдено студентів для створення відомості.");
            return;
        }
        List<Long> createdIds = new ArrayList<>();
        LocalDate reportDate = datePicker.getValue();
        Long orderNumber = Long.parseLong(numberField.getValue());
        try {
            for (StudentEntity studentModel : students) {
                ReportEntity reportEntity = new ReportEntity();
                reportEntity.setStudent(studentModel);
                reportEntity.setStatus(typeOfInformationSelect.getValue());
                reportEntity.setDate(Date.valueOf(reportDate));
                reportEntity.setOrderNumber(orderNumber);
                ReportEntity saved = reportService.saveReport(reportEntity);
                createdIds.add(saved.getId());
            }
            refreshReportsGrid();
            showUndoNotification("Відомість збережено.", createdIds);
        } catch (Exception exception) {
            reportService.deleteReportsByIds(createdIds);
            Notification.show("Не вдалося зберегти відомість: " + exception.getMessage());
        }
    }

    private boolean validateReportInputs() {
        clearValidationStates();
        List<String> errors = new ArrayList<>();
        Pattern numberPattern = validationPatterns.get(numberField);
        if (hasText(numberField.getValue()) && numberPattern != null && !numberPattern.matcher(numberField.getValue()).matches()) {
            numberField.setInvalid(true);
            errors.add(numberField.getErrorMessage());
        }
        if (typeOfInformationSelect.getValue() == null) {
            errors.add("Оберіть тип відомості");
            typeOfInformationSelect.setInvalid(true);
        }
        if (datePicker.getValue() == null) {
            errors.add("Оберіть дату");
            datePicker.setInvalid(true);
        }
        if (!hasText(numberField.getValue())) {
            errors.add("Вкажіть номер наказу");
            numberField.setInvalid(true);
        }
        if (studentOrGroupSelect.getValue() == null) {
            errors.add("Оберіть режим (студент/група)");
            studentOrGroupSelect.setInvalid(true);
        } else if ("Один студент".equals(studentOrGroupSelect.getValue()) && selectStudent.getValue() == null) {
            errors.add("Оберіть студента");
            selectStudent.setInvalid(true);
        }

        if (selectGroup.getValue() == null) {
            errors.add("Оберіть групу");
            selectGroup.setInvalid(true);
        }

        if (!errors.isEmpty()) {
            Notification.show(String.join("; ", errors));
            return false;
        }
        return true;
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

        List<String> candidates = groupCodeService.buildCandidateGroupCodes(groupPrefix, course, groupNumber, graduationYear, specialty);
        for (int i = 0; i < candidates.size(); i++) {
            String candidate = candidates.get(i);
            StudentGroupEntity group = groupService.getGroupByTitle(candidate);
            if (group == null) {
                continue;
            }
            boolean legacyCandidate = i == candidates.size() - 1 && candidate.equals(groupCodeService.buildLegacyGroupCode(groupPrefix, course, groupNumber, graduationYear));
            if (!legacyCandidate || isCurrentStudentGroup(group)) {
                return group;
            }
        }
        return null;
    }

    private Optional<StudentGroupEntity> findLegacyGroupCandidate() {
        if (!isGroupSelectionComplete()) {
            return Optional.empty();
        }

        String legacyGroupCode = groupCodeService.buildLegacyGroupCode(
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
        SpecialtyEntity specialty = specialtyService.getSpecialtyByAbbreviation(groupPrefix);
        return groupCodeService.buildGroupCode(groupPrefix, course, groupNumber, graduationYear, specialty);
    }

    private String buildGroupCode(String groupPrefix, String course, String groupNumber, String graduationYear, SpecialtyEntity specialty) {
        return groupCodeService.buildGroupCode(groupPrefix, course, groupNumber, graduationYear, specialty);
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
        setFieldsReadOnly(true);

        applyStudentChanges(selectedGroupEntity);
        savePassportChanges();
        saveInfoChanges();
        saveEducationChanges();

        resetEditingState();
        reloadSelectedStudent();
        Notification.show("Дані збережено.");
    }

    private void applyStudentChanges(StudentGroupEntity selectedGroupEntity) {
        studentEntity.setSurname(trimToNull(lastNameUkrField.getValue()));
        studentEntity.setName(trimToNull(firstNameUkrField.getValue()));
        studentEntity.setPatronymic(trimToNull(middleNameUkrField.getValue()));
        studentEntity.setRecordBookNumber(trimToNull(recordBookNumberField.getValue()));

        boolean academicDataChanged = hasAcademicDataChanges(selectedGroupEntity);
        studentEntity.setGroup(selectedGroupEntity);
        studentEntity.setFaculty(selectedGroupEntity.getSpecialty().getFaculty());

        if (academicDataChanged) {
            applyAcademicDataChanges(selectedGroupEntity);
        } else {
            studentService.save(studentEntity);
        }
    }

    private void savePassportChanges() {
        if (!hasPassportInput() && studentPassportEntity == null) {
            return;
        }
        StudentPassportEntity passport = studentPassportEntity != null ? studentPassportEntity : new StudentPassportEntity();
        passport.setStudent(studentEntity);
        passport.setSeries(trimToNull(passportSeriesField.getValue()));
        passport.setNumber(trimToNull(passportNumberField.getValue()));
        passport.setIssueDate(resolveDatePickerValue(passportIssueDatePicker, passport.getIssueDate()));
        passport.setExpireDate(resolveDatePickerValue(passportExpiryDatePicker, passport.getExpireDate()));
        passport.setIssuedBy(trimToNull(passportIssuedByField.getValue()));
        passport.setIdentificationNumber(trimToNull(idCodeField.getValue()));
        passport.setUnzrCode(trimToNull(unzrField.getValue()));
        passport.setBirthdate(resolveDatePickerValue(birthDatePicker, passport.getBirthdate()));
        passport.setNationality(trimToNull(nationalityField.getValue()));
        passport.setSex(resolveGenderValue(genderSelect, passport.getSex()));
        passport.setEdboNumberPhis(trimToNull(personNumberEDEBOField.getValue()));
        passport.setEdboNumberZdob(trimToNull(studentCardNumberEDEBOField.getValue()));
        passport.setNameEng(trimToNull(firstNameEngField.getValue()));
        passport.setSurnameEng(trimToNull(lastNameEngField.getValue()));
        studentPassportEntity = studentPassportService.save(passport);
    }

    private void saveInfoChanges() {
        if (!hasInfoInput() && studentInfoEntity == null) {
            return;
        }
        StudentInfoEntity info = studentInfoEntity != null ? studentInfoEntity : new StudentInfoEntity();
        info.setStudent(studentEntity);
        info.setCaseNumber(trimToNull(caseNumberField.getValue()));
        info.setFormStudy(trimToNull(educationFormSelect.getValue()));
        info.setDegree(trimToNull(degreeSelect.getValue()));
        info.setEntryRequirements(trimToNull(admissionConditionSelect.getValue()));
        info.setTypeOfIndividual(trimToNull(paymentSourceSelect.getValue()));
        info.setContractNumber(trimToNull(contractNumberField.getValue()));
        info.setTotal(trimToNull(amountField.getValue()));
        info.setBenefits(benefitsSelect.getValue().isEmpty()
                ? null
                : String.join(", ", benefitsSelect.getValue()));
        info.setRegion(trimToNull(regionSelect.getValue()));
        info.setIndex(trimToNull(indexField.getValue()));
        info.setAddress(trimToNull(fullAddressField.getValue()));
        info.setPhone(trimToNull(phoneNumberField.getValue()));
        info.setEmail(trimToNull(emailField.getValue()));
        studentInfoService.save(info);
        studentInfoEntity = info;
    }

    private void saveEducationChanges() {
        if (!hasEducationInput() && studentEducationEntity == null) {
            return;
        }
        StudentEducationEntity education = studentEducationEntity != null ? studentEducationEntity : new StudentEducationEntity();
        education.setStudent(studentEntity);
        education.setTypeOfDocument(trimToNull(documentTypeSelect.getValue()));
        education.setHonors(Boolean.TRUE.equals(distinctionCheckbox.getValue()) ? 1 : 0);
        education.setSeries(trimToNull(documentSeriesField.getValue()));
        education.setNumber(trimToNull(documentNumberField.getValue()));
        education.setDateOfIssue(resolveDatePickerValue(documentIssueDatePicker, education.getDateOfIssue()));
        education.setIssuedBy(trimToNull(institutionNameField.getValue()));
        education.setIssuedByEng(trimToNull(institutionNameEngField.getValue()));
        education.setDiplomaSeries(trimToNull(diplomaSeriesField.getValue()));
        education.setDiplomaNumber(trimToNull(diplomaNumberField.getValue()));
        education.setDateOfIssueDiploma(resolveDatePickerValue(graduationDatePicker, education.getDateOfIssueDiploma()));
        education.setNumberOfDodatok(trimToNull(appendixNumberField.getValue()));
        education.setThemeOfWork(trimToNull(thesisTitleUkrField.getValue()));
        education.setThemeOfWorkEng(trimToNull(thesisTitleEngField.getValue()));
        studentEducationService.save(education);
        studentEducationEntity = education;
    }

    private List<String> validateBeforeSave(StudentGroupEntity selectedGroupEntity) {
        clearValidationStates();
        List<String> errors = new ArrayList<>(validatePatterns());
        if (selectedGroupEntity == null) {
            errors.add("Оберіть групу");
            groupSelect.setInvalid(true);
            courseSelect.setInvalid(true);
            admissionYearSelect.setInvalid(true);
        }
        if (!hasText(lastNameUkrField.getValue())) {
            errors.add("Прізвище обов'язкове");
            lastNameUkrField.setInvalid(true);
        }
        if (!hasText(firstNameUkrField.getValue())) {
            errors.add("Ім'я обов'язкове");
            firstNameUkrField.setInvalid(true);
        }
        if ((studentPassportEntity != null || hasPassportInput())) {
            if (!hasText(passportSeriesField.getValue())) {
                errors.add("Серія паспорту обов'язкова");
                passportSeriesField.setInvalid(true);
            }
            if (!hasText(passportNumberField.getValue())) {
                errors.add("Номер паспорту обов'язковий");
                passportNumberField.setInvalid(true);
            }
            if (passportIssueDatePicker.getValue() == null) {
                errors.add("Вкажіть дату видачі паспорту");
                passportIssueDatePicker.setInvalid(true);
            }
            if (passportExpiryDatePicker.getValue() == null) {
                errors.add("Вкажіть дату завершення дії паспорту");
                passportExpiryDatePicker.setInvalid(true);
            }
            if (!hasText(nationalityField.getValue())) {
                errors.add("Вкажіть національність");
                nationalityField.setInvalid(true);
            }
            if (genderSelect.getValue() == null) {
                errors.add("Вкажіть стать");
                genderSelect.setInvalid(true);
            }
        }
        if ((studentInfoEntity != null || hasInfoInput())) {
            if (!hasText(fullAddressField.getValue())) {
                errors.add("Адреса обов'язкова");
                fullAddressField.setInvalid(true);
            }
            if (!hasText(phoneNumberField.getValue())) {
                errors.add("Номер телефону обов'язковий");
                phoneNumberField.setInvalid(true);
            }
            if (!hasText(emailField.getValue())) {
                errors.add("Email обов'язковий");
                emailField.setInvalid(true);
            } else if (!emailField.getValue().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                errors.add("Невірний формат email");
                emailField.setInvalid(true);
            }
        }
        if (!errors.isEmpty()) {
            Notification.show(String.join("; ", errors));
        }
        return errors;
    }

    private boolean hasPassportInput() {
        return hasText(passportSeriesField.getValue())
                || hasText(passportNumberField.getValue())
                || passportIssueDatePicker.getValue() != null
                || passportExpiryDatePicker.getValue() != null
                || hasText(passportIssuedByField.getValue())
                || hasText(idCodeField.getValue())
                || hasText(unzrField.getValue())
                || birthDatePicker.getValue() != null
                || hasText(nationalityField.getValue())
                || genderSelect.getValue() != null
                || hasText(personNumberEDEBOField.getValue())
                || hasText(studentCardNumberEDEBOField.getValue())
                || hasText(firstNameEngField.getValue())
                || hasText(lastNameEngField.getValue());
    }

    private boolean hasInfoInput() {
        return hasText(caseNumberField.getValue())
                || hasText(fullAddressField.getValue())
                || hasText(phoneNumberField.getValue())
                || hasText(emailField.getValue())
                || hasText(regionSelect.getValue())
                || hasText(indexField.getValue())
                || hasText(educationFormSelect.getValue())
                || hasText(degreeSelect.getValue())
                || hasText(admissionConditionSelect.getValue())
                || hasText(paymentSourceSelect.getValue())
                || hasText(contractNumberField.getValue())
                || hasText(amountField.getValue())
                || !benefitsSelect.getValue().isEmpty();
    }

    private boolean hasEducationInput() {
        return hasText(documentTypeSelect.getValue())
                || Boolean.TRUE.equals(distinctionCheckbox.getValue())
                || hasText(documentSeriesField.getValue())
                || hasText(documentNumberField.getValue())
                || documentIssueDatePicker.getValue() != null
                || hasText(institutionNameField.getValue())
                || hasText(institutionNameEngField.getValue())
                || hasText(diplomaSeriesField.getValue())
                || hasText(diplomaNumberField.getValue())
                || graduationDatePicker.getValue() != null
                || hasText(appendixNumberField.getValue())
                || hasText(thesisTitleUkrField.getValue())
                || hasText(thesisTitleEngField.getValue());
    }

    private void registerPatternValidation(TextField field, String allowedCharPattern, String valueRegex, String errorMessage) {
        field.setAllowedCharPattern(allowedCharPattern);
        field.setPreventInvalidInput(true);
        if (valueRegex != null) {
            validationPatterns.put(field, Pattern.compile(valueRegex));
            field.setPattern(valueRegex);
        }
        field.setErrorMessage(errorMessage);
    }

    private void clearValidationStates() {
        validationPatterns.keySet().forEach(textField -> textField.setInvalid(false));
        List.of(lastNameUkrField, firstNameUkrField, middleNameUkrField, lastNameEngField, firstNameEngField,
                        recordBookNumberField, phoneNumberField, emailField, contractNumberField, amountField, numberField)
                .forEach(field -> field.setInvalid(false));
        List.of(typeOfInformationSelect, groupSelect, courseSelect, admissionYearSelect, studentOrGroupSelect, selectStudent,
                        nationalityField, genderSelect, educationFormSelect, degreeSelect, admissionConditionSelect,
                        paymentSourceSelect, regionSelect)
                .forEach(component -> component.setInvalid(false));
        List.of(passportIssueDatePicker, passportExpiryDatePicker, datePicker).forEach(datePicker -> datePicker.setInvalid(false));
    }

    private List<String> validatePatterns() {
        List<String> errors = new ArrayList<>();
        validationPatterns.forEach((field, pattern) -> {
            String value = field.getValue();
            if (hasText(value) && !pattern.matcher(value).matches()) {
                field.setInvalid(true);
                errors.add(buildFieldErrorMessage(field, "Невірний формат"));
            }
        });
        return errors;
    }

    private String buildFieldErrorMessage(TextField field, String fallback) {
        String label = field.getLabel();
        String errorMessage = field.getErrorMessage();
        if (errorMessage != null && !errorMessage.isBlank()) {
            return errorMessage;
        }
        if (label != null && !label.isBlank()) {
            return label + ": " + fallback;
        }
        return fallback;
    }

    private void setFieldsReadOnly(boolean readOnly) {
        setOrderSectionReadOnly(readOnly);
        setPersonalSectionReadOnly(readOnly);
        setAcademicSectionReadOnly(readOnly);
        setPassportSectionReadOnly(readOnly);
        setContactSectionReadOnly(readOnly);
        setEducationSectionReadOnly(readOnly);
    }

    private void setOrderSectionReadOnly(boolean readOnly) {
        typeOfInformationSelect.setReadOnly(readOnly);
        datePicker.setReadOnly(readOnly);
        numberField.setReadOnly(readOnly);
        studentOrGroupSelect.setReadOnly(readOnly);
        submitDataButton.setEnabled(!readOnly);
        orderGrid.setEnabled(!readOnly);
    }

    private void setPersonalSectionReadOnly(boolean readOnly) {
        lastNameUkrField.setReadOnly(readOnly);
        firstNameUkrField.setReadOnly(readOnly);
        middleNameUkrField.setReadOnly(readOnly);
        lastNameEngField.setReadOnly(readOnly);
        firstNameEngField.setReadOnly(readOnly);
    }

    private void setAcademicSectionReadOnly(boolean readOnly) {
        groupSelect.setReadOnly(readOnly);
        courseSelect.setReadOnly(readOnly);
        groupNumberField.setReadOnly(readOnly);
        admissionYearSelect.setReadOnly(readOnly);
        recordBookNumberField.setReadOnly(readOnly);
    }

    private void setPassportSectionReadOnly(boolean readOnly) {
        passportSeriesField.setReadOnly(readOnly);
        passportNumberField.setReadOnly(readOnly);
        passportIssueDatePicker.setReadOnly(readOnly);
        passportIssuedByField.setReadOnly(readOnly);
        passportExpiryDatePicker.setReadOnly(readOnly);
        idCodeField.setReadOnly(readOnly);
        unzrField.setReadOnly(readOnly);
        birthDatePicker.setReadOnly(readOnly);
        nationalityField.setReadOnly(readOnly);
        genderSelect.setReadOnly(readOnly);
        personNumberEDEBOField.setReadOnly(readOnly);
        studentCardNumberEDEBOField.setReadOnly(readOnly);
    }

    private void setContactSectionReadOnly(boolean readOnly) {
        caseNumberField.setReadOnly(readOnly);
        educationFormSelect.setReadOnly(readOnly);
        degreeSelect.setReadOnly(readOnly);
        admissionConditionSelect.setReadOnly(readOnly);
        paymentSourceSelect.setReadOnly(readOnly);
        contractNumberField.setReadOnly(readOnly);
        amountField.setReadOnly(readOnly);
        benefitsSelect.setReadOnly(readOnly);
        phoneNumberField.setReadOnly(readOnly);
        emailField.setReadOnly(readOnly);
        regionSelect.setReadOnly(readOnly);
        indexField.setReadOnly(readOnly);
        fullAddressField.setReadOnly(readOnly);
    }

    private void setEducationSectionReadOnly(boolean readOnly) {
        documentTypeSelect.setReadOnly(readOnly);
        distinctionCheckbox.setReadOnly(readOnly);
        documentSeriesField.setReadOnly(readOnly);
        documentNumberField.setReadOnly(readOnly);
        documentIssueDatePicker.setReadOnly(readOnly);
        institutionNameField.setReadOnly(readOnly);
        institutionNameEngField.setReadOnly(readOnly);
        diplomaSeriesField.setReadOnly(readOnly);
        diplomaNumberField.setReadOnly(readOnly);
        graduationDatePicker.setReadOnly(readOnly);
        appendixNumberField.setReadOnly(readOnly);
        thesisTitleUkrField.setReadOnly(readOnly);
        thesisTitleEngField.setReadOnly(readOnly);
    }

    private void resetEditingState() {
        setFieldsReadOnly(true);
        selectStudent.setEnabled(true);
        selectGroup.setEnabled(true);
        addCardButton.setEnabled(true);
        sendToArchiveButton.setEnabled(true);

        MainLayout mainLayout = findMainLayout();
        if (mainLayout != null) {
            mainLayout.setDrawerEnabled(true);
        }

        editButton.setText("Редагувати");
        editButton.getStyle().set("color", "#0056b3");
    }

    private void enterEditMode(MainLayout mainLayout) {
        setFieldsReadOnly(false);
        editButton.setText("Зберегти");
        editButton.getStyle().set("color", "green");
        selectStudent.setEnabled(false);
        selectGroup.setEnabled(false);
        addCardButton.setEnabled(false);
        sendToArchiveButton.setEnabled(false);
        if (mainLayout != null) {
            mainLayout.setDrawerEnabled(false);
        }
    }

    private void revertEditingChanges() {
        if (pendingCreatedGroupId != null) {
            groupService.deleteById(pendingCreatedGroupId);
            pendingCreatedGroupId = null;
            updateGroupSelectorsItems();
            refreshGraduationYearOptions();
        }
        reloadSelectedStudent();
        resetEditingState();
    }

    private void reloadSelectedStudent() {
        if (studentEntity == null || studentEntity.getId() == null) {
            return;
        }
        studentEntity = studentService.findStudentById(studentEntity.getId());
        reloadRelatedEntities();
        populateViewFromEntities();
    }

    private void reloadRelatedEntities() {
        studentPassportEntity = studentPassportService.getPassportByStudentModel(studentEntity).orElse(null);
        studentInfoEntity = studentInfoService.getInfoByStudentModel(studentEntity);
        studentEducationEntity = studentEducationService.getEducationByStudentModel(studentEntity);
    }

    private void populateViewFromEntities() {
        populatePersonalSection();
        populateAcademicSection();
        populatePassportSection();
        populateInfoSection();
        populateEducationSection();
        refreshReportsGrid();
        setFieldsReadOnly(true);
    }

    private void populatePersonalSection() {
        if (studentEntity == null) {
            return;
        }
        setTextFieldValue(lastNameUkrField, studentEntity.getSurname());
        setTextFieldValue(firstNameUkrField, studentEntity.getName());
        setTextFieldValue(middleNameUkrField, studentEntity.getPatronymic());
    }

    private void populateAcademicSection() {
        if (studentEntity == null) {
            return;
        }
        GroupCodeService.GroupParts groupParts = groupCodeService.parseGroupParts(
                studentEntity.getGroup() != null ? studentEntity.getGroup().getGroupCode() : null
        );
        setSelectValue(groupSelect, groupParts.groupPrefix());
        setSelectValue(courseSelect, groupParts.course());
        setTextFieldValue(groupNumberField, groupParts.groupNumber());
        setSelectValue(admissionYearSelect, groupParts.graduationYear());
        setTextFieldValue(recordBookNumberField, studentEntity.getRecordBookNumber());
    }

    private void populatePassportSection() {
        setTextFieldValue(passportSeriesField, studentPassportEntity != null ? studentPassportEntity.getSeries() : null);
        setTextFieldValue(passportNumberField, studentPassportEntity != null ? studentPassportEntity.getNumber() : null);
        setDatePickerValue(passportIssueDatePicker, studentPassportEntity != null ? studentPassportEntity.getIssueDate() : null);
        setTextFieldValue(passportIssuedByField, studentPassportEntity != null ? studentPassportEntity.getIssuedBy() : null);
        setDatePickerValue(passportExpiryDatePicker, studentPassportEntity != null ? studentPassportEntity.getExpireDate() : null);
        setTextFieldValue(idCodeField, studentPassportEntity != null ? studentPassportEntity.getIdentificationNumber() : null);
        setTextFieldValue(unzrField, studentPassportEntity != null ? studentPassportEntity.getUnzrCode() : null);
        setDatePickerValue(birthDatePicker, studentPassportEntity != null ? studentPassportEntity.getBirthdate() : null);
        setSelectValue(nationalityField, studentPassportEntity != null ? studentPassportEntity.getNationality() : null);
        setGenderValue(studentPassportEntity != null ? studentPassportEntity.getSex() : null);
        setTextFieldValue(personNumberEDEBOField, studentPassportEntity != null ? studentPassportEntity.getEdboNumberPhis() : null);
        setTextFieldValue(studentCardNumberEDEBOField, studentPassportEntity != null ? studentPassportEntity.getEdboNumberZdob() : null);
        setTextFieldValue(firstNameEngField, studentPassportEntity != null ? studentPassportEntity.getNameEng() : null);
        setTextFieldValue(lastNameEngField, studentPassportEntity != null ? studentPassportEntity.getSurnameEng() : null);
    }

    private void populateInfoSection() {
        setTextFieldValue(caseNumberField, studentInfoEntity != null ? studentInfoEntity.getCaseNumber() : null);
        setSelectValue(educationFormSelect, studentInfoEntity != null ? studentInfoEntity.getFormStudy() : null);
        setSelectValue(degreeSelect, studentInfoEntity != null ? studentInfoEntity.getDegree() : null);
        setSelectValue(admissionConditionSelect, studentInfoEntity != null ? studentInfoEntity.getEntryRequirements() : null);
        setSelectValue(paymentSourceSelect, studentInfoEntity != null ? studentInfoEntity.getTypeOfIndividual() : null);
        setTextFieldValue(contractNumberField, studentInfoEntity != null ? studentInfoEntity.getContractNumber() : null);
        setTextFieldValue(amountField, studentInfoEntity != null ? studentInfoEntity.getTotal() : null);
        setBenefitsValue(studentInfoEntity != null ? studentInfoEntity.getBenefits() : null);
        setTextFieldValue(phoneNumberField, studentInfoEntity != null ? studentInfoEntity.getPhone() : null);
        setTextFieldValue(emailField, studentInfoEntity != null ? studentInfoEntity.getEmail() : null);
        setSelectValue(regionSelect, studentInfoEntity != null ? studentInfoEntity.getRegion() : null);
        setTextFieldValue(indexField, studentInfoEntity != null ? studentInfoEntity.getIndex() : null);
        setTextFieldValue(fullAddressField, studentInfoEntity != null ? studentInfoEntity.getAddress() : null);
    }

    private void populateEducationSection() {
        setSelectValue(documentTypeSelect, studentEducationEntity != null ? studentEducationEntity.getTypeOfDocument() : null);
        distinctionCheckbox.setValue(studentEducationEntity != null && studentEducationEntity.getHonors() == 1);
        setTextFieldValue(documentSeriesField, studentEducationEntity != null ? studentEducationEntity.getSeries() : null);
        setTextFieldValue(documentNumberField, studentEducationEntity != null ? studentEducationEntity.getNumber() : null);
        setDatePickerValue(documentIssueDatePicker, studentEducationEntity != null ? studentEducationEntity.getDateOfIssue() : null);
        setTextFieldValue(institutionNameField, studentEducationEntity != null ? studentEducationEntity.getIssuedBy() : null);
        setTextFieldValue(institutionNameEngField, studentEducationEntity != null ? studentEducationEntity.getIssuedByEng() : null);
        setTextFieldValue(diplomaSeriesField, studentEducationEntity != null ? studentEducationEntity.getDiplomaSeries() : null);
        setTextFieldValue(diplomaNumberField, studentEducationEntity != null ? studentEducationEntity.getDiplomaNumber() : null);
        setDatePickerValue(graduationDatePicker, studentEducationEntity != null ? studentEducationEntity.getDateOfIssueDiploma() : null);
        setTextFieldValue(appendixNumberField, studentEducationEntity != null ? studentEducationEntity.getNumberOfDodatok() : null);
        setTextFieldValue(thesisTitleUkrField, studentEducationEntity != null ? studentEducationEntity.getThemeOfWork() : null);
        setTextFieldValue(thesisTitleEngField, studentEducationEntity != null ? studentEducationEntity.getThemeOfWorkEng() : null);
    }

    private void refreshReportsGrid() {
        if (studentEntity == null || studentEntity.getId() == null) {
            orderGrid.setItems(Collections.emptyList());
            return;
        }
        orderGrid.setItems(studentReportService.getReportsByStudentId(studentEntity.getId()));
    }

    private void clearStudentContext() {
        studentEntity = null;
        studentPassportEntity = null;
        studentInfoEntity = null;
        studentEducationEntity = null;
        setTextFieldValue(lastNameUkrField, null);
        setTextFieldValue(firstNameUkrField, null);
        setTextFieldValue(middleNameUkrField, null);
        setTextFieldValue(lastNameEngField, null);
        setTextFieldValue(firstNameEngField, null);
        setSelectValue(groupSelect, null);
        setSelectValue(courseSelect, null);
        setTextFieldValue(groupNumberField, null);
        setSelectValue(admissionYearSelect, null);
        setTextFieldValue(recordBookNumberField, null);
        setTextFieldValue(passportSeriesField, null);
        setTextFieldValue(passportNumberField, null);
        setDatePickerValue(passportIssueDatePicker, (String) null);
        setTextFieldValue(passportIssuedByField, null);
        setDatePickerValue(passportExpiryDatePicker, (String) null);
        setTextFieldValue(idCodeField, null);
        setTextFieldValue(unzrField, null);
        setDatePickerValue(birthDatePicker, (String) null);
        setSelectValue(nationalityField, null);
        setGenderValue(null);
        setTextFieldValue(personNumberEDEBOField, null);
        setTextFieldValue(studentCardNumberEDEBOField, null);
        setTextFieldValue(caseNumberField, null);
        setSelectValue(educationFormSelect, null);
        setSelectValue(degreeSelect, null);
        setSelectValue(admissionConditionSelect, null);
        setSelectValue(paymentSourceSelect, null);
        setTextFieldValue(contractNumberField, null);
        setTextFieldValue(amountField, null);
        setBenefitsValue(null);
        setTextFieldValue(phoneNumberField, null);
        setTextFieldValue(emailField, null);
        setSelectValue(regionSelect, null);
        setTextFieldValue(indexField, null);
        setTextFieldValue(fullAddressField, null);
        setSelectValue(documentTypeSelect, null);
        distinctionCheckbox.setValue(false);
        setTextFieldValue(documentSeriesField, null);
        setTextFieldValue(documentNumberField, null);
        setDatePickerValue(documentIssueDatePicker, (Date) null);
        setTextFieldValue(institutionNameField, null);
        setTextFieldValue(institutionNameEngField, null);
        setTextFieldValue(diplomaSeriesField, null);
        setTextFieldValue(diplomaNumberField, null);
        setDatePickerValue(graduationDatePicker, (Date) null);
        setTextFieldValue(appendixNumberField, null);
        setTextFieldValue(thesisTitleUkrField, null);
        setTextFieldValue(thesisTitleEngField, null);
        setFieldsReadOnly(true);
        orderGrid.setItems(Collections.emptyList());
    }

    private void handleArchiveAction() {
        ReportEntity archiveRecord = archiveSelectedStudent();
        if (archiveRecord != null) {
            showUndoNotification("Картку студента відправлено в архів.", List.of(archiveRecord.getId()));
        }
    }

    private ReportEntity archiveSelectedStudent() {
        if (studentEntity == null) {
            return null;
        }
        ReportEntity archiveRecord = reportService.archiveStudent(studentEntity);
        refreshReportsGrid();
        return archiveRecord;
    }

    private String formatOrderNumber(Long orderNumber) {
        return orderNumber != null ? String.valueOf(orderNumber) : "";
    }

    private String formatDateValue(Date date) {
        return date != null ? date.toLocalDate().toString() : "";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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
                .map(code -> groupCodeService.parseGroupParts(code).groupPrefix())
                .filter(Objects::nonNull)
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
                "Так", (event) -> processSave(selectedGroupEntity),
                "Ні", (event) -> revertEditingChanges());
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

    private Gender resolveGenderValue(Select<String> select, Gender fallback) {
        String value = select.getValue();
        if (value != null) {
            value = value.trim();
            if (!value.isEmpty()) {
                try {
                    return Gender.valueOf(value);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return fallback;
    }

    private void setTextFieldValue(TextField field, String value) {
        if (value != null) {
            field.setValue(value);
        } else {
            field.clear();
        }
    }

    private void setSelectValue(Select<String> select, String value) {
        if (value != null) {
            select.setValue(value);
        } else {
            select.clear();
        }
    }

    private void setDatePickerValue(DatePicker picker, String value) {
        if (value != null && !value.isBlank()) {
            try {
                picker.setValue(LocalDate.parse(value));
            } catch (DateTimeParseException ignored) {
                picker.clear();
            }
        } else {
            picker.clear();
        }
    }

    private void setDatePickerValue(DatePicker picker, Date value) {
        if (value != null) {
            picker.setValue(value.toLocalDate());
        } else {
            picker.clear();
        }
    }

    private void setGenderValue(Gender gender) {
        if (gender != null) {
            genderSelect.setValue(gender.name());
        } else {
            genderSelect.clear();
        }
    }

    private void setBenefitsValue(String benefits) {
        if (benefits != null && !benefits.isBlank()) {
            Set<String> values = Arrays.stream(benefits.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (values.isEmpty()) {
                benefitsSelect.clear();
            } else {
                benefitsSelect.setValue(values);
            }
        } else {
            benefitsSelect.clear();
        }
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

    private Dialog buildProgressDialog(String message) {
        Dialog dialog = new Dialog();
        dialog.setCloseOnOutsideClick(false);
        dialog.setCloseOnEsc(false);
        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        VerticalLayout layout = new VerticalLayout(new Span(message), progressBar);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        dialog.add(layout);
        return dialog;
    }

    public static void generateAndSend(String title, List<String> students) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            throw new IllegalStateException(
                    "UI.getCurrent() == null. Викликати generateAndSend треба з UI-потоку Vaadin (наприклад у кнопці)."
            );
        }

        try {
            // 1. Почистити та відсортувати студентів
            List<String> sortedStudents = prepareStudents(students);

            // 2. Генеруємо PDF як масив байтів
            byte[] pdfBytes = buildPdfBytes(title, sortedStudents);

            // 3. Формуємо ім'я файла
            String fileName = sanitizeFilename(title) + "-list.pdf";

            // 4. Створюємо StreamResource
            StreamResource resource = new StreamResource(
                    fileName,
                    (StreamResourceWriter) (outputStream, session) -> {
                        try (InputStream in = new ByteArrayInputStream(pdfBytes)) {
                            in.transferTo(outputStream);
                        } catch (IOException ioException) {
                            throw new UncheckedIOException(ioException);
                        }
                    }
            );

            resource.setContentType("application/pdf");
            resource.setCacheTime(0); // щоб завжди було свіже

            // 5. Реєструємо і відкриваємо у новій вкладці
            StreamRegistration registration = ui.getSession()
                    .getResourceRegistry()
                    .registerResource(resource);

            String resourceUrl = registration.getResourceUri().toString();
            ui.getPage().open(resourceUrl, "_blank");

        } catch (Exception e) {
            throw new RuntimeException("Не вдалося згенерувати або відкрити PDF", e);
        }
    }


    // ------------------ ВНУТРІШНІ ХЕЛПЕРИ ------------------ //

    /**
     * Чистить список від null/порожніх і сортує за українською абеткою.
     */
    private static List<String> prepareStudents(List<String> students) {
        List<String> cleaned = new ArrayList<>();
        for (String s : students) {
            if (s != null && !s.isBlank()) {
                cleaned.add(s.trim());
            }
        }

        Collator uaCollator = Collator.getInstance(new Locale("uk", "UA"));
        cleaned.sort(uaCollator);

        return cleaned;
    }

    /**
     * Створює PDF в оперативній пам'яті і повертає як масив байтів.
     * Нічого не зберігає на диск.
     */
    private static byte[] buildPdfBytes(String groupName, List<String> sortedStudents) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        pdfDoc.setDefaultPageSize(PageSize.A4);

        Document doc = new Document(pdfDoc);
        doc.setMargins(36, 36, 36, 36); // приблизно 1 см поля

        // 1. Підвантажити шрифт з classpath, щоб кирилиця не поламалась
        //    /fonts/DejaVuSans.ttf повинен бути у resources всередині твого jar
        PdfFont font;
        try (InputStream fontStream = CardView.class
                .getResourceAsStream("/fonts/times.ttf")) {

            if (fontStream == null) {
                throw new IllegalStateException("Не знайдено /fonts/DejaVuSans.ttf у resources.");
            }

            byte[] fontBytes = fontStream.readAllBytes();
            font = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H);
        }
        doc.setFont(font);

        // 2. Назва групи по центру, жирним, трішки більшим
        Paragraph titleP = new Paragraph(groupName)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4f);
        doc.add(titleP);

        // 3. Горизонтальна лінія під назвою
        LineSeparator line = new LineSeparator(new SolidLine(1f));
        line.setMarginBottom(16f);
        doc.add(line);

        // 4. Пронумерований список студентів
        int idx = 1;
        for (String student : sortedStudents) {
            Paragraph row = new Paragraph(idx + ". " + student)
                    .setFontSize(12)
                    .setMarginBottom(4f);
            doc.add(row);
            idx++;
        }

        doc.close(); // флашить усе у baos
        return baos.toByteArray();
    }

    /**
     * Робимо чисте ім'я файлу. Дозволяємо кирилицю, цифри, дефіс, підкреслення і крапку.
     */
    private static String sanitizeFilename(String in) {
        if (in == null || in.isBlank()) return "group";
        return in.replaceAll("[^a-zA-Z0-9\\u0400-\\u04FF\\-_.]", "_");
    }

    private void showUndoNotification(String message, List<Long> reportIds) {
        Notification notification = new Notification();
        notification.setDuration(5000);
        notification.setPosition(Notification.Position.TOP_CENTER);

        Span text = new Span(message);
        HorizontalLayout layout = new HorizontalLayout(text);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        if (reportIds != null && !reportIds.isEmpty()) {
            Button undoButton = new Button("Скасувати", event -> {
                reportService.deleteReportsByIds(reportIds);
                refreshReportsGrid();
                notification.close();
                Notification.show("Операцію скасовано.");
            });
            undoButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_TERTIARY_INLINE);
            layout.add(undoButton);
        }

        notification.add(layout);
        notification.open();
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
