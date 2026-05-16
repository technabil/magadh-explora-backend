package com.magadhexplora.api.i18n;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class UiTranslationController {

    private static final String UI_TYPE = "UI";
    private static final Long UI_ID = 0L;
    private static final int MAX_KEY_LEN = 40;

    private final TranslationRepository repo;

    public UiTranslationController(TranslationRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/api/translations/{lang}")
    public Map<String, String> publicGet(@PathVariable String lang) {
        return loadAll(lang);
    }

    @GetMapping("/api/admin/translations/{lang}")
    public Map<String, String> adminGet(@PathVariable String lang) {
        return loadAll(lang);
    }

    @PutMapping("/api/admin/translations/{lang}")
    @Transactional
    public Map<String, String> adminSave(@PathVariable String lang,
                                         @RequestBody Map<String, String> entries) {
        if (entries == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body required");
        }
        entries.forEach((key, value) -> {
            if (key == null || key.isBlank()) return;
            if (key.length() > MAX_KEY_LEN) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Translation key exceeds " + MAX_KEY_LEN + " chars: " + key);
            }
            if (value == null || value.isBlank()) {
                repo.deleteByEntityTypeAndEntityIdAndLangAndField(UI_TYPE, UI_ID, lang, key);
                return;
            }
            TranslationEntity t = repo
                    .findByEntityTypeAndEntityIdAndLangAndField(UI_TYPE, UI_ID, lang, key)
                    .orElseGet(() -> new TranslationEntity(UI_TYPE, UI_ID, lang, key, value));
            t.setValue(value);
            repo.save(t);
        });
        return loadAll(lang);
    }

    @DeleteMapping("/api/admin/translations/{lang}/{key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteKey(@PathVariable String lang, @PathVariable String key) {
        repo.deleteByEntityTypeAndEntityIdAndLangAndField(UI_TYPE, UI_ID, lang, key);
    }

    @PostMapping("/api/admin/translations/{lang}/entry")
    public Map<String, String> adminUpsertOne(@PathVariable String lang,
                                              @Valid @RequestBody UpsertEntry req) {
        Map<String, String> single = new LinkedHashMap<>();
        single.put(req.key(), req.value() == null ? "" : req.value());
        return adminSave(lang, single);
    }

    public record UpsertEntry(
            @NotBlank @Size(max = MAX_KEY_LEN) String key,
            String value) {}

    private Map<String, String> loadAll(String lang) {
        Map<String, String> out = new LinkedHashMap<>();
        for (TranslationEntity t : repo.findByEntityTypeAndEntityIdAndLang(UI_TYPE, UI_ID, lang)) {
            out.put(t.getField(), t.getValue() == null ? "" : t.getValue());
        }
        return out;
    }
}
