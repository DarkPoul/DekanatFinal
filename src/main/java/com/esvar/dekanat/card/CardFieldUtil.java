package com.esvar.dekanat.card;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.esvar.dekanat.entity.Gender;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Small helpers for updating UI field values consistently without duplicating null checks.
 */
public final class CardFieldUtil {

    private CardFieldUtil() {
    }

    public static void setTextFieldValue(TextField field, String value) {
        if (value != null) {
            field.setValue(value);
        } else {
            field.clear();
        }
    }

    public static void setSelectValue(Select<String> select, String value) {
        if (value != null) {
            select.setValue(value);
        } else {
            select.clear();
        }
    }

    public static void setDatePickerValue(DatePicker picker, String value) {
        if (value != null && !value.isBlank()) {
            try {
                picker.setValue(LocalDate.parse(value));
            } catch (DateTimeParseException ignored) {
                picker.clear();
            }
        } else {
            picker.clear();
        }
    }

    public static void setDatePickerValue(DatePicker picker, Date value) {
        if (value != null) {
            picker.setValue(value.toLocalDate());
        } else {
            picker.clear();
        }
    }

    public static void setGenderValue(Select<String> genderSelect, Gender gender) {
        if (gender != null) {
            genderSelect.setValue(gender.name());
        } else {
            genderSelect.clear();
        }
    }

    public static void setBenefitsValue(MultiSelectComboBox<String> benefitsSelect, String benefits) {
        if (benefits != null && !benefits.isBlank()) {
            Set<String> values = Arrays.stream(benefits.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (values.isEmpty()) {
                benefitsSelect.clear();
            } else {
                benefitsSelect.setValue(values);
            }
        } else {
            benefitsSelect.clear();
        }
    }

    public static Gender resolveGenderValue(Select<String> select, Gender fallback) {
        String value = select.getValue();
        if (value != null) {
            value = value.trim();
            if (!value.isEmpty()) {
                try {
                    return Gender.valueOf(value);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return fallback;
    }
}
