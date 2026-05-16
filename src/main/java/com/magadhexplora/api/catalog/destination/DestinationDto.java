package com.magadhexplora.api.catalog.destination;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class DestinationDto {
    private Long id;

    @NotBlank @Size(max = 80)
    private String slug;

    @NotBlank @Size(max = 150)
    private String name;

    @Size(max = 120)
    private String region;

    private String description;
    private String heroImageUrl;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getHeroImageUrl() { return heroImageUrl; }
    public void setHeroImageUrl(String heroImageUrl) { this.heroImageUrl = heroImageUrl; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public static DestinationDto from(DestinationEntity e) {
        DestinationDto d = new DestinationDto();
        d.id = e.getId();
        d.slug = e.getSlug();
        d.name = e.getName();
        d.region = e.getRegion();
        d.description = e.getDescription();
        d.heroImageUrl = e.getHeroImageUrl();
        d.latitude = e.getLatitude();
        d.longitude = e.getLongitude();
        d.active = e.isActive();
        return d;
    }

    public void apply(DestinationEntity e) {
        e.setSlug(slug.trim().toLowerCase());
        e.setName(name.trim());
        e.setRegion(region);
        e.setDescription(description);
        e.setHeroImageUrl(heroImageUrl);
        e.setLatitude(latitude);
        e.setLongitude(longitude);
        e.setActive(active);
    }
}
