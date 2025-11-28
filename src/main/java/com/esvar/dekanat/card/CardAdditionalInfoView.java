package com.esvar.dekanat.card;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.div.Div;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

/**
 * View responsible for additional student info: education terms, contacts, and address.
 */
public class CardAdditionalInfoView extends VerticalLayout {

    public CardAdditionalInfoView(TextField caseNumberField,
                                  Select<String> educationFormSelect,
                                  Select<String> degreeSelect,
                                  Select<String> admissionConditionSelect,
                                  Select<String> paymentSourceSelect,
                                  TextField contractNumberField,
                                  TextField amountField,
                                  MultiSelectComboBox<String> benefitsSelect,
                                  Select<String> regionSelect,
                                  TextField indexField,
                                  TextField fullAddressField,
                                  TextField phoneNumberField,
                                  TextField emailField) {

        Div educationDetailsWrapper = new Div();
        educationDetailsWrapper.getStyle().set("border", "1px solid #ddd");
        educationDetailsWrapper.getStyle().set("border-radius", "8px");
        educationDetailsWrapper.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        educationDetailsWrapper.getStyle().set("padding", "20px");
        educationDetailsWrapper.getStyle().set("position", "relative");
        educationDetailsWrapper.getStyle().set("background", "white");
        educationDetailsWrapper.getStyle().set("width", "97%");

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

        Div contactDetailsWrapper = new Div();
        contactDetailsWrapper.getStyle().set("border", "1px solid #ddd");
        contactDetailsWrapper.getStyle().set("border-radius", "8px");
        contactDetailsWrapper.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        contactDetailsWrapper.getStyle().set("padding", "20px");
        contactDetailsWrapper.getStyle().set("position", "relative");
        contactDetailsWrapper.getStyle().set("background", "white");
        contactDetailsWrapper.getStyle().set("width", "97%");

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

        Div addressDetailsWrapper = new Div();
        addressDetailsWrapper.getStyle().set("border", "1px solid #ddd");
        addressDetailsWrapper.getStyle().set("border-radius", "8px");
        addressDetailsWrapper.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        addressDetailsWrapper.getStyle().set("padding", "20px");
        addressDetailsWrapper.getStyle().set("position", "relative");
        addressDetailsWrapper.getStyle().set("background", "white");
        addressDetailsWrapper.getStyle().set("width", "97%");

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
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        addressDetailsLayout.setColspan(fullAddressField, 2);

        addressDetailsWrapper.add(addressDetailsTitle, addressDetailsLayout);

        setWidth("100%");
        getStyle().set("padding", "0px");
        add(educationDetailsWrapper, contactDetailsWrapper, addressDetailsWrapper);
    }
}
