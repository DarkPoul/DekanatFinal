package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.ControlMethodEntity;
import com.esvar.dekanat.entity.ControlPartsEntity;
import com.esvar.dekanat.repository.ControlPartsRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


    /**
     * Retrieve all control parts for a method creating missing ones.
     *
     * @param method     control method
     * @param totalParts expected number of parts
     * @return map of part number to ControlPartsEntity
     */
    public Map<Integer, ControlPartsEntity> getOrCreatePartsMap(ControlMethodEntity method, int totalParts) {
        List<ControlPartsEntity> existing = controlPartsRepository.findByControlMethodId(method.getId());
        Map<Integer, ControlPartsEntity> result = new HashMap<>();
        for (ControlPartsEntity cp : existing) {
            result.put(cp.getPartNumber(), cp);
        }

        List<ControlPartsEntity> toCreate = new ArrayList<>();
        for (int i = 1; i <= totalParts; i++) {
            if (!result.containsKey(i)) {
                ControlPartsEntity cp = new ControlPartsEntity();
                cp.setControlMethod(method);
                cp.setPartNumber(i);
                toCreate.add(cp);
                result.put(i, cp);
            }
        }
        if (!toCreate.isEmpty()) {
            controlPartsRepository.saveAll(toCreate);
        }
        return result;
    }

}
