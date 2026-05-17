package com.magadhexplora.api.settings;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class SettingsController {

    private final AppSettingRepository repo;

    public SettingsController(AppSettingRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/api/admin/settings")
    public Map<String, String> all() {
        Map<String, String> out = new LinkedHashMap<>();
        repo.findAll().forEach(s -> out.put(s.getKey(), s.getValue()));
        return out;
    }

    @PutMapping("/api/admin/settings")
    @Transactional
    public Map<String, String> upsert(@RequestBody Map<String, String> body) {
        body.forEach((k, v) -> {
            AppSettingEntity e = repo.findById(k).orElseGet(() -> {
                AppSettingEntity n = new AppSettingEntity();
                n.setKey(k);
                return n;
            });
            e.setValue(v);
            repo.save(e);
        });
        return all();
    }

    @GetMapping("/api/public/settings")
    public Map<String, String> publicSettings() {
        Map<String, String> out = new LinkedHashMap<>();
        repo.findAllById(java.util.List.of(
                "currency.default", "currency.allowed",
                "language.default", "language.allowed",
                "pricing.markup.percent",
                "theme.mode", "theme.primary", "theme.accent", "theme.gold",
                "whatsapp.enabled", "whatsapp.number", "whatsapp.default_message"
        )).forEach(s -> out.put(s.getKey(), s.getValue()));
        return out;
    }
}
