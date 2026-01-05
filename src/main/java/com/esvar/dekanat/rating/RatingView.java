package com.esvar.dekanat.rating;

import com.esvar.dekanat.view.MainLayout;
import com.esvar.dekanat.service.RatingService;
import com.esvar.dekanat.dto.GroupDTO;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@PermitAll
@PageTitle("Рейтинг | Деканат")
@Route(value = "rating", layout = MainLayout.class)
public class RatingView extends Div {

    private final RatingService ratingService;
    private final List<GroupDTO> groups;

    private final Select<String> specialtySelect = new Select<>();
    private final Select<Integer> courseSelect = new Select<>();
    private final Select<Integer> groupSelect = new Select<>();

    private final Select<Integer> yearSelect = new Select<>();

    private final Checkbox technikumCheckbox = new Checkbox("Технікум");
    private final Checkbox budgetCheckbox = new Checkbox("Бюджет");
    private final Grid<RatingRow> ratingGrid = new Grid<>(RatingRow.class, false);

    public RatingView(RatingService ratingService) {
        this.ratingService = ratingService;
        this.groups = ratingService.getGroups();
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
        initializeDefaults();
        search();
    }

    private void configureFilters() {
        specialtySelect.setLabel("Спеціальність");
        specialtySelect.setItems(ratingService.getSpecialties());
        specialtySelect.setPlaceholder("Усі спеціальності");
        specialtySelect.setEmptySelectionAllowed(true);
        specialtySelect.addValueChangeListener(e -> {
            updateCourseOptions();
            updateGroupOptions();
            updateYearOptions();
            search();
        });

        courseSelect.setLabel("Курс");
        courseSelect.setItems(ratingService.getCourses());
        courseSelect.setPlaceholder("Усі курси");
        courseSelect.setEmptySelectionAllowed(true);
        courseSelect.addValueChangeListener(e -> {
            updateGroupOptions();
            updateYearOptions();
            search();
        });

        groupSelect.setLabel("Група");
        groupSelect.setItems(ratingService.getGroupNumbers());
        groupSelect.setPlaceholder("Усі групи");
        groupSelect.setEmptySelectionAllowed(true);
        groupSelect.addValueChangeListener(e -> {
            updateYearOptions();
            search();
        });

        yearSelect.setLabel("Рік");
        yearSelect.setItems(ratingService.getYears());
        yearSelect.setPlaceholder("Оберіть рік");
        yearSelect.setEmptySelectionAllowed(true);
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
        ratingGrid.setWidthFull();
    }

    private void initializeDefaults() {
        updateCourseOptions();
        updateGroupOptions();
        updateYearOptions();
        if (yearSelect.getValue() == null && !yearSelect.getListDataView().getItems().toList().isEmpty()) {
            yearSelect.setValue(yearSelect.getListDataView().getItems().findFirst().orElse(null));
        }
    }

    private void updateCourseOptions() {
        List<Integer> availableCourses = filterGroups(specialtySelect.getValue(), null, null).stream()
                .map(GroupDTO::getCourse)
                .distinct()
                .sorted()
                .toList();
        courseSelect.setItems(availableCourses);
        if (courseSelect.getValue() != null && !availableCourses.contains(courseSelect.getValue())) {
            courseSelect.clear();
        }
    }

    private void updateGroupOptions() {
        List<Integer> availableGroups = filterGroups(
                specialtySelect.getValue(),
                courseSelect.getValue(),
                null
        ).stream()
                .map(GroupDTO::getGroupNumber)
                .distinct()
                .sorted()
                .toList();
        groupSelect.setItems(availableGroups);
        if (groupSelect.getValue() != null && !availableGroups.contains(groupSelect.getValue())) {
            groupSelect.clear();
        }
    }

    private void updateYearOptions() {
        List<Integer> availableYears = filterGroups(
                specialtySelect.getValue(),
                courseSelect.getValue(),
                groupSelect.getValue()
        ).stream()
                .map(GroupDTO::getYear)
                .distinct()
                .sorted((a, b) -> b.compareTo(a))
                .toList();
        yearSelect.setItems(availableYears);
        if (yearSelect.getValue() != null && !availableYears.contains(yearSelect.getValue())) {
            yearSelect.clear();
        }
        if (yearSelect.getValue() == null && !availableYears.isEmpty()) {
            yearSelect.setValue(availableYears.get(0));
        }
    }

    private List<GroupDTO> filterGroups(String specialty, Integer course, Integer groupNumber) {
        return groups.stream()
                .filter(group -> specialty == null || Objects.equals(group.getSpecialtyAbbreviation(), specialty))
                .filter(group -> course == null || group.getCourse() == course)
                .filter(group -> groupNumber == null || group.getGroupNumber() == groupNumber)
                .collect(Collectors.toList());
    }

    private void search() {
        List<RatingRow> rows = ratingService.searchRatings(
                        specialtySelect.getValue(),
                        courseSelect.getValue(),
                        groupSelect.getValue(),
                        yearSelect.getValue(),
                        technikumCheckbox.getValue(),
                        budgetCheckbox.getValue(),
                        null, // Pageable (буде використано значення за замовчуванням у сервісі)
                        null  // Sort (буде використано значення за замовчуванням у сервісі)
                ).getContent().stream() // Викликаємо getContent(), щоб отримати список з Page
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
