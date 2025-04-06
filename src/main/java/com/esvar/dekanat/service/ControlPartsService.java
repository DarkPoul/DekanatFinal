package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.ControlMethodEntity;
import com.esvar.dekanat.entity.ControlPartsEntity;
import com.esvar.dekanat.repository.ControlPartsRepository;
import org.springframework.stereotype.Service;

@Service
public class ControlPartsService {

    private final ControlPartsRepository controlPartsRepository;

    public ControlPartsService(ControlPartsRepository controlPartsRepository) {
        this.controlPartsRepository = controlPartsRepository;
    }

    public ControlPartsEntity getControlPartByControlMethodAndPartNumber(ControlMethodEntity controlMethod, int partNumber) {
        if (controlMethod == null || partNumber <= 0) {
            return null;
        }
        return controlPartsRepository.findByControlMethodIdAndPartNumber(controlMethod.getId(), partNumber)
                .orElse(null);
    }

    // Метод збереження нової контрольної частини
    public ControlPartsEntity saveControlPart(ControlPartsEntity part) {
        if (part == null || part.getControlMethod() == null) {
            throw new IllegalArgumentException("Метод контролю для частини повинен бути заданий.");
        }
        boolean exists = controlPartsRepository.existsByControlMethodIdAndPartNumber(
                part.getControlMethod().getId(),
                part.getPartNumber()
        );
        if (exists) {
            throw new IllegalStateException("Частина для даного методу контролю вже існує.");
        }
        return controlPartsRepository.save(part);
    }
}
