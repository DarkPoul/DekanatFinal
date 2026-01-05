package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.EduProgramEntity;
import com.esvar.dekanat.entity.SpecialtyEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GroupCodeServiceTest {

    private final GroupCodeService service = new GroupCodeService();

    @Test
    void buildsCodeWithEduProgramWhenAvailable() {
        SpecialtyEntity specialty = new SpecialtyEntity();
        specialty.setAbbreviation("CS");
        EduProgramEntity eduProgram = new EduProgramEntity();
        eduProgram.setId(7L);
        specialty.setEduProgram(eduProgram);

        String code = service.buildGroupCode("CS", "1", "2", "24", specialty);

        assertEquals("CS-1-2-24(7)", code);
    }

    @Test
    void buildsCodeWithSpecialtySuffixWhenEduProgramMissing() {
        SpecialtyEntity specialty = new SpecialtyEntity();
        specialty.setAbbreviation("CS");
        specialty.setId(5L);

        String code = service.buildGroupCode("CS", "2", "3", "25", specialty);

        assertEquals("CS-2-3-25(5)", code);
    }

    @Test
    void buildsLegacyCodeWhenNoSpecialtyProvided() {
        String code = service.buildGroupCode("CS", "3", "4", "26");

        assertEquals("CS-3-4-26", code);
    }

    @Test
    void collectsCandidateCodesInPriorityOrder() {
        SpecialtyEntity specialty = new SpecialtyEntity();
        specialty.setAbbreviation("CS");
        specialty.setId(3L);
        EduProgramEntity eduProgram = new EduProgramEntity();
        eduProgram.setId(8L);
        specialty.setEduProgram(eduProgram);

        var candidates = service.buildCandidateGroupCodes("CS", "1", "1", "24", specialty);

        assertEquals("CS-1-1-24(8)", candidates.get(0));
        assertEquals("CS-1-1-24(3)", candidates.get(1));
        assertEquals("CS-1-1-24", candidates.get(2));
    }

    @Test
    void parsesGroupPartsWithSuffix() {
        GroupCodeService.GroupParts parts = service.parseGroupParts("CS-3-4-26(9)");

        assertEquals("CS", parts.groupPrefix());
        assertEquals("3", parts.course());
        assertEquals("4", parts.groupNumber());
        assertEquals("26", parts.graduationYear());
        assertEquals(9L, parts.suffixId());
    }

    @Test
    void returnsEmptyPartsForInvalidCode() {
        GroupCodeService.GroupParts parts = service.parseGroupParts("invalid-code");

        assertNull(parts.groupPrefix());
        assertNull(parts.course());
        assertNull(parts.groupNumber());
        assertNull(parts.graduationYear());
        assertNull(parts.suffixId());
    }
}
