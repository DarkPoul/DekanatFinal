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

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        ratingGrid.addColumn(RatingRow::group).setHeader("Група");
        ratingGrid.addColumn(RatingRow::student).setHeader("Студент");
        ratingGrid.addColumn(RatingRow::average).setHeader("Середній бал");
        ratingGrid.addColumn(RatingRow::count5).setHeader("Кількість 5");
        ratingGrid.addColumn(RatingRow::percent5).setHeader("% 5");
        ratingGrid.addColumn(RatingRow::count4).setHeader("Кількість 4");
        ratingGrid.addColumn(RatingRow::percent4).setHeader("% 4");
        ratingGrid.addColumn(RatingRow::count3).setHeader("Кількість 3");
        ratingGrid.addColumn(RatingRow::percent3).setHeader("% 3");
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
                .map(entity -> {
                    BigDecimal avg = entity.getAverageScore();
                    int total = entity.getTotalSubjects();
                    String perc5 = formatPercent(entity.getCount5(), total);
                    String perc4 = formatPercent(entity.getCount4(), total);
                    String perc3 = formatPercent(entity.getCount3(), total);
                    return new RatingRow(
                            entity.getStudent().getFullName(),
                            entity.getGroup().getGroupCode(),
                            avg.setScale(2, RoundingMode.HALF_UP).toString(),
                            entity.getCount5(),
                            perc5,
                            entity.getCount4(),
                            perc4,
                            entity.getCount3(),
                            perc3
                    );
                })
                .toList();
        ratingGrid.setItems(rows);

    }

    private String formatPercent(int count, int total) {
        if (total == 0) {
            return "0";
        }
        BigDecimal percent = new BigDecimal(count * 100.0 / total);
        return percent.setScale(2, RoundingMode.HALF_UP).toString();
    }

    private record RatingRow(
            String student,
            String group,
            String average,
            int count5,
            String percent5,
            int count4,
            String percent4,
            int count3,
            String percent3
    ) {
    }
}

