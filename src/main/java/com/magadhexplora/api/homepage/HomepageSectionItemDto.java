package com.magadhexplora.api.homepage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class HomepageSectionItemDto {
    private Long id;

    @NotBlank
    private String entityType;

    @NotNull
    private Long entityId;

    private int displayOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public static HomepageSectionItemDto from(HomepageSectionItemEntity e) {
        HomepageSectionItemDto d = new HomepageSectionItemDto();
        d.id = e.getId();
        d.entityType = e.getEntityType();
        d.entityId = e.getEntityId();
        d.displayOrder = e.getDisplayOrder();
        return d;
    }
}
