package com.esvar.dekanat.rating;

import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.dto.RatingFilterOptions;
import com.esvar.dekanat.entity.StudentRatingEntity;
import com.esvar.dekanat.service.RatingService;
import com.esvar.dekanat.view.MainLayout;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@PermitAll
@PageTitle("Рейтинг | Деканат")
@Route(value = "rating", layout = MainLayout.class)
public class RatingView extends Div {

    private final RatingService ratingService;
    private final RatingFilterOptions filterOptions;
    private final List<GroupDTO> groups;
    private final NumberFormat numberFormat;
    private final CallbackDataProvider<RatingRow, Void> dataProvider;

    private final Select<String> specialtySelect = new Select<>();
    private final Select<Integer> courseSelect = new Select<>();
    private final Select<Integer> groupSelect = new Select<>();

    private final Select<Integer> yearSelect = new Select<>();

    private final Checkbox technikumCheckbox = new Checkbox("Технікум");
    private final Checkbox budgetCheckbox = new Checkbox("Бюджет");
    private final Grid<RatingRow> ratingGrid = new Grid<>(RatingRow.class, false);
    private boolean suppressRefresh;

    public RatingView(RatingService ratingService) {
        this.ratingService = ratingService;
        this.filterOptions = ratingService.getFilterOptions();
        this.groups = filterOptions.groups();
        this.numberFormat = NumberFormat.getNumberInstance(new Locale("uk", "UA"));
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(2);
        this.dataProvider = DataProvider.fromCallbacks(this::fetchRows, this::countRows);
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
        specialtySelect.setItems(filterOptions.specialties());
        specialtySelect.setPlaceholder("Усі спеціальності");
        specialtySelect.setEmptySelectionAllowed(true);
        specialtySelect.addValueChangeListener(e -> {
            updateCourseOptions();
            updateGroupOptions();
            updateYearOptions();
            search();
        });

        courseSelect.setLabel("Курс");
        courseSelect.setItems(filterOptions.courses());
        courseSelect.setPlaceholder("Усі курси");
        courseSelect.setEmptySelectionAllowed(true);
        courseSelect.addValueChangeListener(e -> {
            updateGroupOptions();
            updateYearOptions();
            search();
        });

        groupSelect.setLabel("Група");
        groupSelect.setItems(filterOptions.groupNumbers());
        groupSelect.setPlaceholder("Усі групи");
        groupSelect.setEmptySelectionAllowed(true);
        groupSelect.addValueChangeListener(e -> {
            updateYearOptions();
            search();
        });

        yearSelect.setLabel("Рік");
        yearSelect.setItems(filterOptions.years());
        yearSelect.setPlaceholder("Оберіть рік");
        yearSelect.setEmptySelectionAllowed(true);
        yearSelect.addValueChangeListener(e -> search());

        technikumCheckbox.addValueChangeListener(e -> search());
        budgetCheckbox.addValueChangeListener(e -> search());
    }

    private void configureGrid() {
        ratingGrid.setMultiSort(true);
        ratingGrid.setMultiSortPriority(Grid.MultiSortPriority.APPEND);

        ratingGrid.addColumn(RatingRow::group)
                .setHeader("Група")
                .setKey("group")
                .setSortable(true);
        ratingGrid.addColumn(RatingRow::student)
                .setHeader("Студент")
                .setKey("student")
                .setSortable(true);
        ratingGrid.addColumn(row -> formatAverage(row.average()))
                .setHeader("Середній бал")
                .setKey("average")
                .setSortable(true);
        ratingGrid.addColumn(RatingRow::count5).setHeader("Кількість 5").setAutoWidth(true);
        ratingGrid.addColumn(new ComponentRenderer<>(row -> percentageBadge(row.count5(), row.total())))
                .setHeader("% 5")
                .setAutoWidth(true);
        ratingGrid.addColumn(RatingRow::count4).setHeader("Кількість 4").setAutoWidth(true);
        ratingGrid.addColumn(new ComponentRenderer<>(row -> percentageBadge(row.count4(), row.total())))
                .setHeader("% 4")
                .setAutoWidth(true);
        ratingGrid.addColumn(RatingRow::count3).setHeader("Кількість 3").setAutoWidth(true);
        ratingGrid.addColumn(new ComponentRenderer<>(row -> percentageBadge(row.count3(), row.total())))
                .setHeader("% 3")
                .setAutoWidth(true);
        ratingGrid.setWidthFull();
        ratingGrid.setPageSize(30);
        ratingGrid.setDataProvider(dataProvider);
    }

