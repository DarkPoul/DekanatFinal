package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.StudentGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<StudentGroupEntity, Long> {
    StudentGroupEntity findByGroupCode(String groupCode);

    /**
     * Отримує ID групи за її кодом.
     *
     * @param groupCode Код групи.
     * @return Optional<Long> - ID групи або порожній Optional.
     */
    @Query("""
        SELECT g.id FROM StudentGroupEntity g WHERE g.groupCode = :groupCode
    """)
    Optional<Long> findIdByGroupCode(String groupCode);


}
