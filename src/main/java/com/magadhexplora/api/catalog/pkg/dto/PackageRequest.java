package com.magadhexplora.api.catalog.pkg.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public class PackageRequest {

    @NotBlank @Size(max = 120)
    private String slug;

    @NotBlank @Size(max = 200)
    private String title;

    @Size(max = 500)
    private String summary;

    private String description;

    @NotNull @PositiveOrZero
    private BigDecimal priceInr;

    @PositiveOrZero
    private BigDecimal originalPriceInr;

    @Positive
    private Integer durationDays;

    @DecimalMin("0.0") @DecimalMax("5.0")
    private BigDecimal rating;

    @PositiveOrZero
    private Integer reviewsCount;

    @Positive
    private Integer groupSizeMin;

    @Positive
    private Integer groupSizeMax;

    @Size(max = 500)
    private String heroImageUrl;

    @NotBlank
    private String mode;

    private String travelerTypes;

    private String itinerary;
    private String inclusions;
    private String exclusions;

    private boolean published;
    private boolean featured;

    private List<Long> categoryIds;
    private List<Long> destinationIds;

    @Valid
    private List<PackageImageDto> images;

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPriceInr() { return priceInr; }
    public void setPriceInr(BigDecimal priceInr) { this.priceInr = priceInr; }

    public BigDecimal getOriginalPriceInr() { return originalPriceInr; }
    public void setOriginalPriceInr(BigDecimal originalPriceInr) { this.originalPriceInr = originalPriceInr; }

    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public Integer getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(Integer reviewsCount) { this.reviewsCount = reviewsCount; }

    public Integer getGroupSizeMin() { return groupSizeMin; }
    public void setGroupSizeMin(Integer groupSizeMin) { this.groupSizeMin = groupSizeMin; }

    public Integer getGroupSizeMax() { return groupSizeMax; }
    public void setGroupSizeMax(Integer groupSizeMax) { this.groupSizeMax = groupSizeMax; }

    public String getHeroImageUrl() { return heroImageUrl; }
    public void setHeroImageUrl(String heroImageUrl) { this.heroImageUrl = heroImageUrl; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getTravelerTypes() { return travelerTypes; }
    public void setTravelerTypes(String travelerTypes) { this.travelerTypes = travelerTypes; }

    public String getItinerary() { return itinerary; }
    public void setItinerary(String itinerary) { this.itinerary = itinerary; }

    public String getInclusions() { return inclusions; }
    public void setInclusions(String inclusions) { this.inclusions = inclusions; }

    public String getExclusions() { return exclusions; }
    public void setExclusions(String exclusions) { this.exclusions = exclusions; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public List<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Long> categoryIds) { this.categoryIds = categoryIds; }

    public List<Long> getDestinationIds() { return destinationIds; }
    public void setDestinationIds(List<Long> destinationIds) { this.destinationIds = destinationIds; }

    public List<PackageImageDto> getImages() { return images; }
    public void setImages(List<PackageImageDto> images) { this.images = images; }
}
