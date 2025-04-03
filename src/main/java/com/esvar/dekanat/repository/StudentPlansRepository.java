package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentPlansEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentPlansRepository extends JpaRepository<StudentPlansEntity, Long> {

    /**
     * Знайти ID студентів, які вибрали конкретний план.
     *
     * @param planId ID плану.
     * @return List<Long> - список ID студентів.
     */
    @Query("""
        SELECT sp.student.id
        FROM StudentPlansEntity sp
        WHERE sp.plan.id = :planId
    """)
    List<Long> findStudentIdsByPlanId(Long planId);

    /**
     * Перевіряє, чи існує запис у таблиці student_plans за student_id та plan_id.
     *
     * @param studentId ID студента.
     * @param planId    ID плану.
     * @return true, якщо запис існує; false — якщо ні.
     */
    boolean existsByStudentIdAndPlanId(Long studentId, Long planId);

    /**
     * Видаляє всі записи у student_plans для певного плану.
     *
     * @param planId ID плану.
     */
    void deleteAllByPlanId(Long planId);

    void deleteByPlan(PlansEntity plan);

    @Modifying
    @Query("DELETE FROM StudentPlansEntity sp WHERE sp.plan.id = :planId")
    void deleteByPlanId(@Param("planId") Long planId);

    List<StudentPlansEntity> findByPlan(PlansEntity plan); // Повертає записи для конкретного плану
}
