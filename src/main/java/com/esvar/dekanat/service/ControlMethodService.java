package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.ControlMethodEntity;

import com.esvar.dekanat.repository.ControlMethodRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class ControlMethodService {
    private final ControlMethodRepository controlMethodRepository;

    public ControlMethodService(ControlMethodRepository controlMethodRepository) {
        this.controlMethodRepository = controlMethodRepository;
    }

    public ControlMethodEntity getFirstControlMethod() {
        return controlMethodRepository.findAll().get(0);
    }

    public List<ControlMethodEntity> getTypeControlMethod(int type) {
        return controlMethodRepository.findByType(type);
    }

    public ControlMethodEntity getControlMethodByName(String name) {
        return controlMethodRepository.findByName(name);
    }
}
