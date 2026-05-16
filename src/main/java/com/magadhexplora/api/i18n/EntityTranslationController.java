package com.magadhexplora.api.i18n;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

/**
 * Admin CRUD for translations attached to a specific entity (a package or destination).
 *
 *   GET  /api/admin/translations/entity/{type}/{id}/{lang}   → field→value map
 *   PUT  /api/admin/translations/entity/{type}/{id}/{lang}   ← field→value map (upsert; "" / null deletes)
 */
@RestController
@RequestMapping("/api/admin/translations/entity")
public class EntityTranslationController {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            ContentTranslationService.PACKAGE,
            ContentTranslationService.DESTINATION
    );

    private final ContentTranslationService service;

    public EntityTranslationController(ContentTranslationService service) {
        this.service = service;
    }

    @GetMapping("/{type}/{id}/{lang}")
    public Map<String, String> get(@PathVariable String type,
                                   @PathVariable Long id,
                                   @PathVariable String lang) {
        validate(type, lang);
        return service.loadMap(type, id, lang);
    }

    @PutMapping("/{type}/{id}/{lang}")
    public Map<String, String> put(@PathVariable String type,
                                   @PathVariable Long id,
                                   @PathVariable String lang,
                                   @RequestBody Map<String, String> fields) {
        validate(type, lang);
        if (fields == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body must be a field→value map");
        }
        return service.upsertMap(type, id, lang, fields);
    }

    private static void validate(String type, String lang) {
        if (!ALLOWED_TYPES.contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported entity type: " + type);
        }
        if (lang == null || lang.isBlank() || lang.length() > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid language code");
        }
    }
}
