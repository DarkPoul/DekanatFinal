package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.ReportEntity;
import com.esvar.dekanat.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, Long> {

    List<ReportEntity> findAllByStudent(StudentEntity student);

    @Query("SELECT MAX(r.id) FROM ReportEntity r")
    Long findMaxId();

}
