package com.magadhexplora.api.homepage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class HomepageSectionDto {
    private Long id;

    @NotBlank @Size(max = 80)
    private String sectionKey;

    @NotBlank @Size(max = 150)
    private String title;

    private int displayOrder = 0;
    private boolean active = true;
    private int maxItems = 6;

    private List<HomepageSectionItemDto> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSectionKey() { return sectionKey; }
    public void setSectionKey(String sectionKey) { this.sectionKey = sectionKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getMaxItems() { return maxItems; }
    public void setMaxItems(int maxItems) { this.maxItems = maxItems; }

    public List<HomepageSectionItemDto> getItems() { return items; }
    public void setItems(List<HomepageSectionItemDto> items) { this.items = items; }

    public static HomepageSectionDto from(HomepageSectionEntity e, boolean includeItems) {
        HomepageSectionDto d = new HomepageSectionDto();
        d.id = e.getId();
        d.sectionKey = e.getSectionKey();
        d.title = e.getTitle();
        d.displayOrder = e.getDisplayOrder();
        d.active = e.isActive();
        d.maxItems = e.getMaxItems();
        if (includeItems && e.getItems() != null) {
            d.items = e.getItems().stream()
                    .map(HomepageSectionItemDto::from)
                    .toList();
        }
        return d;
    }

    public void apply(HomepageSectionEntity e) {
        e.setSectionKey(sectionKey.trim().toLowerCase());
        e.setTitle(title.trim());
        e.setDisplayOrder(displayOrder);
        e.setActive(active);
        e.setMaxItems(Math.max(1, maxItems));
    }
}
