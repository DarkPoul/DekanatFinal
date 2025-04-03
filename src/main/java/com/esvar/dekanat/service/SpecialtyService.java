package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.SpecialtyEntity;
import com.esvar.dekanat.repository.SpecialtyRepository;
import org.springframework.stereotype.Service;

@Service
public class SpecialtyService {
    private final SpecialtyRepository specialtyRepository;

    public SpecialtyService(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    public SpecialtyEntity getSpecialtyByAbbreviation(String abbreviation) {
        return specialtyRepository.findByAbbreviation(abbreviation);
    }
}
