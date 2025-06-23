package com.esvar.dekanat.card;

import com.vaadin.flow.component.datepicker.DatePicker;
import java.util.List;

/** Utility methods for DatePicker localization. */
public final class DatePickerUtil {
    private DatePickerUtil() {}

    /** Returns Ukrainian localization for Vaadin DatePicker. */
    public static DatePicker.DatePickerI18n ukrainian() {
        DatePicker.DatePickerI18n ua = new DatePicker.DatePickerI18n();
        ua.setMonthNames(List.of("Січень", "Лютий", "Березень", "Квітень",
                "Травень", "Червень", "Липень", "Серпень", "Вересень", "Жовтень",
                "Листопад", "Грудень"));
        ua.setWeekdays(List.of("Неділя", "Понеділок", "Вівторок",
                "Середа", "Четвер", "П'ятниця", "Субота"));
        ua.setWeekdaysShort(List.of("Нд", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"));
        ua.setToday("Сьогодні");
        ua.setCancel("Скасувати");
        return ua;
    }
}
