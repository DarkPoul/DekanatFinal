package com.esvar.dekanat.card;

import com.esvar.dekanat.entity.StudentPassportEntity;
import com.esvar.dekanat.card.DatePickerUtil;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

/**
 * Layout for "Паспортна Інформація" tab.
 */
public class PassportInfoTab extends VerticalLayout {
    public TextField passportSeriesField = new TextField("Серія");
    public TextField passportNumberField = new TextField("Номер");
    public DatePicker passportIssueDatePicker = new DatePicker("Дата видачі");
    public DatePicker passportExpiryDatePicker = new DatePicker("Дата закінчення");
    public TextField passportIssuedByField = new TextField("Ким виданий");
    public TextField idCodeField = new TextField("Ідентифікаційний код");
    public TextField unzrField = new TextField("УНЗР");
    public DatePicker birthDatePicker = new DatePicker("Дата народження");
    public Select<String> nationalityField = new Select<>();
    public Select<String> genderSelect = new Select<>();
    public TextField personNumberEDEBOField = new TextField("Номер EDEBO фіз.");
    public TextField studentCardNumberEDEBOField = new TextField("Номер EDEBO здоб.");

    public PassportInfoTab() {
        setPadding(false);
        setWidthFull();

        nationalityField.setLabel("Національність");
        genderSelect.setLabel("Стать");

        FormLayout layout = new FormLayout();
        layout.add(passportSeriesField, passportNumberField, passportIssueDatePicker,
                passportExpiryDatePicker, passportIssuedByField, idCodeField,
                unzrField, birthDatePicker, nationalityField, genderSelect,
                personNumberEDEBOField, studentCardNumberEDEBOField);
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        add(layout);
        passportIssueDatePicker.setI18n(DatePickerUtil.ukrainian());
        passportExpiryDatePicker.setI18n(DatePickerUtil.ukrainian());
        birthDatePicker.setI18n(DatePickerUtil.ukrainian());
    }

    public void populate(StudentPassportEntity passport) {
        if (passport == null) {
            return;
        }
        passportSeriesField.setValue(nonNull(passport.getSeries()));
        passportNumberField.setValue(nonNull(passport.getNumber()));
        if (passport.getIssueDate() != null) {
            passportIssueDatePicker.setValue(passport.getIssueDate().toLocalDate());
        }
        if (passport.getExpireDate() != null) {
            passportExpiryDatePicker.setValue(passport.getExpireDate().toLocalDate());
        }
        passportIssuedByField.setValue(nonNull(passport.getIssuedBy()));
        idCodeField.setValue(nonNull(passport.getIdentificationNumber()));
        unzrField.setValue(nonNull(passport.getUnzrCode()));
        if (passport.getBirthdate() != null) {
            birthDatePicker.setValue(passport.getBirthdate().toLocalDate());
        }
        nationalityField.setValue(nonNull(passport.getNationality()));
        if (passport.getSex() != null) {
            genderSelect.setValue(passport.getSex().name());
        }
        personNumberEDEBOField.setValue(nonNull(passport.getEdboNumberPhis()));
        studentCardNumberEDEBOField.setValue(nonNull(passport.getEdboNumberZdob()));
    }

    public void setReadOnly(boolean readOnly) {
        passportSeriesField.setReadOnly(readOnly);
        passportNumberField.setReadOnly(readOnly);
        passportIssueDatePicker.setReadOnly(readOnly);
        passportExpiryDatePicker.setReadOnly(readOnly);
        passportIssuedByField.setReadOnly(readOnly);
        idCodeField.setReadOnly(readOnly);
        unzrField.setReadOnly(readOnly);
        birthDatePicker.setReadOnly(readOnly);
        nationalityField.setReadOnly(readOnly);
        genderSelect.setReadOnly(readOnly);
        personNumberEDEBOField.setReadOnly(readOnly);
        studentCardNumberEDEBOField.setReadOnly(readOnly);
    }

    private String nonNull(String value) {
        return value == null ? "" : value;
    }
}
