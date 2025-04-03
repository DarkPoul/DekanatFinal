package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.DepartmentEntity;
import com.esvar.dekanat.repository.DepartmentRepository;
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
        return departmentRepository.findAll().get(0);
    }

    public List<DepartmentEntity> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public DepartmentEntity getDepartmentByAbbreviation(String abbreviation) {
        return departmentRepository.findByAbbreviation(abbreviation);
    }

    public DepartmentEntity getDepartmentByTitle(String title) {
        return departmentRepository.findByTitle(title);
    }

    public List<String> getAllDepartment() {
        return departmentRepository.findAll().stream().map(DepartmentEntity::getTitle).toList();
    }
}