    private void initializeDefaults() {
        suppressRefresh = true;
        updateCourseOptions();
        updateGroupOptions();
        updateYearOptions();

        if (filterOptions.defaultSpecialty() != null) {
            specialtySelect.setValue(filterOptions.defaultSpecialty());
        }
        updateCourseOptions();
        if (filterOptions.defaultCourse() != null) {
            courseSelect.setValue(filterOptions.defaultCourse());
        }
        updateGroupOptions();
        if (filterOptions.defaultGroupNumber() != null) {
            groupSelect.setValue(filterOptions.defaultGroupNumber());
        }
        updateYearOptions();
        if (filterOptions.defaultYear() != null) {
            yearSelect.setValue(filterOptions.defaultYear());
        } else if (yearSelect.getValue() == null && !yearSelect.getListDataView().getItems().isEmpty()) {
            yearSelect.setValue(yearSelect.getListDataView().getItems().findFirst().orElse(null));
        }
        suppressRefresh = false;
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
        if (suppressRefresh) {
            return;
        }
        ratingGrid.getDataProvider().refreshAll();
    }

    private Stream<RatingRow> fetchRows(Query<RatingRow, Void> query) {
        Sort sort = resolveSort(query);
        int limit = query.getLimit();
        int page = limit == 0 ? 0 : query.getOffset() / limit;
        Pageable pageable = PageRequest.of(page, Math.max(limit, 1));
        Page<StudentRatingEntity> pageResult = ratingService.searchRatings(
                specialtySelect.getValue(),
                courseSelect.getValue(),
                groupSelect.getValue(),
                yearSelect.getValue(),
                technikumCheckbox.getValue(),
                budgetCheckbox.getValue(),
                pageable,
                sort
        );
        return pageResult.stream().map(this::toRow);
    }

    private int countRows(Query<RatingRow, Void> query) {
        return Math.toIntExact(ratingService.countRatings(
                specialtySelect.getValue(),
                courseSelect.getValue(),
                groupSelect.getValue(),
                yearSelect.getValue(),
                technikumCheckbox.getValue(),
                budgetCheckbox.getValue()
        ));
    }

    private RatingRow toRow(StudentRatingEntity entity) {
        return new RatingRow(
                entity.getStudent().getFullName(),
                entity.getGroup().getGroupCode(),
                entity.getAverageScore(),
                entity.getCount5(),
                entity.getCount4(),
                entity.getCount3(),
                entity.getTotalSubjects()
        );
    }

    private Sort resolveSort(Query<RatingRow, Void> query) {
        Map<String, String> propertyMapping = Map.of(
                "group", "group.groupCode",
                "average", "averageScore",
                "student", "student.surname"
        );
        Sort sort = Sort.unsorted();
        for (QuerySortOrder order : query.getSortOrders()) {
            String property = propertyMapping.get(order.getSorted());
            if (property == null) {
                continue;
            }
            Sort.Order sortOrder = order.getDirection() == SortDirection.ASCENDING
                    ? Sort.Order.asc(property)
                    : Sort.Order.desc(property);
            sort = sort.and(sortOrder);
        }
        if (sort.isUnsorted()) {
            sort = Sort.by(Sort.Order.desc("averageScore"), Sort.Order.asc("group.groupCode"));
        }
        return sort;
    }

    private String formatAverage(BigDecimal average) {
        BigDecimal safeAverage = average == null ? BigDecimal.ZERO : average.setScale(2, RoundingMode.HALF_UP);
        return numberFormat.format(safeAverage);
    }

    private String formatPercentValue(int count, int total) {
        if (total <= 0) {
            return numberFormat.format(BigDecimal.ZERO) + " %";
        }
        BigDecimal percent = BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return numberFormat.format(percent) + " %";
    }

    private Span percentageBadge(int count, int total) {
        String text = formatPercentValue(count, total);
        Span span = new Span(text);
        span.getElement().getThemeList().add("badge");
        double percentValue = total == 0 ? 0 : (double) count * 100 / total;
        if (percentValue >= 80) {
            span.getElement().getThemeList().add("success");
        } else if (percentValue >= 60) {
            span.getElement().getThemeList().add("contrast");
        } else if (percentValue > 0) {
            span.getElement().getThemeList().add("error");
        }
        return span;
    }

    private record RatingRow(
            String student,
            String group,
            BigDecimal average,
            int count5,
            int count4,
            int count3,
            int total
    ) {
    }
}
