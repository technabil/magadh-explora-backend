package com.magadhexplora.api.catalog.category;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class CategoryController {

    private final CategoryRepository repo;

    public CategoryController(CategoryRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/api/categories")
    public List<CategoryDto> list(@RequestParam(required = false) String kind) {
        List<CategoryEntity> rows = (kind == null || kind.isBlank())
                ? repo.findAllByOrderByKindAscDisplayOrderAsc()
                : repo.findByKindOrderByDisplayOrderAsc(kind.toUpperCase());
        return rows.stream().map(CategoryDto::from).toList();
    }

    @PostMapping("/api/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@Valid @RequestBody CategoryDto dto) {
        if (repo.existsBySlugIgnoreCase(dto.getSlug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists");
        }
        CategoryEntity e = new CategoryEntity();
        dto.apply(e);
        return CategoryDto.from(repo.save(e));
    }

    @PutMapping("/api/admin/categories/{id}")
    public CategoryDto update(@PathVariable Long id, @Valid @RequestBody CategoryDto dto) {
        CategoryEntity e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        dto.apply(e);
        return CategoryDto.from(repo.save(e));
    }

    @DeleteMapping("/api/admin/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        repo.deleteById(id);
    }
}
