package com.magadhexplora.api.catalog.pkg;

import jakarta.persistence.*;

@Entity
@Table(name = "package_images")
public class PackageImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private PackageEntity pkg;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "alt_text", length = 200)
    private String altText;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PackageEntity getPkg() { return pkg; }
    public void setPkg(PackageEntity pkg) { this.pkg = pkg; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getAltText() { return altText; }
    public void setAltText(String altText) { this.altText = altText; }

    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
