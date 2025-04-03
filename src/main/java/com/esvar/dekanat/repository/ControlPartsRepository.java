package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.ControlPartsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ControlPartsRepository extends JpaRepository<ControlPartsEntity, Long> {
    /**
     * Перевіряє, чи існує частина за комбінацією control_method_id та part_number.
     *
     * @param controlMethodId ID методу контролю.
     * @param partNumber      Номер частини.
     * @return true, якщо частина існує; false — якщо ні.
     */
    boolean existsByControlMethodIdAndPartNumber(Long controlMethodId, int partNumber);

    /**
     * Знайти частина за ID методу контролю та номером частини.
     *
     * @param controlMethodId ID методу контролю.
     * @param partNumber      Номер частини.
     * @return Optional<ControlPartsEntity> - знайдена частина або порожній Optional.
     */
    @Query("""
        SELECT cp FROM ControlPartsEntity cp
        WHERE cp.controlMethod.id = :controlMethodId AND cp.partNumber = :partNumber
    """)
    Optional<ControlPartsEntity> findByControlMethodIdAndPartNumber(Long controlMethodId, int partNumber);
}
