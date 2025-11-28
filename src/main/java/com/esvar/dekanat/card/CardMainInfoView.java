package com.esvar.dekanat.card;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.div.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

/**
 * View responsible for the main (personal and academic) student data block.
 * It keeps only layout and styling concerns; CardView orchestrates data flow.
 */
@CssImport(value = "./styles/card-main-info-view.css", themeFor = "vaadin-text-field")
public class CardMainInfoView extends VerticalLayout {

    public CardMainInfoView(TextField lastNameUkrField,
                            TextField firstNameUkrField,
                            TextField middleNameUkrField,
                            TextField lastNameEngField,
                            TextField firstNameEngField,
                            Select<String> groupSelect,
                            Select<String> courseSelect,
                            TextField groupNumberField,
                            Select<String> admissionYearSelect,
                            TextField recordBookNumberField) {
        HorizontalLayout leftLayout1Page = new HorizontalLayout();
        HorizontalLayout rightLayout1Page = new HorizontalLayout();

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

        lastNameUkrField.setWidth("24%");
        firstNameUkrField.setWidth("24%");
        middleNameUkrField.setWidth("24%");
        lastNameEngField.setWidth("24%");
        firstNameEngField.setWidth("24%");
        groupSelect.setWidth("24%");
        courseSelect.setWidth("24%");
        groupNumberField.setWidth("24%");
        admissionYearSelect.setWidth("24%");
        recordBookNumberField.setWidth("24%");

        leftLayout1Page.add(lastNameUkrField, firstNameUkrField, middleNameUkrField, lastNameEngField, firstNameEngField);
        rightLayout1Page.add(groupSelect, courseSelect, groupNumberField, admissionYearSelect, recordBookNumberField);

        setWidth("100%");
        getStyle().set("padding", "0px");
        leftLayoutWrapper.getStyle().set("width", "97%");
        rightLayoutWrapper.getStyle().set("width", "97%");

        add(leftLayoutWrapper, rightLayoutWrapper);
    }
}
