package com.esvar.dekanat.service;

import com.esvar.dekanat.dto.GroupDTO;
import com.esvar.dekanat.entity.StudentRatingEntity;
import com.esvar.dekanat.repository.StudentRatingRepository;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
@Service
public class RatingService {

    private final GroupService groupService;

    private final StudentRatingRepository ratingRepository;

    public RatingService(GroupService groupService, StudentRatingRepository ratingRepository) {
        this.groupService = groupService;
        this.ratingRepository = ratingRepository;


    public RatingService(GroupService groupService) {
        this.groupService = groupService;

    }

    public List<String> getSpecialties() {
        return groupService.getGroupsDTO().stream()

                .map(GroupDTO::getSpecialtyAbbreviation)

                .map(GroupDTO::getGroupCode)
                .map(split -> split.split("-")[0])
                .distinct()
                .sorted()
                .toList();
    }

    public List<String> getCourses() {
        return groupService.getGroupsDTO().stream()
                .map(GroupDTO::getCourse)
                .map(String::valueOf)
                .map(GroupDTO::getGroupCode)
                .map(split -> split.split("-")[1])

                .distinct()
                .sorted()
                .toList();
    }

    public List<String> getGroupCodes() {
        return groupService.getGroupsDTO().stream()
                .map(GroupDTO::getGroupCode)

                .map(split -> split.split("-")[2])
                .distinct()
                .sorted()
                .toList();
    }

    public List<Integer> getYears() {
        return groupService.getGroupsDTO().stream()
                .map(GroupDTO::getYear);
      
    public List<String> getYears() {
        return groupService.getGroupsDTO().stream()
                .map(GroupDTO::getGroupCode)
                .map(split -> split.split("-")[3])
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    public List<StudentRatingEntity> searchRatings(String specialty,
                                                   String course,
                                                   String group,
                                                   Integer year,
                                                   boolean technikum,
                                                   boolean budget) {
        Integer courseNumber = null;
        if (course != null && !course.isEmpty()) {
            try {
                courseNumber = Integer.parseInt(course);
            } catch (NumberFormatException ignored) {
            }
        }
        return ratingRepository.searchRatings(
                specialty,
                courseNumber,
                group,
                year,
                technikum,
                budget
        );
    }
}
