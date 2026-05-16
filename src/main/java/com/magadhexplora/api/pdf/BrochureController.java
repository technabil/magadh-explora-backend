package com.magadhexplora.api.pdf;

import com.magadhexplora.api.catalog.pkg.PackageEntity;
import com.magadhexplora.api.catalog.pkg.PackageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class BrochureController {

    private static final Logger log = LoggerFactory.getLogger(BrochureController.class);

    private final PackageRepository packageRepository;
    private final PackagePdfService pdfService;

    public BrochureController(PackageRepository packageRepository, PackagePdfService pdfService) {
        this.packageRepository = packageRepository;
        this.pdfService = pdfService;
    }

    @GetMapping(value = "/api/packages/{slug}/brochure.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> download(@PathVariable String slug) {
        PackageEntity pkg = packageRepository.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        if (!pkg.isPublished()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found");
        }
        try {
            byte[] pdf = pdfService.render(pkg);
            String filename = "Magadh-" + pkg.getSlug().replaceAll("[^a-zA-Z0-9-_]", "-") + ".pdf";
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(filename).build().toString())
                    .body(pdf);
        } catch (Exception ex) {
            log.error("Failed to render brochure for {}: {}", slug, ex.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate brochure");
        }
    }
}
