package com.magadhexplora.api.catalog.pkg.dto;

import com.magadhexplora.api.catalog.pkg.PackageImageEntity;
import jakarta.validation.constraints.NotBlank;

public class PackageImageDto {
    private Long id;

    @NotBlank
    private String url;

    private String altText;
    private boolean primary;
    private int displayOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getAltText() { return altText; }
    public void setAltText(String altText) { this.altText = altText; }

    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public static PackageImageDto from(PackageImageEntity e) {
        PackageImageDto d = new PackageImageDto();
        d.id = e.getId();
        d.url = e.getUrl();
        d.altText = e.getAltText();
        d.primary = e.isPrimary();
        d.displayOrder = e.getDisplayOrder();
        return d;
    }
}
