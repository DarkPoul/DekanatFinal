package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.SpecialtyEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GroupCodeService {

    private static final Pattern GROUP_CODE_PATTERN = Pattern.compile(
            "^(?<prefix>[^-]+)-(?<course>\\d+)-(?<number>\\d+)-(?<year>\\d+)(?:\\((?<suffix>\\d+)\\))?$"
    );

    public String buildGroupCode(String groupPrefix, String course, String groupNumber, String graduationYear) {
        return buildGroupCode(groupPrefix, course, groupNumber, graduationYear, null);
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

    public List<String> buildCandidateGroupCodes(String groupPrefix, String course, String groupNumber, String graduationYear, SpecialtyEntity specialty) {
        List<String> candidates = new ArrayList<>();

        String eduProgramCode = buildGroupCodeWithEduProgram(groupPrefix, course, groupNumber, graduationYear, specialty);
        if (eduProgramCode != null) {
            candidates.add(eduProgramCode);
        }

        String specialtyCode = buildGroupCodeWithSpecialtySuffix(groupPrefix, course, groupNumber, graduationYear, specialty);
        if (specialtyCode != null && !candidates.contains(specialtyCode)) {
            candidates.add(specialtyCode);
        }

        String legacy = buildLegacyGroupCode(groupPrefix, course, groupNumber, graduationYear);
        if (!candidates.contains(legacy)) {
            candidates.add(legacy);
        }

        return candidates;
    }

    public String buildLegacyGroupCode(String groupPrefix, String course, String groupNumber, String graduationYear) {
        return String.format("%s-%s-%s-%s", groupPrefix, course, groupNumber, graduationYear);
    }

    public GroupParts parseGroupParts(String groupCode) {
        if (groupCode == null || groupCode.isBlank()) {
            return GroupParts.empty();
        }

        Matcher matcher = GROUP_CODE_PATTERN.matcher(groupCode);
        if (!matcher.matches()) {
            return GroupParts.empty();
        }

        return new GroupParts(
                matcher.group("prefix"),
                matcher.group("course"),
                matcher.group("number"),
                matcher.group("year"),
                parseSuffix(matcher.group("suffix"))
        );
    }

    private String buildGroupCodeWithEduProgram(String groupPrefix, String course, String groupNumber, String graduationYear, SpecialtyEntity specialty) {
        if (specialty != null && specialty.getEduProgram() != null && specialty.getEduProgram().getId() > 0) {
            return String.format("%s-%s-%s-%s(%d)", groupPrefix, course, groupNumber, graduationYear, specialty.getEduProgram().getId());
        }
        return null;
    }

    private String buildGroupCodeWithSpecialtySuffix(String groupPrefix, String course, String groupNumber, String graduationYear, SpecialtyEntity specialty) {
        if (specialty != null && specialty.getId() != null) {
            return String.format("%s-%s-%s-%s(%d)", groupPrefix, course, groupNumber, graduationYear, specialty.getId());
        }
        return null;
    }

    private Long parseSuffix(String suffix) {
        if (suffix == null || suffix.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(suffix);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public record GroupParts(String groupPrefix, String course, String groupNumber, String graduationYear, Long suffixId) {
        static GroupParts empty() {
            return new GroupParts(null, null, null, null, null);
        }
    }
}
