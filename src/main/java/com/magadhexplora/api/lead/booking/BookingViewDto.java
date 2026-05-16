package com.magadhexplora.api.lead.booking;

import com.magadhexplora.api.catalog.pkg.PackageEntity;
import com.magadhexplora.api.catalog.pkg.PackageImageEntity;

/** Public booking view returned by the magic-link endpoint. Includes package summary. */
public class BookingViewDto {

    private BookingDto booking;
    private PackageSummary pkg;

    public BookingDto getBooking() { return booking; }
    public void setBooking(BookingDto booking) { this.booking = booking; }

    public PackageSummary getPkg() { return pkg; }
    public void setPkg(PackageSummary pkg) { this.pkg = pkg; }

    public static BookingViewDto from(BookingEntity bookingEntity, PackageEntity packageEntity) {
        BookingViewDto v = new BookingViewDto();
        v.booking = BookingDto.from(bookingEntity);
        if (packageEntity != null) {
            PackageSummary ps = new PackageSummary();
            ps.id = packageEntity.getId();
            ps.slug = packageEntity.getSlug();
            ps.title = packageEntity.getTitle();
            ps.summary = packageEntity.getSummary();
            ps.durationDays = packageEntity.getDurationDays();
            ps.heroImageUrl = packageEntity.getHeroImageUrl() != null
                    ? packageEntity.getHeroImageUrl()
                    : packageEntity.getImages().stream()
                            .filter(PackageImageEntity::isPrimary)
                            .map(PackageImageEntity::getUrl)
                            .findFirst()
                            .orElseGet(() -> packageEntity.getImages().isEmpty()
                                    ? null
                                    : packageEntity.getImages().get(0).getUrl());
            v.pkg = ps;
        }
        return v;
    }

    public static class PackageSummary {
        private Long id;
        private String slug;
        private String title;
        private String summary;
        private Integer durationDays;
        private String heroImageUrl;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public Integer getDurationDays() { return durationDays; }
        public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }

        public String getHeroImageUrl() { return heroImageUrl; }
        public void setHeroImageUrl(String heroImageUrl) { this.heroImageUrl = heroImageUrl; }
    }
}
