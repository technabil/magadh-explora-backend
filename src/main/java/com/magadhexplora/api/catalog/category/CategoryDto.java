package com.magadhexplora.api.catalog.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryDto {
    private Long id;

    @NotBlank @Size(max = 80)
    private String slug;

    @NotBlank @Size(max = 20)
    private String kind;

    @NotBlank @Size(max = 120)
    private String name;

    private String description;
    private int displayOrder;
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public static CategoryDto from(CategoryEntity e) {
        CategoryDto d = new CategoryDto();
        d.id = e.getId();
        d.slug = e.getSlug();
        d.kind = e.getKind();
        d.name = e.getName();
        d.description = e.getDescription();
        d.displayOrder = e.getDisplayOrder();
        d.active = e.isActive();
        return d;
    }

    public void apply(CategoryEntity e) {
        e.setSlug(slug.trim().toLowerCase());
        e.setKind(kind.trim().toUpperCase());
        e.setName(name.trim());
        e.setDescription(description);
        e.setDisplayOrder(displayOrder);
        e.setActive(active);
    }
}
