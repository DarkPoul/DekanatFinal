package com.esvar.dekanat.card;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.div.Div;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

/**
 * View responsible for the passport-related data block.
 */
public class CardPassportInfoView extends VerticalLayout {

    public CardPassportInfoView(TextField passportSeriesField,
                                TextField passportNumberField,
                                DatePicker passportIssueDatePicker,
                                DatePicker passportExpiryDatePicker,
                                TextField passportIssuedByField,
                                TextField idCodeField,
                                TextField unzrField,
                                DatePicker birthDatePicker,
                                Select<String> nationalityField,
                                Select<String> genderSelect,
                                TextField personNumberEDEBOField,
                                TextField studentCardNumberEDEBOField) {

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

        setWidth("100%");
        getStyle().set("padding", "0px");
        add(passportDetailsWrapper);
    }
}
