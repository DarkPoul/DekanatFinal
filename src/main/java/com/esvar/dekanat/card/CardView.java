package com.esvar.dekanat.card;

import com.esvar.dekanat.entity.StudentEducationEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentInfoEntity;
import com.esvar.dekanat.entity.StudentPassportEntity;
import com.esvar.dekanat.view.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

/**
 * Main view that contains tabs with different student card sections.
 */
@PageTitle("Перегляд карток | Деканат")
@Route(value = "card", layout = MainLayout.class)
@PermitAll
public class CardView extends Div {

    private final Tabs tabs = new Tabs();
    private final VerticalLayout content = new VerticalLayout();

    private final Tab mainInfoTab = new Tab("Основна Інформація");
    private final Tab passportInfoTab = new Tab("Паспортна Інформація");
    private final Tab additionalInfoTab = new Tab("Додаткова Інформація");
    private final Tab educationDocumentsTab = new Tab("Документи про освіту");

    private final MainInfoTab mainInfo = new MainInfoTab();
    private final PassportInfoTab passportInfo = new PassportInfoTab();
    private final AdditionalInfoTab additionalInfo = new AdditionalInfoTab();
    private final EducationDocumentsTab educationInfo = new EducationDocumentsTab();

    public CardView() {
        tabs.add(mainInfoTab, passportInfoTab, additionalInfoTab, educationDocumentsTab);
        tabs.addSelectedChangeListener(e -> updateContent());

        content.setPadding(false);
        add(tabs, content);
        setHeightFull();
        updateContent();
    }

    /**
     * Fill tab contents using provided entities.
     */
    public void populate(StudentEntity student,
                         StudentPassportEntity passport,
                         StudentInfoEntity info,
                         StudentEducationEntity education) {
        mainInfo.populate(student, passport);
        passportInfo.populate(passport);
        additionalInfo.populate(info);
        educationInfo.populate(education);
    }

    /**
     * Toggle read only state for every tab.
     */
    public void setReadOnly(boolean readOnly) {
        mainInfo.setReadOnly(readOnly);
        passportInfo.setReadOnly(readOnly);
        additionalInfo.setReadOnly(readOnly);
        educationInfo.setReadOnly(readOnly);
    }

    private void updateContent() {
        content.removeAll();
        if (tabs.getSelectedTab() == passportInfoTab) {
            content.add(passportInfo);
        } else if (tabs.getSelectedTab() == additionalInfoTab) {
            content.add(additionalInfo);
        } else if (tabs.getSelectedTab() == educationDocumentsTab) {
            content.add(educationInfo);
        } else {
            content.add(mainInfo);
        }
    }
}
