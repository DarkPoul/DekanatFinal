package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.FacultyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FacultyRepository extends JpaRepository<FacultyEntity, Long> {

    FacultyEntity findByTitle(String title);

}
