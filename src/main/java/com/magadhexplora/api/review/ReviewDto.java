package com.magadhexplora.api.review;

import jakarta.validation.constraints.*;

import java.time.Instant;

public class ReviewDto {

    private Long id;

    @NotNull
    private Long packageId;

    @NotBlank @Size(max = 120)
    private String authorName;

    @Email @Size(max = 180)
    private String authorEmail;

    @NotNull @Min(1) @Max(5)
    private Integer rating;

    @Size(max = 180)
    private String title;

    @NotBlank @Size(max = 4000)
    private String body;

    private boolean approved;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static ReviewDto from(ReviewEntity e) {
        ReviewDto d = new ReviewDto();
        d.id = e.getId();
        d.packageId = e.getPackageId();
        d.authorName = e.getAuthorName();
        d.authorEmail = e.getAuthorEmail();
        d.rating = e.getRating();
        d.title = e.getTitle();
        d.body = e.getBody();
        d.approved = e.isApproved();
        d.createdAt = e.getCreatedAt();
        return d;
    }

    public ReviewEntity toEntity() {
        ReviewEntity e = new ReviewEntity();
        e.setPackageId(packageId);
        e.setAuthorName(authorName.trim());
        if (authorEmail != null) e.setAuthorEmail(authorEmail.trim().toLowerCase());
        e.setRating(rating);
        e.setTitle(title);
        e.setBody(body);
        return e;
    }
}
