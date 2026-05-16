package com.magadhexplora.api.catalog.pkg.dto;

import com.magadhexplora.api.catalog.category.CategoryDto;
import com.magadhexplora.api.catalog.destination.DestinationDto;
import com.magadhexplora.api.catalog.pkg.PackageEntity;
import com.magadhexplora.api.catalog.pkg.PackageImageEntity;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public class PackageDto {
    private Long id;
    private String slug;
    private String title;
    private String summary;
    private String description;

    private BigDecimal priceInr;
    private BigDecimal originalPriceInr;

    private Integer durationDays;
    private BigDecimal rating;
    private int reviewsCount;
    private Integer groupSizeMin;
    private Integer groupSizeMax;
    private String heroImageUrl;

    private String mode;
    private String travelerTypes;

    private String itinerary;
    private String inclusions;
    private String exclusions;

    private boolean published;
    private boolean featured;

    private List<CategoryDto> categories;
    private List<DestinationDto> destinations;
    private List<PackageImageDto> images;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public int getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(int reviewsCount) { this.reviewsCount = reviewsCount; }

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

    public List<CategoryDto> getCategories() { return categories; }
    public void setCategories(List<CategoryDto> categories) { this.categories = categories; }

    public List<DestinationDto> getDestinations() { return destinations; }
    public void setDestinations(List<DestinationDto> destinations) { this.destinations = destinations; }

    public List<PackageImageDto> getImages() { return images; }
    public void setImages(List<PackageImageDto> images) { this.images = images; }

    public static PackageDto from(PackageEntity p) {
        PackageDto d = new PackageDto();
        d.id = p.getId();
        d.slug = p.getSlug();
        d.title = p.getTitle();
        d.summary = p.getSummary();
        d.description = p.getDescription();
        d.priceInr = p.getPriceInr();
        d.originalPriceInr = p.getOriginalPriceInr();
        d.durationDays = p.getDurationDays();
        d.rating = p.getRating();
        d.reviewsCount = p.getReviewsCount();
        d.groupSizeMin = p.getGroupSizeMin();
        d.groupSizeMax = p.getGroupSizeMax();
        d.heroImageUrl = p.getHeroImageUrl();
        d.mode = p.getMode();
        d.travelerTypes = p.getTravelerTypes();
        d.itinerary = p.getItinerary();
        d.inclusions = p.getInclusions();
        d.exclusions = p.getExclusions();
        d.published = p.isPublished();
        d.featured = p.isFeatured();

        d.categories = p.getCategories().stream().map(CategoryDto::from).toList();
        d.destinations = p.getDestinations().stream().map(DestinationDto::from).toList();
        d.images = p.getImages().stream()
                .sorted(Comparator.comparingInt(PackageImageEntity::getDisplayOrder))
                .map(PackageImageDto::from)
                .toList();
        return d;
    }
}
