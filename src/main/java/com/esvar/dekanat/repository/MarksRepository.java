package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.PlansEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarksRepository extends JpaRepository<MarksEntity, Long> {

    /**
     * Перевіряє, чи існує запис у таблиці marks за комбінацією student_id, plan_id та control_method_id.
     *
     * @param studentId       ID студента.
     * @param planId          ID плану.
     * @param controlMethodId ID методу контролю.
     * @return true, якщо запис існує; false — якщо ні.
     */
    boolean existsByStudentIdAndPlanIdAndControlMethodId(Long studentId, Long planId, Long controlMethodId);

    MarksEntity findByStudentIdAndPlanIdAndControlMethodId(Long studentId, Long planId, Long controlMethodId);

    /**
     * Знайти оцінку за ID студента та ID плану.
     *
     * @param studentId ID студента.
     * @param planId    ID плану.
     * @return Optional<MarksEntity> - знайдена оцінка або порожній Optional.
     */
    @Query("""
        SELECT m FROM MarksEntity m
        WHERE m.student.id = :studentId AND m.plan.id = :planId
    """)
    Optional<MarksEntity> findByStudentIdAndPlanId(Long studentId, Long planId);
    @Query("SELECT MAX(m.id) FROM MarksEntity m")
    Optional<Long> findMaxId();

    List<MarksEntity> findByPlan(PlansEntity plansEntity);
}
