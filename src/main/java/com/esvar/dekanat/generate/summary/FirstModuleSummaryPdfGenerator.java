package com.esvar.dekanat.generate.summary;

import com.esvar.dekanat.document.DocumentException;
import com.esvar.dekanat.document.PdfGenerator;
import com.esvar.dekanat.entity.ControlMethodEntity;
import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.repository.ControlMethodRepository;
import com.esvar.dekanat.repository.GroupRepository;
import com.esvar.dekanat.service.MarksService;
import com.esvar.dekanat.service.PlanService;
import com.esvar.dekanat.service.StudentService;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * PDF generator for the first module summary report.
 */
@Component
public class FirstModuleSummaryPdfGenerator implements PdfGenerator {

    public static final String NAME = "first-module-summary";

    private final GroupRepository groupRepository;
    private final PlanService planService;
    private final StudentService studentService;
    private final MarksService marksService;
    private final ControlMethodRepository controlMethodRepository;

    public FirstModuleSummaryPdfGenerator(GroupRepository groupRepository,
                                          PlanService planService,
                                          StudentService studentService,
                                          MarksService marksService, ControlMethodRepository controlMethodRepository) {
        this.groupRepository = groupRepository;
        this.planService = planService;
        this.studentService = studentService;
        this.marksService = marksService;
        this.controlMethodRepository = controlMethodRepository;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Path generatePdf(Object data) {
        if (!(data instanceof FirstModuleSummaryRequest request)) {
            throw new DocumentException("Expected FirstModuleSummaryRequest");
        }

        FirstModuleSummaryData summary = buildSummary(request);

        try {
            Path pdfPath = Files.createTempFile("module_summary_", ".pdf");
            try (PdfWriter writer = new PdfWriter(pdfPath.toFile());
                 PdfDocument pdfDoc = new PdfDocument(writer);
                 Document document = new Document(pdfDoc)) {

                PdfFont font = loadFont();
                FirstModuleSummaryTable.generate(document, font, summary);
            }
            return pdfPath;
        } catch (IOException e) {
            throw new DocumentException("Failed to generate PDF", e);
        }
    }

    private PdfFont loadFont() throws IOException {
        try (InputStream fontStream = FirstModuleSummaryPdfGenerator.class.getResourceAsStream("/fonts/times.ttf")) {
            if (fontStream == null) {
                throw new IOException("Font resource /fonts/times.ttf not found");
            }
            FontProgram fp = FontProgramFactory.createFont(fontStream.readAllBytes());
            return PdfFontFactory.createFont(fp, PdfEncodings.IDENTITY_H);
        }
    }

    private FirstModuleSummaryData buildSummary(FirstModuleSummaryRequest request) {
        if (request.groupId() == null) {
            throw new DocumentException("Group identifier must be provided");
        }
        if (request.controlType() == null || request.controlType().isBlank()) {
            throw new DocumentException("Control type must be provided");
        }

        StudentGroupEntity group = groupRepository.findById(request.groupId())
                .orElseThrow(() -> new DocumentException("Group not found: " + request.groupId()));

        Collator collator = Collator.getInstance(new Locale("uk", "UA"));
        Comparator<PlansEntity> byDiscipline = Comparator.comparing(
                plan -> plan.getDiscipline() != null ? plan.getDiscipline().getTitle() : "",
                collator
        );

        ControlMethodEntity controlMethod = resolveControlMethod(request.controlType());

        List<PlansEntity> plans = planService.getAllPlansForGroupAndSemester(group, request.semester()).stream()
                .filter(plan -> !plan.isElective())
                .filter(plan -> matchesControlType(plan, controlMethod))
                .sorted(byDiscipline)
                .toList();

        List<String> disciplines = plans.stream()
                .map(plan -> plan.getDiscipline().getTitle())
                .toList();

        List<StudentEntity> students = studentService.getStudentByGroupId(group.getId());
        List<FirstModuleSummaryRow> rows = new ArrayList<>();

        for (StudentEntity student : students) {
            List<Integer> marks = new ArrayList<>();
            for (PlansEntity plan : plans) {
                marks.add(resolveMark(student, plan, request.controlType()));
            }
            rows.add(new FirstModuleSummaryRow(student.getFullName(), marks));
        }

        return new FirstModuleSummaryData(group.getGroupCode(),
                request.controlType(), disciplines, rows);
    }

    private int resolveMark(StudentEntity student, PlansEntity plan, String controlType) {
        try {
            String value = marksService.getMarkForFirstModalControl(student, plan, controlType);
            if (value == null || value.isBlank()) {
                return 0;
            }
            return Integer.parseInt(value.trim());
        } catch (Exception ex) {
            return 0;
        }
    }

    private boolean matchesControlType(PlansEntity plan, ControlMethodEntity controlMethod) {
        if (plan == null || controlMethod == null) {
            return false;
        }
        Long controlId = controlMethod.getId();
        return hasControl(plan.getFirstControl(), controlId)
                || hasControl(plan.getSecondControl(), controlId);
    }

    private boolean hasControl(ControlMethodEntity control, Long controlId) {
        return control != null && control.getId() != null && control.getId().equals(controlId);
    }

    private ControlMethodEntity resolveControlMethod(String controlType) {
        if (controlType == null || controlType.isBlank()) {
            throw new DocumentException("Control type must be provided");
        }
        ControlMethodEntity controlMethod = controlMethodRepository.findByName(controlType);
        if (controlMethod == null) {
            throw new DocumentException("Control method not found: " + controlType);
        }
        return controlMethod;
    }
}
