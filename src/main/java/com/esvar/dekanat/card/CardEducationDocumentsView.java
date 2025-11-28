package com.esvar.dekanat.card;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.div.Div;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

/**
 * View responsible for prior education documents and diploma data blocks.
 */
public class CardEducationDocumentsView extends VerticalLayout {

    private final Div generalEducationDocumentsWrapper;
    private final Div diplomaDocumentsWrapper;

    public CardEducationDocumentsView(Select<String> documentTypeSelect,
                                      Checkbox distinctionCheckbox,
                                      TextField documentSeriesField,
                                      TextField documentNumberField,
                                      DatePicker documentIssueDatePicker,
                                      TextField institutionNameField,
                                      TextField institutionNameEngField,
                                      TextField diplomaSeriesField,
                                      TextField diplomaNumberField,
                                      DatePicker graduationDatePicker,
                                      TextField appendixNumberField,
                                      TextField thesisTitleUkrField,
                                      TextField thesisTitleEngField) {

        generalEducationDocumentsWrapper = new Div();
        generalEducationDocumentsWrapper.getStyle().set("border", "1px solid #ddd");
        generalEducationDocumentsWrapper.getStyle().set("border-radius", "8px");
        generalEducationDocumentsWrapper.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        generalEducationDocumentsWrapper.getStyle().set("padding", "20px");
        generalEducationDocumentsWrapper.getStyle().set("position", "relative");
        generalEducationDocumentsWrapper.getStyle().set("background", "white");
        generalEducationDocumentsWrapper.getStyle().set("width", "97%");

        Span generalEducationDocumentsTitle = new Span("Попередня освіта");
        generalEducationDocumentsTitle.getStyle().set("position", "absolute");
        generalEducationDocumentsTitle.getStyle().set("top", "-10px");
        generalEducationDocumentsTitle.getStyle().set("left", "20px");
        generalEducationDocumentsTitle.getStyle().set("background", "white");
        generalEducationDocumentsTitle.getStyle().set("padding", "0 10px");
        generalEducationDocumentsTitle.getStyle().set("font-weight", "bold");

        FormLayout generalEducationDocumentsLayout = new FormLayout();
        generalEducationDocumentsLayout.add(
                documentTypeSelect,
                distinctionCheckbox,
                documentSeriesField,
                documentNumberField,
                documentIssueDatePicker,
                institutionNameField,
                institutionNameEngField
        );

        generalEducationDocumentsLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 2),
                new FormLayout.ResponsiveStep("600px", 2)
        );
        generalEducationDocumentsLayout.setColspan(documentTypeSelect, 1);
        generalEducationDocumentsLayout.setColspan(distinctionCheckbox, 1);
        generalEducationDocumentsLayout.setColspan(institutionNameField, 2);
        generalEducationDocumentsLayout.setColspan(institutionNameEngField, 2);

        generalEducationDocumentsWrapper.add(generalEducationDocumentsTitle, generalEducationDocumentsLayout);

        diplomaDocumentsWrapper = new Div();
        diplomaDocumentsWrapper.getStyle().set("border", "1px solid #ddd");
        diplomaDocumentsWrapper.getStyle().set("border-radius", "8px");
        diplomaDocumentsWrapper.getStyle().set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        diplomaDocumentsWrapper.getStyle().set("padding", "20px");
        diplomaDocumentsWrapper.getStyle().set("position", "relative");
        diplomaDocumentsWrapper.getStyle().set("background", "white");
        diplomaDocumentsWrapper.getStyle().set("width", "97%");

        Span diplomaSectionTitle = new Span("Диплом");
        diplomaSectionTitle.getStyle().set("position", "absolute");
        diplomaSectionTitle.getStyle().set("top", "-10px");
        diplomaSectionTitle.getStyle().set("left", "20px");
        diplomaSectionTitle.getStyle().set("background", "white");
        diplomaSectionTitle.getStyle().set("padding", "0 10px");
        diplomaSectionTitle.getStyle().set("font-weight", "bold");

        HorizontalLayout diplomaLayout = new HorizontalLayout();
        diplomaLayout.setWidthFull();
        diplomaLayout.setSpacing(true);
        diplomaLayout.add(diplomaSeriesField, diplomaNumberField, graduationDatePicker);
        diplomaLayout.setFlexGrow(1, diplomaSeriesField, diplomaNumberField, graduationDatePicker);

        FormLayout diplomaDocumentsLayout = new FormLayout();
        diplomaDocumentsLayout.add(
                diplomaLayout,
                appendixNumberField,
                thesisTitleUkrField,
                thesisTitleEngField
        );
        diplomaDocumentsLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 1)
        );

        diplomaDocumentsWrapper.add(diplomaSectionTitle, diplomaDocumentsLayout);

        setWidth("100%");
        getStyle().set("padding", "0px");
        add(generalEducationDocumentsWrapper, diplomaDocumentsWrapper);
    }

    public Div getGeneralEducationDocumentsWrapper() {
        return generalEducationDocumentsWrapper;
    }

    public Div getDiplomaDocumentsWrapper() {
        return diplomaDocumentsWrapper;
    }
}
