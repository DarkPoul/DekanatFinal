package com.esvar.dekanat.document;

import com.esvar.dekanat.document.PdfGenerator;
import java.util.Map;
import com.esvar.dekanat.document.DocumentGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Facade service for generating documents.
 */
@Service
public class DocumentGenerationService {

    private static final Logger log = LoggerFactory.getLogger(DocumentGenerationService.class);

    private final Map<String, DocumentGenerator> generators;
    private final Map<String, PdfGenerator> pdfGenerators;
    private final DocumentTemplateEngine engine;

    public DocumentGenerationService(List<DocumentGenerator> generators, List<PdfGenerator> pdfGenerators) {
        this.generators = generators.stream().collect(Collectors.toMap(DocumentGenerator::getName, g -> g));
        this.pdfGenerators = pdfGenerators.stream().collect(Collectors.toMap(PdfGenerator::getName, g -> g));
        this.engine = new DocumentTemplateEngine();
    }

    /**
     * Generate document using selected generator.
     *
     * @param name generator name
     * @param data source data
     * @return path to generated document
     */
    public Path generate(String name, Object data) {
        PdfGenerator pdfGen = pdfGenerators.get(name);
        if (pdfGen != null) {
            log.info("Generating PDF document {}", name);
            return pdfGen.generatePdf(data);
        }

        DocumentGenerator generator = generators.get(name);
        if (generator == null) {
            throw new MissingTemplateException("Generator not found: " + name);
        }
        Map<String, Object> context = generator.prepareContext(data);
        log.info("Generating document {}", name);

        String templatePath = generator.resolveTemplatePath(data);
        return engine.generate(templatePath, context);
    }
}
