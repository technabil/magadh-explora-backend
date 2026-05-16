package com.magadhexplora.api.i18n;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Loads translations for a given entity and applies non-blank values to mutable target fields.
 *
 * Keep entity types as short stable strings (e.g. "package", "destination") — they're persisted in the
 * translations.entity_type column.
 */
@Service
public class ContentTranslationService {

    public static final String PACKAGE = "package";
    public static final String DESTINATION = "destination";

    private final TranslationRepository repo;

    public ContentTranslationService(TranslationRepository repo) {
        this.repo = repo;
    }

    /** Returns a field→value map for the given entity & language (only non-blank values). */
    public Map<String, String> loadMap(String entityType, Long entityId, String lang) {
        if (lang == null || lang.isBlank()) return Map.of();
        Map<String, String> out = new LinkedHashMap<>();
        for (TranslationEntity t : repo.findByEntityTypeAndEntityIdAndLang(entityType, entityId, lang)) {
            String v = t.getValue();
            if (v != null && !v.isBlank()) out.put(t.getField(), v);
        }
        return out;
    }

    /**
     * Apply translations for a single entity using the provided field→setter map.
     * The setter is invoked only if a non-blank translation exists for that field.
     */
    public void apply(String entityType, Long entityId, String lang, Map<String, Consumer<String>> setters) {
        if (lang == null || lang.isBlank() || entityId == null) return;
        Map<String, String> tr = loadMap(entityType, entityId, lang);
        if (tr.isEmpty()) return;
        for (var entry : setters.entrySet()) {
            String v = tr.get(entry.getKey());
            if (v != null && !v.isBlank()) entry.getValue().accept(v);
        }
    }

    /** Replace all field translations for entity+lang with the given map. Empty/blank values delete the row. */
    public Map<String, String> upsertMap(String entityType, Long entityId, String lang, Map<String, String> fields) {
        if (lang == null || lang.isBlank()) return Map.of();
        List<TranslationEntity> existing = repo.findByEntityTypeAndEntityIdAndLang(entityType, entityId, lang);
        Map<String, TranslationEntity> byField = new LinkedHashMap<>();
        for (TranslationEntity t : existing) byField.put(t.getField(), t);

        fields.forEach((field, value) -> {
            if (value == null || value.isBlank()) {
                TranslationEntity rm = byField.remove(field);
                if (rm != null) repo.delete(rm);
                return;
            }
            TranslationEntity t = byField.remove(field);
            if (t == null) {
                repo.save(new TranslationEntity(entityType, entityId, lang, field, value));
            } else {
                t.setValue(value);
                repo.save(t);
            }
        });
        return loadMap(entityType, entityId, lang);
    }
}
