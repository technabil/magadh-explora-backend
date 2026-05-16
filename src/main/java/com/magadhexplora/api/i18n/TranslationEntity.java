package com.magadhexplora.api.i18n;

import jakarta.persistence.*;

@Entity
@Table(name = "translations")
public class TranslationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 8)
    private String lang;

    @Column(nullable = false, length = 40)
    private String field;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String value;

    public TranslationEntity() {}

    public TranslationEntity(String entityType, Long entityId, String lang, String field, String value) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.lang = lang;
        this.field = field;
        this.value = value;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
