package com.magadhexplora.api.catalog.pkg;

import com.magadhexplora.api.catalog.category.CategoryEntity;
import com.magadhexplora.api.catalog.destination.DestinationEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "packages")
public class PackageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_inr", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceInr = BigDecimal.ZERO;

    @Column(name = "original_price_inr", precision = 12, scale = 2)
    private BigDecimal originalPriceInr;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "reviews_count", nullable = false)
    private int reviewsCount = 0;

    @Column(name = "group_size_min")
    private Integer groupSizeMin;

    @Column(name = "group_size_max")
    private Integer groupSizeMax;

    @Column(name = "hero_image_url", length = 500)
    private String heroImageUrl;

    @Column(nullable = false, length = 20)
    private String mode = "HOLIDAY";

    @Column(name = "traveler_types", length = 255)
    private String travelerTypes;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String itinerary;

    @Column(columnDefinition = "TEXT")
    private String inclusions;

    @Column(columnDefinition = "TEXT")
    private String exclusions;

    @Column(name = "is_published", nullable = false)
    private boolean published = false;

    @Column(name = "is_featured", nullable = false)
    private boolean featured = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "package_categories",
            joinColumns = @JoinColumn(name = "package_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<CategoryEntity> categories = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "package_destinations",
            joinColumns = @JoinColumn(name = "package_id"),
            inverseJoinColumns = @JoinColumn(name = "destination_id"))
    private Set<DestinationEntity> destinations = new HashSet<>();

    @OneToMany(mappedBy = "pkg", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PackageImageEntity> images = new ArrayList<>();

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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Set<CategoryEntity> getCategories() { return categories; }
    public void setCategories(Set<CategoryEntity> categories) { this.categories = categories; }

    public Set<DestinationEntity> getDestinations() { return destinations; }
    public void setDestinations(Set<DestinationEntity> destinations) { this.destinations = destinations; }

    public List<PackageImageEntity> getImages() { return images; }
    public void setImages(List<PackageImageEntity> images) { this.images = images; }
}
