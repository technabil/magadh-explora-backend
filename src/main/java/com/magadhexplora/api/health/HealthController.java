package com.magadhexplora.api.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final JdbcTemplate jdbc;
    private final String appName;

    public HealthController(JdbcTemplate jdbc,
                            @Value("${spring.application.name:magadh-explora}") String appName) {
        this.jdbc = jdbc;
        this.appName = appName;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "UP");
        out.put("service", appName);
        out.put("timestamp", Instant.now().toString());

        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            out.put("database", "UP");
        } catch (Exception ex) {
            out.put("status", "DEGRADED");
            out.put("database", "DOWN");
            out.put("databaseError", ex.getClass().getSimpleName());
        }
        return out;
    }
}
