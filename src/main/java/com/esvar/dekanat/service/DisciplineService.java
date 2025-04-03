package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.DisciplineEntity;
import com.esvar.dekanat.repository.DisciplineRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class DisciplineService {
    private final DisciplineRepository disciplineRepository;

    public DisciplineService(DisciplineRepository disciplineRepository) {
        this.disciplineRepository = disciplineRepository;
    }


    public DisciplineEntity getFirstDisc() {
        return disciplineRepository.findAll().get(0);
    }

    public List<DisciplineEntity> getAllDisciplines() {
        return disciplineRepository.findAll();
    }

    public DisciplineEntity getDisciplineByTitle(String title) {
        return disciplineRepository.findByTitle(title);
    }
}
