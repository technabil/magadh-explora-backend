package com.magadhexplora.api.currency;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "currency_rates")
public class CurrencyRateEntity {

    @Id
    @Column(name = "currency_code", length = 8)
    private String currencyCode;

    @Column(name = "rate_to_inr", nullable = false, precision = 18, scale = 8)
    private BigDecimal rateToInr;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public CurrencyRateEntity() {}

    public CurrencyRateEntity(String currencyCode, BigDecimal rateToInr, Instant updatedAt) {
        this.currencyCode = currencyCode;
        this.rateToInr = rateToInr;
        this.updatedAt = updatedAt;
    }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public BigDecimal getRateToInr() { return rateToInr; }
    public void setRateToInr(BigDecimal rateToInr) { this.rateToInr = rateToInr; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
