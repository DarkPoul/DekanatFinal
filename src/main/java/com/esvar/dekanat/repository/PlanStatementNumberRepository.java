package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.FacultyEntity;
import com.esvar.dekanat.entity.PlanStatementNumberEntity;
import com.esvar.dekanat.entity.PlansEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanStatementNumberRepository extends JpaRepository<PlanStatementNumberEntity, Long> {
    Optional<PlanStatementNumberEntity> findFirstByFacultyOrderByStatementNumberDesc(FacultyEntity faculty);

    List<PlanStatementNumberEntity> findByAcademicPlan(PlansEntity academicPlan);

    @Modifying
    @Query("DELETE FROM PlanStatementNumberEntity p WHERE p.academicPlan.id = :planId")
    void deleteByPlanId(@Param("planId") Long planId);
}