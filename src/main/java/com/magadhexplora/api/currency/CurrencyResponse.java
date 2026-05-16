package com.magadhexplora.api.currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class CurrencyResponse {
    private Map<String, BigDecimal> ratesToInr;
    private BigDecimal markupPercent;
    private String defaultCurrency;
    private List<String> allowedCurrencies;
    private Instant updatedAt;

    public CurrencyResponse() {}

    public CurrencyResponse(Map<String, BigDecimal> ratesToInr, BigDecimal markupPercent,
                            String defaultCurrency, List<String> allowedCurrencies, Instant updatedAt) {
        this.ratesToInr = ratesToInr;
        this.markupPercent = markupPercent;
        this.defaultCurrency = defaultCurrency;
        this.allowedCurrencies = allowedCurrencies;
        this.updatedAt = updatedAt;
    }

    public Map<String, BigDecimal> getRatesToInr() { return ratesToInr; }
    public void setRatesToInr(Map<String, BigDecimal> ratesToInr) { this.ratesToInr = ratesToInr; }

    public BigDecimal getMarkupPercent() { return markupPercent; }
    public void setMarkupPercent(BigDecimal markupPercent) { this.markupPercent = markupPercent; }

    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }

    public List<String> getAllowedCurrencies() { return allowedCurrencies; }
    public void setAllowedCurrencies(List<String> allowedCurrencies) { this.allowedCurrencies = allowedCurrencies; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
