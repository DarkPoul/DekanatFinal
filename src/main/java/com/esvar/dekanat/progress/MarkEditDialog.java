package com.esvar.dekanat.progress;

import com.esvar.dekanat.entity.ControlMethodEntity;
import com.esvar.dekanat.entity.DisciplineEntity;
import com.esvar.dekanat.service.ControlMethodService;
import com.esvar.dekanat.service.DisciplineService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import lombok.Setter;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Dialog for editing a student's mark information.
 */
public class MarkEditDialog extends Dialog {

    @Setter
    private SaveListener saveListener;

    private final ComboBox<String> discipline = new ComboBox<>("Дисципліна");
    private final Select<Integer> semester = new Select<>();
    private final IntegerField hours = new IntegerField("Години");
    private final ComboBox<String> controlType = new ComboBox<>("Тип контролю");
    private final IntegerField grade = new IntegerField("Оцінка");

    private final Button save = new Button("Зберегти");
    private final Button cancel = new Button("Відміна");

    public MarkEditDialog(DisciplineService disciplineService,
                          ControlMethodService controlMethodService) {
        List<String> disciplines = disciplineService.getAllDisciplines().stream()
                .map(DisciplineEntity::getTitle).toList();
        discipline.setItems(disciplines);
        List<String> controls = controlMethodService.getTypeControlMethod(0).stream()
                .map(ControlMethodEntity::getName).toList();
        controlType.setItems(controls);

        semester.setLabel("Семестр");
        semester.setItems(IntStream.rangeClosed(1, 4).boxed().toList());
        grade.setMin(0);
        grade.setMax(100);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addClickListener(e -> handleSave());
        cancel.addClickListener(e -> close());

        HorizontalLayout actions = new HorizontalLayout(save, cancel);
        add(discipline, semester, hours, controlType, grade, actions);
    }

    public void open(String disc, int sem, int hrs, String ctrl, int grd) {
        discipline.setValue(disc);
        semester.setValue(sem);
        hours.setValue(hrs);
        controlType.setValue(ctrl);
        grade.setValue(grd);
        super.open();
    }

    private void handleSave() {
        if (saveListener != null) {
            saveListener.onSave(discipline.getValue(),
                    semester.getValue(),
                    hours.getValue() == null ? 0 : hours.getValue(),
                    controlType.getValue(),
                    grade.getValue() == null ? 0 : grade.getValue());
        }
        close();
    }

    /** Listener for save action. */
    public interface SaveListener {
        void onSave(String discipline, int semester, int hours, String controlType, int grade);
    }
}
