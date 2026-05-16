package com.magadhexplora.api.blog;

import com.magadhexplora.api.catalog.category.CategoryDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BlogDto {
    private Long id;

    @NotBlank @Size(max = 150)
    private String slug;

    @NotBlank @Size(max = 200)
    private String title;

    @Size(max = 500)
    private String excerpt;

    private String content;

    @Size(max = 500)
    private String coverImageUrl;

    @Size(max = 120)
    private String author;

    private Instant publishedAt;
    private boolean published = false;

    private Instant createdAt;
    private Instant updatedAt;

    /** Read-only: full categories the blog belongs to. */
    private List<CategoryDto> categories = new ArrayList<>();

    /** Write-only: IDs supplied by admin when saving. */
    private List<Long> categoryIds = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<CategoryDto> getCategories() { return categories; }
    public void setCategories(List<CategoryDto> categories) { this.categories = categories; }

    public List<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Long> categoryIds) { this.categoryIds = categoryIds; }

    public static BlogDto from(BlogEntity e) {
        BlogDto d = new BlogDto();
        d.id = e.getId();
        d.slug = e.getSlug();
        d.title = e.getTitle();
        d.excerpt = e.getExcerpt();
        d.content = e.getContent();
        d.coverImageUrl = e.getCoverImageUrl();
        d.author = e.getAuthor();
        d.publishedAt = e.getPublishedAt();
        d.published = e.isPublished();
        d.createdAt = e.getCreatedAt();
        d.updatedAt = e.getUpdatedAt();
        d.categories = (e.getCategories() == null ? new ArrayList<CategoryDto>() :
                e.getCategories().stream()
                        .map(CategoryDto::from)
                        .sorted(Comparator.comparing(CategoryDto::getName, Comparator.nullsLast(String::compareTo)))
                        .toList());
        return d;
    }

    public static BlogDto summary(BlogEntity e) {
        BlogDto d = from(e);
        d.content = null;
        return d;
    }

    public void apply(BlogEntity e) {
        e.setSlug(slug.trim().toLowerCase());
        e.setTitle(title.trim());
        e.setExcerpt(excerpt);
        e.setContent(content);
        e.setCoverImageUrl(coverImageUrl);
        e.setAuthor(author);
        if (published && e.getPublishedAt() == null) {
            e.setPublishedAt(Instant.now());
        }
        e.setPublished(published);
    }
}
