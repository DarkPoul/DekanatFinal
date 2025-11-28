package com.esvar.dekanat.card;

import com.esvar.dekanat.entity.SpecialtyEntity;
import com.esvar.dekanat.service.SpecialtyService;

/**
 * Responsible for constructing group codes based on specialty and educational program data.
 */
public class GroupCodeBuilder {

    private final SpecialtyService specialtyService;

    public GroupCodeBuilder(SpecialtyService specialtyService) {
        this.specialtyService = specialtyService;
    }

    public String buildGroupCode(String groupPrefix, String course, String groupNumber, String graduationYear) {
        SpecialtyEntity specialty = specialtyService.getSpecialtyByAbbreviation(groupPrefix);
        String eduProgramCode = buildGroupCodeWithEduProgram(groupPrefix, course, groupNumber, graduationYear, specialty);
        if (eduProgramCode != null) {
            return eduProgramCode;
        }
        String specialtyCode = buildGroupCodeWithSpecialtySuffix(groupPrefix, course, groupNumber, graduationYear, specialty);
        if (specialtyCode != null) {
            return specialtyCode;
        }
        return buildLegacyGroupCode(groupPrefix, course, groupNumber, graduationYear);
    }

    public String buildGroupCode(String groupPrefix, String course, String groupNumber, String graduationYear, SpecialtyEntity specialty) {
        String eduProgramCode = buildGroupCodeWithEduProgram(groupPrefix, course, groupNumber, graduationYear, specialty);
        if (eduProgramCode != null) {
            return eduProgramCode;
        }
        String specialtyCode = buildGroupCodeWithSpecialtySuffix(groupPrefix, course, groupNumber, graduationYear, specialty);
        if (specialtyCode != null) {
            return specialtyCode;
        }
        return buildLegacyGroupCode(groupPrefix, course, groupNumber, graduationYear);
    }

    public String buildGroupCodeWithEduProgram(String groupPrefix, String course, String groupNumber, String graduationYear, SpecialtyEntity specialty) {
        if (specialty != null && specialty.getEduProgram() != null && specialty.getEduProgram().getId() > 0) {
            return String.format("%s-%s-%s-%s(%d)", groupPrefix, course, groupNumber, graduationYear, specialty.getEduProgram().getId());
        }
        return null;
    }

    public String buildGroupCodeWithSpecialtySuffix(String groupPrefix, String course, String groupNumber, String graduationYear, SpecialtyEntity specialty) {
        if (specialty != null && specialty.getId() != null) {
            return String.format("%s-%s-%s-%s(%d)", groupPrefix, course, groupNumber, graduationYear, specialty.getId());
        }
        return null;
    }

    public String buildLegacyGroupCode(String groupPrefix, String course, String groupNumber, String graduationYear) {
        return String.format("%s-%s-%s-%s", groupPrefix, course, groupNumber, graduationYear);
    }
}
