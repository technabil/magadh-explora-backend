package com.magadhexplora.api.settings;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "app_settings")
public class AppSettingEntity {

    @Id
    @Column(name = "setting_key", length = 120)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public AppSettingEntity() {}

    public AppSettingEntity(String key, String value, Instant updatedAt) {
        this.key = key;
        this.value = value;
        this.updatedAt = updatedAt;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
