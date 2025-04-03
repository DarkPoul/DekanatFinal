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

    /**
     * Зберігає нову частину або оновлює існуючу.
     *
     * @param part ControlPartsEntity - об'єкт для збереження.
     * @return ControlPartsEntity - збережений об'єкт.
     */
//    public ControlPartsEntity saveControlPart(ControlPartsEntity part) {
//        if (part == null || part.getControlMethod() == null) {
//            throw new IllegalArgumentException("Метод контролю для частини повинен бути заданий.");
//        }
//
//        // Перевіряємо, чи частина вже існує
//        boolean exists = controlPartsRepository.existsByControlMethodIdAndPartNumber(
//                part.getControlMethod().getId(),
//                part.getPartNumber()
//        );
//
//        if (exists) {
//            throw new IllegalStateException("Частина для даного методу контролю вже існує.");
//        }
//
//        // Зберігаємо нову частину
//        return controlPartsRepository.save(part);
//    }

    /**
     * Отримує частина за методом контролю та номером частини.
     *
     * @param controlMethodByName ControlMethodEntity - метод контролю.
     * @param partNumber          int - номер частини.
     * @return ControlPartsEntity - знайдена частина або null, якщо не знайдено.
     */
//    public ControlPartsEntity getControlPartByControlMethodAndPartNumber(ControlMethodEntity controlMethodByName, int partNumber) {
//        if (controlMethodByName == null || partNumber <= 0) {
//            return null; // Якщо метод контролю або номер частини некоректні, повертаємо null
//        }
//
//        return controlPartsRepository.findByControlMethodIdAndPartNumber(controlMethodByName.getId(), partNumber).orElse(null);
//    }
}
