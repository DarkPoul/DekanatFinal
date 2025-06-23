package com.esvar.dekanat.card;

import com.esvar.dekanat.entity.StudentEducationEntity;
import com.esvar.dekanat.card.DatePickerUtil;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

/**
 * Layout for "Документи про освіту" tab.
 */
public class EducationDocumentsTab extends VerticalLayout {
    public Select<String> documentTypeSelect = new Select<>();
    public Checkbox distinctionCheckbox = new Checkbox("З відзнакою");
    public TextField documentSeriesField = new TextField("Серія документу");
    public TextField documentNumberField = new TextField("№ документу");
    public DatePicker documentIssueDatePicker = new DatePicker("Дата видачі");
    public TextField institutionNameField = new TextField("Назва навчального закладу");
    public TextField institutionNameEngField = new TextField("Назва навчального закладу (англ)");
    public TextField diplomaSeriesField = new TextField("Серія диплому");
    public TextField diplomaNumberField = new TextField("№ диплому");
    public DatePicker graduationDatePicker = new DatePicker("Дата закінчення");
    public TextField appendixNumberField = new TextField("№ додатку");
    public TextField thesisTitleUkrField = new TextField("Назва роботи");
    public TextField thesisTitleEngField = new TextField("Назва роботи (англ)");

    public EducationDocumentsTab() {
        setPadding(false);
        setWidthFull();

        documentTypeSelect.setLabel("Тип документу");

        FormLayout layout = new FormLayout();
        layout.add(documentTypeSelect, distinctionCheckbox, documentSeriesField,
                documentNumberField, documentIssueDatePicker, institutionNameField,
                institutionNameEngField, diplomaSeriesField, diplomaNumberField,
                graduationDatePicker, appendixNumberField,
                thesisTitleUkrField, thesisTitleEngField);
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        add(layout);
        documentIssueDatePicker.setI18n(DatePickerUtil.ukrainian());
        graduationDatePicker.setI18n(DatePickerUtil.ukrainian());
    }

    public void populate(StudentEducationEntity education) {
        if (education == null) {
            return;
        }
        documentTypeSelect.setValue(nonNull(education.getTypeOfDocument()));
        distinctionCheckbox.setValue(education.getHonors() == 1);
        documentSeriesField.setValue(nonNull(education.getSeries()));
        documentNumberField.setValue(nonNull(education.getNumber()));
        if (education.getDateOfIssue() != null) {
            documentIssueDatePicker.setValue(education.getDateOfIssue().toLocalDate());
        }
        institutionNameField.setValue(nonNull(education.getIssuedBy()));
        institutionNameEngField.setValue(nonNull(education.getIssuedByEng()));
        diplomaSeriesField.setValue(nonNull(education.getDiplomaSeries()));
        diplomaNumberField.setValue(nonNull(education.getDiplomaNumber()));
        if (education.getDateOfIssueDiploma() != null) {
            graduationDatePicker.setValue(education.getDateOfIssueDiploma().toLocalDate());
        }
        appendixNumberField.setValue(nonNull(education.getNumberOfDodatok()));
        thesisTitleUkrField.setValue(nonNull(education.getThemeOfWork()));
        thesisTitleEngField.setValue(nonNull(education.getThemeOfWorkEng()));
    }

    public void setReadOnly(boolean readOnly) {
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

    private String nonNull(String value) {
        return value == null ? "" : value;
    }
}
