package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.DepartmentEntity;
import com.esvar.dekanat.repository.DepartmentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public DepartmentEntity getFirstDep() {
        return departmentRepository.findAll(Sort.by(Sort.Direction.ASC, "title")).get(0);
    }

    public List<DepartmentEntity> getAllDepartments() {
        return departmentRepository.findAll(Sort.by(Sort.Direction.ASC, "title"));
    }

    public DepartmentEntity getDepartmentByAbbreviation(String abbreviation) {
        return departmentRepository.findByAbbreviation(abbreviation);
    }

    public DepartmentEntity getDepartmentByTitle(String title) {
        return departmentRepository.findByTitle(title);
    }

    public List<String> getAllDepartment() {
        return getAllDepartments().stream()
                .map(DepartmentEntity::getTitle)
                .toList();
    }

    public String getDepartmentById(Long id) {
        return departmentRepository.findById(id).map(DepartmentEntity::getTitle).orElse(null);
    }
}
