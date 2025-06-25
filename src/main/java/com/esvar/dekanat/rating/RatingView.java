package com.esvar.dekanat.rating;

import com.esvar.dekanat.view.MainLayout;
import com.esvar.dekanat.service.RatingService;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@PermitAll
@PageTitle("Рейтинг | Деканат")
@Route(value = "rating", layout = MainLayout.class)
public class RatingView extends Div {

    private final RatingService ratingService;

    private final Select<String> specialtySelect = new Select<>();
    private final Select<String> courseSelect = new Select<>();
    private final Select<String> groupSelect = new Select<>();

    private final Select<String> yearSelect = new Select<>();

    private final Checkbox technikumCheckbox = new Checkbox("Технікум");
    private final Checkbox budgetCheckbox = new Checkbox("Бюджет");
    private final Grid<RatingRow> ratingGrid = new Grid<>(RatingRow.class, false);

    public RatingView(RatingService ratingService) {
        this.ratingService = ratingService;
        configureFilters();
        configureGrid();
        VerticalLayout checkboxColumn = new VerticalLayout(technikumCheckbox, budgetCheckbox);
        checkboxColumn.setSpacing(false);
        checkboxColumn.setPadding(false);

        HorizontalLayout filters = new HorizontalLayout(
                specialtySelect,
                courseSelect,
                groupSelect,
                yearSelect,
                checkboxColumn
        );
        filters.setPadding(true);
        filters.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.START);
        filters.setVerticalComponentAlignment(FlexComponent.Alignment.END, checkboxColumn);

        VerticalLayout layout = new VerticalLayout(new H2("Сторінка рейтингу"), filters, ratingGrid);
        layout.setPadding(false);
        add(layout);
    }

    private void configureFilters() {
        specialtySelect.setLabel("Спеціальність");
        specialtySelect.setItems(ratingService.getSpecialties());
        specialtySelect.addValueChangeListener(e -> search());

        courseSelect.setLabel("Курс");
        courseSelect.setItems(ratingService.getCourses());
        courseSelect.addValueChangeListener(e -> search());

        groupSelect.setLabel("Група");
        groupSelect.setItems(ratingService.getGroupCodes());
        groupSelect.addValueChangeListener(e -> search());

        yearSelect.setLabel("Рік");
        yearSelect.setItems(ratingService.getYears());
        yearSelect.addValueChangeListener(e -> search());

        technikumCheckbox.addValueChangeListener(e -> search());
        budgetCheckbox.addValueChangeListener(e -> search());
    }

    private void configureGrid() {
        ratingGrid.addColumn(RatingRow::student).setHeader("Студент");
        ratingGrid.addColumn(RatingRow::rating).setHeader("Рейтинг");
        ratingGrid.addColumn(RatingRow::student).setHeader("Група");
        ratingGrid.addColumn(RatingRow::rating).setHeader("Прізвище");
        ratingGrid.addColumn(RatingRow::rating).setHeader("Кількість 5");
        ratingGrid.addColumn(RatingRow::rating).setHeader("% 5");
        ratingGrid.addColumn(RatingRow::rating).setHeader("Кількість 4");
        ratingGrid.addColumn(RatingRow::rating).setHeader("% 4");
        ratingGrid.addColumn(RatingRow::rating).setHeader("Кількість 3");
        ratingGrid.addColumn(RatingRow::rating).setHeader("% 3");
        ratingGrid.addColumn(RatingRow::rating).setHeader("Загальний бал");
        ratingGrid.setItems(new ArrayList<>());
        ratingGrid.setWidthFull();
    }

    private void search() {
        List<RatingRow> rows = ratingService.searchRatings(
                specialtySelect.getValue(),
                courseSelect.getValue(),
                groupSelect.getValue(),
                yearSelect.getValue(),
                technikumCheckbox.getValue(),
                budgetCheckbox.getValue()
        ).stream()
                .map(entity -> new RatingRow(
                        entity.getStudent().getFullName(),
                        entity.getAverageScore().toString()
                ))
                .toList();
        ratingGrid.setItems(rows);

    }

    private record RatingRow(String student, String rating) {
    }
}

