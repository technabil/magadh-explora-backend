package com.magadhexplora.api.currency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magadhexplora.api.settings.AppSettingEntity;
import com.magadhexplora.api.settings.AppSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyService.class);

    private final CurrencyRateRepository rateRepo;
    private final AppSettingRepository settingsRepo;

    private final RestClient http = RestClient.builder()
            .baseUrl("https://open.er-api.com")
            .build();

    public CurrencyService(CurrencyRateRepository rateRepo, AppSettingRepository settingsRepo) {
        this.rateRepo = rateRepo;
        this.settingsRepo = settingsRepo;
    }

    @Transactional(readOnly = true)
    public CurrencyResponse snapshot() {
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        Instant latest = Instant.EPOCH;
        for (CurrencyRateEntity r : rateRepo.findAll()) {
            rates.put(r.getCurrencyCode(), r.getRateToInr());
            if (r.getUpdatedAt() != null && r.getUpdatedAt().isAfter(latest)) latest = r.getUpdatedAt();
        }

        BigDecimal markup = readDecimal("pricing.markup.percent", BigDecimal.ZERO);
        String defaultCurrency = readString("currency.default", "INR");
        List<String> allowed = csv(readString("currency.allowed", "INR"));
        return new CurrencyResponse(rates, markup, defaultCurrency, allowed, latest);
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledRefresh() {
        try {
            int n = refresh();
            log.info("Currency rates refreshed: {} rows", n);
        } catch (Exception ex) {
            log.warn("Scheduled currency refresh failed: {}", ex.toString());
        }
    }

    @Transactional
    public int refresh() {
        try {
            String body = http.get().uri("/v6/latest/INR").retrieve().body(String.class);
            JsonNode json = new ObjectMapper().readTree(body);
            if (!"success".equalsIgnoreCase(json.path("result").asText())) {
                log.warn("er-api returned non-success: {}", body);
                return 0;
            }
            List<String> allowed = csv(readString("currency.allowed", "INR,USD,EUR,GBP,JPY"));
            Set<String> wanted = new HashSet<>(allowed);
            wanted.add("INR");

            JsonNode rates = json.path("rates");
            int n = 0;
            for (Iterator<Map.Entry<String, JsonNode>> it = rates.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> e = it.next();
                String code = e.getKey();
                if (!wanted.contains(code)) continue;
                double v = e.getValue().asDouble(0d);
                if (v <= 0) continue;
                BigDecimal rateToInr = code.equals("INR")
                        ? BigDecimal.ONE
                        : BigDecimal.ONE.divide(BigDecimal.valueOf(v), MathContext.DECIMAL64)
                                        .setScale(8, RoundingMode.HALF_UP);

                CurrencyRateEntity row = rateRepo.findById(code).orElseGet(() -> {
                    CurrencyRateEntity r = new CurrencyRateEntity();
                    r.setCurrencyCode(code);
                    return r;
                });
                row.setRateToInr(rateToInr);
                rateRepo.save(row);
                n++;
            }
            rateRepo.findById("INR").orElseGet(() -> {
                CurrencyRateEntity r = new CurrencyRateEntity();
                r.setCurrencyCode("INR");
                r.setRateToInr(BigDecimal.ONE);
                return rateRepo.save(r);
            });
            return n;
        } catch (Exception ex) {
            log.warn("Currency refresh failed: {}", ex.toString());
            return 0;
        }
    }

    private String readString(String key, String fallback) {
        return settingsRepo.findById(key).map(AppSettingEntity::getValue).orElse(fallback);
    }

    private BigDecimal readDecimal(String key, BigDecimal fallback) {
        try {
            return new BigDecimal(readString(key, fallback.toPlainString()));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static List<String> csv(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split(","))
                .map(String::trim).filter(v -> !v.isEmpty())
                .toList();
    }
}
