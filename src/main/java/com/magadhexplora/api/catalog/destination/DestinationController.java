package com.magadhexplora.api.catalog.destination;

import com.magadhexplora.api.i18n.ContentTranslationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@RestController
public class DestinationController {

    private final DestinationRepository repo;
    private final ContentTranslationService translations;

    public DestinationController(DestinationRepository repo,
                                 ContentTranslationService translations) {
        this.repo = repo;
        this.translations = translations;
    }

    @GetMapping("/api/destinations")
    public List<DestinationDto> list(@RequestParam(required = false, defaultValue = "true") boolean active,
                                     @RequestParam(required = false) String lang) {
        return repo.findByActiveOrderByNameAsc(active).stream()
                .map(e -> applyTranslations(DestinationDto.from(e), e.getId(), lang))
                .toList();
    }

    @GetMapping("/api/destinations/{slug}")
    public DestinationDto get(@PathVariable String slug,
                              @RequestParam(required = false) String lang) {
        DestinationEntity e = repo.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
        return applyTranslations(DestinationDto.from(e), e.getId(), lang);
    }

    @GetMapping("/api/admin/destinations")
    public List<DestinationDto> adminList() {
        return repo.findAllByOrderByNameAsc().stream().map(DestinationDto::from).toList();
    }

    @PostMapping("/api/admin/destinations")
    @ResponseStatus(HttpStatus.CREATED)
    public DestinationDto create(@Valid @RequestBody DestinationDto dto) {
        if (repo.existsBySlugIgnoreCase(dto.getSlug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists");
        }
        DestinationEntity e = new DestinationEntity();
        dto.apply(e);
        return DestinationDto.from(repo.save(e));
    }

    @PutMapping("/api/admin/destinations/{id}")
    public DestinationDto update(@PathVariable Long id, @Valid @RequestBody DestinationDto dto) {
        DestinationEntity e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
        dto.apply(e);
        return DestinationDto.from(repo.save(e));
    }

    @DeleteMapping("/api/admin/destinations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found");
        }
        repo.deleteById(id);
    }

    private DestinationDto applyTranslations(DestinationDto dto, Long id, String lang) {
        if (lang == null || lang.isBlank() || "en".equalsIgnoreCase(lang)) return dto;
        Map<String, Consumer<String>> setters = Map.of(
                "name", dto::setName,
                "description", dto::setDescription,
                "region", dto::setRegion
        );
        translations.apply(ContentTranslationService.DESTINATION, id, lang, setters);
        return dto;
    }
}
