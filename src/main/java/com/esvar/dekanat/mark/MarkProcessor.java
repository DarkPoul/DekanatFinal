package com.esvar.dekanat.mark;

import com.esvar.dekanat.dto.MarkDTO;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;

public interface MarkProcessor {
    /**
     * Process the given {@link MarkDTO} and return a {@link MarksEntity}.
     * The implementation may or may not persist the entity.
     *
     * @param markDTO     data transfer object with mark information
     * @param plan        plan the mark belongs to
     * @param controlType name of the control type
     * @return processed entity (possibly already persisted)
     */
    MarksEntity processMark(MarkDTO markDTO, PlansEntity plan, StudentGroupEntity group, String controlType);

    /**
     * @return {@code true} if {@link #processMark} already persisted the entity
     * and no additional save is required.
     */
    default boolean isPersistedAfterProcessing() {
        return false;
    }
}

