package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.FacultyEntity;
import com.esvar.dekanat.entity.PlanStatementNumberEntity;
import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.repository.PlanStatementNumberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlanStatementNumberService {

    private final PlanStatementNumberRepository repository;

    public PlanStatementNumberService(PlanStatementNumberRepository repository) {
        this.repository = repository;
    }

    /**
     * Assigns the next statement number for the plan without persisting
     * any {@link PlanStatementNumberEntity} records. This should be invoked
     * before the plan is saved so the {@code statement_number} column is not
     * null when inserting the plan.
     */
    public void assignNumber(PlansEntity plan) {
        if (plan == null || plan.getFaculty() == null || plan.getStatementNumber() != null) {
            return;
        }

        String number = nextNumberForFaculty(plan.getFaculty());
        plan.setStatementNumber(number);
    }

    /**
     * Creates the statement number records for the already persisted plan.
     */
    @Transactional
    public void createRecordsForPlan(PlansEntity plan) {
        if (plan == null || plan.getFaculty() == null || plan.getId() == null) {
            return;
        }

        String number = plan.getStatementNumber();
        if (number == null) {
            number = nextNumberForFaculty(plan.getFaculty());
            plan.setStatementNumber(number);
        }

        createRecords(plan, number);
    }

    @Transactional
    public void updateForPlan(PlansEntity plan) {
        if (plan == null) {
            return;
        }

        List<PlanStatementNumberEntity> existing = repository.findByAcademicPlan(plan);
        String number = existing.stream()
                .findFirst()
                .map(PlanStatementNumberEntity::getStatementNumber)
                .orElseGet(() -> nextNumberForFaculty(plan.getFaculty()));

        Map<Integer, PlanStatementNumberEntity> map = existing.stream()
                .collect(Collectors.toMap(PlanStatementNumberEntity::getControlType, e -> e));

        int firstType = plan.getFirstControl().getType();
        PlanStatementNumberEntity first = map.get(firstType);
        if (first == null) {
            first = new PlanStatementNumberEntity();
        }
        first.setAcademicPlan(plan);
        first.setFaculty(plan.getFaculty());
        first.setStatementNumber(number);
        first.setControlType(firstType);
        first.setFirstAdditional(false);
        first.setSecondAdditional(false);
        repository.save(first);
        map.remove(firstType);

        if (plan.getSecondControl() != null && !"Відсутній".equals(plan.getSecondControl().getName())) {
            int secondType = plan.getSecondControl().getType();
            PlanStatementNumberEntity second = map.get(secondType);
            if (second == null) {
                second = new PlanStatementNumberEntity();
            }
            second.setAcademicPlan(plan);
            second.setFaculty(plan.getFaculty());
            second.setStatementNumber(number);
            second.setControlType(secondType);
            second.setFirstAdditional(false);
            second.setSecondAdditional(false);
            repository.save(second);
            map.remove(secondType);
        }

        // delete obsolete records
        if (!map.isEmpty()) {
            repository.deleteAll(map.values());
        }

        plan.setStatementNumber(number);
    }

    @Transactional
    public void deleteByPlanId(Long planId) {
        if (planId != null) {
            repository.deleteByPlanId(planId);
        }
    }

    private String nextNumberForFaculty(FacultyEntity faculty) {
        String last = repository.findFirstByFacultyOrderByStatementNumberDesc(faculty)
                .map(PlanStatementNumberEntity::getStatementNumber)
                .orElse("000");
        return String.format("%03d", Integer.parseInt(last) + 1);
    }

    private void createRecords(PlansEntity plan, String number) {
        PlanStatementNumberEntity first = new PlanStatementNumberEntity();
        first.setAcademicPlan(plan);
        first.setFaculty(plan.getFaculty());
        first.setStatementNumber(number);
        first.setControlType(plan.getFirstControl().getType());
        first.setFirstAdditional(false);
        first.setSecondAdditional(false);
        repository.save(first);

        if (plan.getSecondControl() != null && !"Відсутній".equals(plan.getSecondControl().getName())) {
            PlanStatementNumberEntity second = new PlanStatementNumberEntity();
            second.setAcademicPlan(plan);
            second.setFaculty(plan.getFaculty());
            second.setStatementNumber(number);
            second.setControlType(plan.getSecondControl().getType());
            second.setFirstAdditional(false);
            second.setSecondAdditional(false);
            repository.save(second);
        }
    }
}