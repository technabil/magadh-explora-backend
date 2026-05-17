package com.magadhexplora.api.homepage;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
public class HomepageController {

    private static final Set<String> ENTITY_TYPES = Set.of("PACKAGE", "DESTINATION", "BLOG");

    private final HomepageSectionRepository repo;

    public HomepageController(HomepageSectionRepository repo) {
        this.repo = repo;
    }

    /** Public: active sections with item refs. */
    @GetMapping("/api/homepage")
    @Transactional(readOnly = true)
    public List<HomepageSectionDto> publicLayout() {
        return repo.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(s -> HomepageSectionDto.from(s, true))
                .toList();
    }

    @GetMapping("/api/admin/homepage")
    @Transactional(readOnly = true)
    public List<HomepageSectionDto> adminLayout() {
        return repo.findAllByOrderByDisplayOrderAsc().stream()
                .map(s -> HomepageSectionDto.from(s, true))
                .toList();
    }

    @PostMapping("/api/admin/homepage")
    @ResponseStatus(HttpStatus.CREATED)
    public HomepageSectionDto createSection(@Valid @RequestBody HomepageSectionDto dto) {
        if (repo.existsBySectionKey(dto.getSectionKey().trim().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Section key already exists");
        }
        HomepageSectionEntity e = new HomepageSectionEntity();
        dto.apply(e);
        return HomepageSectionDto.from(repo.save(e), true);
    }

    @PutMapping("/api/admin/homepage/{id}")
    public HomepageSectionDto updateSection(@PathVariable Long id, @Valid @RequestBody HomepageSectionDto dto) {
        HomepageSectionEntity e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        dto.apply(e);
        return HomepageSectionDto.from(repo.save(e), true);
    }

    @DeleteMapping("/api/admin/homepage/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found");
        }
        repo.deleteById(id);
    }

    /** Replace the entire item list for a section. Frontend sends the ordered array. */
    @PutMapping("/api/admin/homepage/{id}/items")
    @Transactional
    public HomepageSectionDto replaceItems(@PathVariable Long id,
                                           @Valid @RequestBody List<HomepageSectionItemDto> items) {
        HomepageSectionEntity section = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));

        section.getItems().clear();

        List<HomepageSectionItemEntity> fresh = new ArrayList<>();
        int order = 0;
        for (HomepageSectionItemDto in : items) {
            String type = in.getEntityType() == null ? "" : in.getEntityType().toUpperCase();
            if (!ENTITY_TYPES.contains(type)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid entity type: " + in.getEntityType());
            }
            HomepageSectionItemEntity item = new HomepageSectionItemEntity();
            item.setSection(section);
            item.setEntityType(type);
            item.setEntityId(in.getEntityId());
            item.setDisplayOrder(order++);
            fresh.add(item);
        }
        section.getItems().addAll(fresh);

        return HomepageSectionDto.from(repo.save(section), true);
    }
}
