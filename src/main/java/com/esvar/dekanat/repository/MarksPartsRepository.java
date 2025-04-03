package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.ControlPartsEntity;
import com.esvar.dekanat.entity.MarksPartsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface MarksPartsRepository extends JpaRepository<MarksPartsEntity, Long> {

    @Query("""
        SELECT DISTINCT cp.partNumber
        FROM MarksPartsEntity mp
        JOIN ControlPartsEntity cp ON mp.controlPart.id = cp.id
        WHERE mp.mark.id IN (
            SELECT m.id
            FROM MarksEntity m
            WHERE m.plan.id = :planId
        )
    """)
    Set<Integer> findDistinctPartNumbersByPlanId(Long planId);

    /**
     * Перевіряє, чи існує запис у marks_parts за комбінацією mark_id та part_id.
     *
     * @param markId   ID оцінки.
     * @param partId   ID частини.
     * @return true, якщо запис існує; false — якщо ні.
     */
    boolean existsByMarkIdAndControlPart(Long markId, ControlPartsEntity partId);

    /**
     * Видаляє всі записи у marks_parts, пов'язані з певним планом через marks.
     *
     * @param planId ID плану.
     */
    @Modifying
    @Query("DELETE FROM MarksPartsEntity mp WHERE mp.mark.plan.id = :planId")
    void deleteByPlanId(@Param("planId") Long planId);

    /**
     * Знайти запис у marks_parts за ID оцінки та ID частини.
     *
     * @param markId  ID оцінки.
     * @param partId  ID частини.
     * @return Optional<MarksPartsEntity> - знайдений запис або порожній Optional.
     */
    @Query("""
        SELECT mp FROM MarksPartsEntity mp
        WHERE mp.mark.id = :markId AND mp.controlPart.id = :partId
    """)
    Optional<MarksPartsEntity> findByMarkIdAndPartId(Long markId, Long partId);
}
