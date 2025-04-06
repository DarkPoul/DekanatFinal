package com.esvar.dekanat.mark;

import com.esvar.dekanat.dto.MarkDTO;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.PlansEntity;

public interface MarkProcessor {
    MarksEntity processMark(MarkDTO markDTO, PlansEntity plan, String controlType);
}

