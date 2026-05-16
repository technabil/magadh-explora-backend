package com.magadhexplora.api.currency;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping("/api/public/currency")
    public CurrencyResponse snapshot() {
        return currencyService.snapshot();
    }

    @PostMapping("/api/admin/currency/refresh")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> refresh() {
        int updated = currencyService.refresh();
        return Map.of("updated", updated);
    }
}
