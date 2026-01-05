package com.esvar.dekanat.generate;

import com.esvar.dekanat.document.DocumentException;
import com.esvar.dekanat.document.DocumentGenerator;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Generator for zalik documents based on a docx template.
 */
@Component
public class ZalikGenerator implements DocumentGenerator {

    public static final String NAME = "zalik";
    private static final String TEMPLATE = "uploads/zalik.docx";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getTemplatePath() {
        return TEMPLATE;
    }

    @Override
    public Map<String, Object> prepareContext(Object data) {
        if (!(data instanceof DataModelForZalik zalik)) {
            throw new DocumentException("Expected DataModelForZalik");
        }
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("facultyName", zalik.facultyName());
        ctx.put("specialityName", zalik.specialityName());
        ctx.put("courseNumber", zalik.courseNumber());
        ctx.put("groupName", zalik.groupName());
        ctx.put("studyYear", zalik.studyYear());
        ctx.put("order", zalik.order());
        ctx.put("day", zalik.day());
        ctx.put("month", zalik.month());
        ctx.put("year", zalik.year());
        ctx.put("disciplineName", zalik.disciplineName());
        ctx.put("sN", zalik.semesterNumber());
        ctx.put("controlTypeName", zalik.controlTypeName());
        ctx.put("h", zalik.hours());
        ctx.put("f", zalik.firstTeacher());
        ctx.put("s", zalik.secondTeacher());
        ctx.put("dekan", zalik.deanName());
        ctx.put("dekanPos", zalik.deanPosition());
        ctx.put("dName", zalik.departmentName());
        ctx.put("A", zalik.a());
        ctx.put("B", zalik.b());
        ctx.put("C", zalik.c());
        ctx.put("D", zalik.d());
        ctx.put("E", zalik.e());
        ctx.put("Fx", zalik.fx());
        ctx.put("F", zalik.f());
        ctx.put("tI", zalik.gradeTeacher());
        ctx.put("students", zalik.students());
        return ctx;
    }
}
