package com.magadhexplora.api.blog;

import com.magadhexplora.api.catalog.category.CategoryEntity;
import com.magadhexplora.api.catalog.category.CategoryRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
public class BlogController {

    private final BlogRepository repo;
    private final CategoryRepository categoryRepo;

    public BlogController(BlogRepository repo, CategoryRepository categoryRepo) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
    }

    @GetMapping("/api/blogs")
    public List<BlogDto> list() {
        return repo.findByPublishedTrueOrderByPublishedAtDesc()
                .stream().map(BlogDto::summary).toList();
    }

    @GetMapping("/api/blogs/{slugOrId}")
    public BlogDto get(@PathVariable String slugOrId) {
        BlogEntity e = lookup(slugOrId);
        if (!e.isPublished()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog not found");
        }
        return BlogDto.from(e);
    }

    @GetMapping("/api/admin/blogs")
    public List<BlogDto> adminList() {
        return repo.findAllByOrderByCreatedAtDesc().stream().map(BlogDto::summary).toList();
    }

    @GetMapping("/api/admin/blogs/{id}")
    public BlogDto adminGet(@PathVariable Long id) {
        return repo.findById(id).map(BlogDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog not found"));
    }

    @PostMapping("/api/admin/blogs")
    @ResponseStatus(HttpStatus.CREATED)
    public BlogDto create(@Valid @RequestBody BlogDto dto) {
        if (repo.existsBySlugIgnoreCase(dto.getSlug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists");
        }
        BlogEntity e = new BlogEntity();
        dto.apply(e);
        applyCategories(e, dto.getCategoryIds());
        return BlogDto.from(repo.save(e));
    }

    @PutMapping("/api/admin/blogs/{id}")
    public BlogDto update(@PathVariable Long id, @Valid @RequestBody BlogDto dto) {
        BlogEntity e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog not found"));
        dto.apply(e);
        applyCategories(e, dto.getCategoryIds());
        return BlogDto.from(repo.save(e));
    }

    @DeleteMapping("/api/admin/blogs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog not found");
        }
        repo.deleteById(id);
    }

    private void applyCategories(BlogEntity e, List<Long> categoryIds) {
        if (categoryIds == null) {
            // null = leave existing categories alone; pass [] to clear
            return;
        }
        Set<CategoryEntity> resolved = new HashSet<>();
        if (!categoryIds.isEmpty()) {
            for (Long id : categoryIds) {
                CategoryEntity c = categoryRepo.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Unknown category id: " + id));
                resolved.add(c);
            }
        }
        e.setCategories(resolved);
    }

    private BlogEntity lookup(String slugOrId) {
        try {
            Long id = Long.parseLong(slugOrId);
            return repo.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog not found"));
        } catch (NumberFormatException ignored) {
            return repo.findBySlugIgnoreCase(slugOrId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog not found"));
        }
    }
}
