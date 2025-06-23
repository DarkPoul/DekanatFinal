package com.esvar.dekanat.service;

import com.esvar.dekanat.dto.GroupDTO;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
@Service
public class RatingService {

    private final GroupService groupService;

    public RatingService(GroupService groupService) {
        this.groupService = groupService;
    }

    public List<String> getSpecialties() {
        return groupService.getGroupsDTO().stream()
                .map(GroupDTO::getSpecialtyAbbreviation)
                .distinct()
                .sorted()
                .toList();
    }

    public List<String> getCourses() {
        return groupService.getGroupsDTO().stream()
                .map(GroupDTO::getCourse)
                .map(String::valueOf)
                .distinct()
                .sorted()
                .toList();
    }

    public List<String> getGroupCodes() {
        return groupService.getGroupsDTO().stream()
                .map(GroupDTO::getGroupCode)
                .distinct()
                .sorted()
                .toList();
    }

    public List<Integer> getYears() {
        return groupService.getGroupsDTO().stream()
                .map(GroupDTO::getYear)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }
}
