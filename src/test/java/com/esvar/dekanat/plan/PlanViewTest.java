package com.esvar.dekanat.plan;

import com.esvar.dekanat.entity.*;
import com.esvar.dekanat.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlanViewTest {
    @Mock
    private DisciplineService disciplineService;
    @Mock
    private DepartmentService departmentService;
    @Mock
    private ControlMethodService controlMethodService;
    @Mock
    private SpecialtyService specialtyService;
    @Mock
    private GroupService groupService;
    @Mock
    private PlanService planService;
    @Mock
    private MarksPartsService marksPartsService;
    @Mock
    private StudentPlansService studentPlansService;
    @Mock
    private MarksService marksService;
    @Mock
    private ControlPartsService controlPartsService;
    @Mock
    private StudentService studentService;
    @Mock
    private MarksInitializerService marksInitializerService;

    private PlanView planView;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(disciplineService.getAllDisciplines()).thenReturn(Collections.emptyList());
        when(departmentService.getAllDepartments()).thenReturn(Collections.emptyList());
        when(controlMethodService.getTypeControlMethod(anyInt())).thenReturn(Collections.emptyList());
        planView = new PlanView(disciplineService, departmentService, controlMethodService, specialtyService,
                groupService, planService, marksPartsService, studentPlansService, marksService, controlPartsService,
                studentService, marksInitializerService);
    }

    @Test
    void updateExistingPlan_switchToNonElective_deletesStudentPlans() throws Exception {
        PlansEntity existingPlan = new PlansEntity();
        existingPlan.setId(1L);
        existingPlan.setElective(true);
        existingPlan.setFirstControl(new ControlMethodEntity());
        ControlMethodEntity second = new ControlMethodEntity();
        second.setName("Test");
        existingPlan.setSecondControl(second);

        when(planService.getPlanById(1L)).thenReturn(existingPlan);
        when(planService.getAllPlansForGroupAndSemester(any(), anyInt())).thenReturn(Collections.emptyList());
        when(disciplineService.getDisciplineByTitle(anyString())).thenReturn(new DisciplineEntity());
        when(controlMethodService.getControlMethodByName(anyString())).thenReturn(second);
        when(departmentService.getDepartmentByTitle(anyString())).thenReturn(new DepartmentEntity());

        Method m = PlanView.class.getDeclaredMethod("updateExistingPlan", Long.class, String.class, int.class, boolean.class,
                String.class, String.class, String.class, String.class, java.util.List.class);
        m.setAccessible(true);

        m.invoke(planView, 1L, "Disc", 1, false, "First", "Second", "1", "Dep", Collections.emptyList());

        verify(studentPlansService).deleteByPlanId(1L);
    }
}