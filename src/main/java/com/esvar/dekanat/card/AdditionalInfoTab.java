package com.esvar.dekanat.card;

import com.esvar.dekanat.entity.StudentInfoEntity;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;

/**
 * Layout for "Додаткова Інформація" tab.
 */
public class AdditionalInfoTab extends VerticalLayout {
    public TextField caseNumberField = new TextField("Номер справи");
    public TextField idCodeField = new TextField("Ідентифікаційний код");
    public TextField unzrField = new TextField("УНЗР");
    public Select<String> nationalityField = new Select<>();

    public Select<String> educationFormSelect = new Select<>();
    public Select<String> degreeSelect = new Select<>();
    public Select<String> admissionConditionSelect = new Select<>();
    public Select<String> paymentSourceSelect = new Select<>();
    public TextField contractNumberField = new TextField("Договір за номером");
    public TextField amountField = new TextField("Сума");
    public Checkbox distinctionCheckbox = new Checkbox("З відзнакою");
    public TextField phoneNumberField = new TextField("Номер телефону");
    public TextField emailField = new TextField("E-mail");
    public Select<String> regionSelect = new Select<>();
    public TextField indexField = new TextField("Індекс");
    public TextField fullAddressField = new TextField("Повна адреса");
    public MultiSelectComboBox<String> benefitsSelect = new MultiSelectComboBox<>();

    public AdditionalInfoTab() {
        setPadding(false);
        setWidthFull();

        educationFormSelect.setLabel("Форма навчання");
        degreeSelect.setLabel("Здобуття звання");
        admissionConditionSelect.setLabel("Умови вступу");
        paymentSourceSelect.setLabel("Тип особи");
        regionSelect.setLabel("Область");
        benefitsSelect.setLabel("Пільги");

        FormLayout layout = new FormLayout();
        layout.add(caseNumberField, idCodeField, unzrField, nationalityField,
                educationFormSelect, degreeSelect, admissionConditionSelect,
                paymentSourceSelect, contractNumberField, amountField,
                phoneNumberField, emailField, regionSelect, indexField,
                fullAddressField, benefitsSelect, distinctionCheckbox);
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        
        add(layout);
    }

    public void populate(StudentInfoEntity info) {
        if (info == null) {
            return;
        }
        caseNumberField.setValue(nonNull(info.getCaseNumber()));
        educationFormSelect.setValue(nonNull(info.getFormStudy()));
        degreeSelect.setValue(nonNull(info.getDegree()));
        admissionConditionSelect.setValue(nonNull(info.getEntryRequirements()));
        paymentSourceSelect.setValue(nonNull(info.getTypeOfIndividual()));
        contractNumberField.setValue(nonNull(info.getContractNumber()));
        amountField.setValue(nonNull(info.getTotal()));
        phoneNumberField.setValue(nonNull(info.getPhone()));
        emailField.setValue(nonNull(info.getEmail()));
        regionSelect.setValue(nonNull(info.getRegion()));
        indexField.setValue(nonNull(info.getIndex()));
        fullAddressField.setValue(nonNull(info.getAddress()));
        if (info.getBenefits() != null) {
            benefitsSelect.setValue(List.of(info.getBenefits().split(", ")));        
        }
    }

    public void setReadOnly(boolean readOnly) {
        caseNumberField.setReadOnly(readOnly);
        idCodeField.setReadOnly(readOnly);
        unzrField.setReadOnly(readOnly);
        nationalityField.setReadOnly(readOnly);
        educationFormSelect.setReadOnly(readOnly);
        degreeSelect.setReadOnly(readOnly);
        admissionConditionSelect.setReadOnly(readOnly);
        paymentSourceSelect.setReadOnly(readOnly);
        contractNumberField.setReadOnly(readOnly);
        amountField.setReadOnly(readOnly);
        phoneNumberField.setReadOnly(readOnly);
        emailField.setReadOnly(readOnly);
        regionSelect.setReadOnly(readOnly);
        indexField.setReadOnly(readOnly);
        fullAddressField.setReadOnly(readOnly);
        benefitsSelect.setReadOnly(readOnly);
        distinctionCheckbox.setReadOnly(readOnly);
    }

    private String nonNull(String value) {
        return value == null ? "" : value;
    }
}
