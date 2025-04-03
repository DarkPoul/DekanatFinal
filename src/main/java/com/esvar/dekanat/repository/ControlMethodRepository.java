package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.ControlMethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ControlMethodRepository extends JpaRepository<ControlMethodEntity, Long> {
    List<ControlMethodEntity> findByType(int type);

    ControlMethodEntity findByName(String name);
}
