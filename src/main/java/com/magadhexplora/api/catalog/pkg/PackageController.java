package com.magadhexplora.api.catalog.pkg;

import com.magadhexplora.api.catalog.pkg.dto.PackageDto;
import com.magadhexplora.api.catalog.pkg.dto.PackageRequest;
import com.magadhexplora.api.i18n.ContentTranslationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@RestController
public class PackageController {

    private final PackageRepository repo;
    private final PackageService service;
    private final ContentTranslationService translations;

    public PackageController(PackageRepository repo,
                             PackageService service,
                             ContentTranslationService translations) {
        this.repo = repo;
        this.service = service;
        this.translations = translations;
    }

    @GetMapping("/api/packages")
    @Transactional(readOnly = true)
    public List<PackageDto> publicList(@RequestParam(required = false) String q,
                                       @RequestParam(required = false) String mode,
                                       @RequestParam(required = false, name = "category") String categorySlug,
                                       @RequestParam(required = false) String travelerType,
                                       @RequestParam(required = false) Integer minDays,
                                       @RequestParam(required = false) Integer maxDays,
                                       @RequestParam(required = false) String lang) {
        String normMode = (mode == null || mode.isBlank()) ? null : mode.toUpperCase();
        String normCat  = (categorySlug == null || categorySlug.isBlank()) ? null : categorySlug.toLowerCase();
        List<PackageEntity> entities = repo.findPublic(emptyToNull(q), normMode, normCat, emptyToNull(travelerType), minDays, maxDays);
        return entities.stream()
                .map(e -> applyTranslations(PackageDto.from(e), e.getId(), lang))
                .toList();
    }

    @GetMapping("/api/packages/{slug}")
    @Transactional(readOnly = true)
    public PackageDto publicGet(@PathVariable String slug,
                                @RequestParam(required = false) String lang) {
        PackageEntity p = repo.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        if (!p.isPublished()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found");
        }
        return applyTranslations(PackageDto.from(p), p.getId(), lang);
    }

    @GetMapping("/api/admin/packages")
    @Transactional(readOnly = true)
    public List<PackageDto> adminList() {
        return repo.findAllForAdmin().stream().map(PackageDto::from).toList();
    }

    @GetMapping("/api/admin/packages/{id}")
    @Transactional(readOnly = true)
    public PackageDto adminGet(@PathVariable Long id) {
        return PackageDto.from(repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found")));
    }

    @PostMapping("/api/admin/packages")
    @ResponseStatus(HttpStatus.CREATED)
    public PackageDto create(@Valid @RequestBody PackageRequest req) {
        return PackageDto.from(service.create(req));
    }

    @PutMapping("/api/admin/packages/{id}")
    public PackageDto update(@PathVariable Long id, @Valid @RequestBody PackageRequest req) {
        return PackageDto.from(service.update(id, req));
    }

    @DeleteMapping("/api/admin/packages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    private PackageDto applyTranslations(PackageDto dto, Long id, String lang) {
        if (lang == null || lang.isBlank() || "en".equalsIgnoreCase(lang)) return dto;
        Map<String, Consumer<String>> setters = Map.of(
                "title", dto::setTitle,
                "summary", dto::setSummary,
                "description", dto::setDescription,
                "itinerary", dto::setItinerary,
                "inclusions", dto::setInclusions,
                "exclusions", dto::setExclusions
        );
        translations.apply(ContentTranslationService.PACKAGE, id, lang, setters);
        return dto;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
