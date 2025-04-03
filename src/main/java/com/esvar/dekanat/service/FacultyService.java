package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.FacultyEntity;
import com.esvar.dekanat.repository.FacultyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FacultyService {

    private final FacultyRepository facultyRepository;

    public FacultyService(FacultyRepository facultyRepository) {
        this.facultyRepository = facultyRepository;
    }


    public List<String> getFacultyTitles() {
        return facultyRepository.findAll().stream().map(FacultyEntity::getTitle).collect(Collectors.toList());
    }
}
