package com.esvar.dekanat.card;

import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentPassportEntity;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

/**
 * Layout for "Основна Інформація" tab.
 */
public class MainInfoTab extends VerticalLayout {

    public TextField lastNameUkrField = new TextField("Прізвище");
    public TextField firstNameUkrField = new TextField("Ім'я");
    public TextField middleNameUkrField = new TextField("По батькові");
    public TextField lastNameEngField = new TextField("Прізвище (англ)");
    public TextField firstNameEngField = new TextField("Ім'я (англ)");

    public Select<String> groupSelect = new Select<>();
    public Select<String> courseSelect = new Select<>();
    public TextField groupNumberField = new TextField("Номер групи");
    public Select<String> admissionYearSelect = new Select<>();
    public TextField recordBookNumberField = new TextField("Номер заліковки");

    public MainInfoTab() {
        setPadding(false);
        setWidthFull();

        groupSelect.setLabel("Група");
        courseSelect.setLabel("Курс");
        admissionYearSelect.setLabel("Рік вступу");

        // Personal data section
        Div personalWrapper = createWrapper("Персональні дані");
        FormLayout personalLayout = new FormLayout();
        personalLayout.add(lastNameUkrField, firstNameUkrField, middleNameUkrField,
                lastNameEngField, firstNameEngField);
        personalLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2));
        personalWrapper.add(personalLayout);

        // Academic data section
        Div academicWrapper = createWrapper("Академічні дані");
        FormLayout academicLayout = new FormLayout();
        academicLayout.add(groupSelect, courseSelect, groupNumberField,
                admissionYearSelect, recordBookNumberField);
        academicLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2));
        academicWrapper.add(academicLayout);

        add(personalWrapper, academicWrapper);
    }

    /**
     * Populate fields with data from student entities.
     */
    public void populate(StudentEntity student, StudentPassportEntity passport) {
        if (student != null) {
            lastNameUkrField.setValue(nonNull(student.getSurname()));
            firstNameUkrField.setValue(nonNull(student.getName()));
            middleNameUkrField.setValue(nonNull(student.getPatronymic()));
            if (student.getGroup() != null) {
                groupSelect.setValue(nonNull(student.getGroup().getSpecialty().getAbbreviation()));
                courseSelect.setValue(String.valueOf(student.getGroup().getCourse()));
                groupNumberField.setValue(String.valueOf(student.getGroup().getGroupNumber()));
                admissionYearSelect.setValue(String.valueOf(student.getGroup().getYear()));
            }
            recordBookNumberField.setValue(nonNull(student.getRecordBookNumber()));
        }
        if (passport != null) {
            lastNameEngField.setValue(nonNull(passport.getSurnameEng()));
            firstNameEngField.setValue(nonNull(passport.getNameEng()));
        }
    }

    /**
     * Toggle read only mode for all fields.
     */
    public void setReadOnly(boolean readOnly) {
        lastNameUkrField.setReadOnly(readOnly);
        firstNameUkrField.setReadOnly(readOnly);
        middleNameUkrField.setReadOnly(readOnly);
        lastNameEngField.setReadOnly(readOnly);
        firstNameEngField.setReadOnly(readOnly);
        groupSelect.setReadOnly(readOnly);
        courseSelect.setReadOnly(readOnly);
        groupNumberField.setReadOnly(readOnly);
        admissionYearSelect.setReadOnly(readOnly);
        recordBookNumberField.setReadOnly(readOnly);
    }

    private Div createWrapper(String title) {
        Div wrapper = new Div();
        wrapper.getStyle().set("border", "1px solid #ddd");
        wrapper.getStyle().set("border-radius", "8px");
        wrapper.getStyle().set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");
        wrapper.getStyle().set("padding", "20px");
        wrapper.getStyle().set("position", "relative");
        wrapper.getStyle().set("background", "white");

        Span caption = new Span(title);
        caption.getStyle().set("position", "absolute");
        caption.getStyle().set("top", "-10px");
        caption.getStyle().set("left", "20px");
        caption.getStyle().set("background", "white");
        caption.getStyle().set("padding", "0 10px");
        caption.getStyle().set("font-weight", "bold");

        wrapper.add(caption);
        return wrapper;
    }

    private String nonNull(String value) {
        return value == null ? "" : value;
    }
}
