package com.magadhexplora.api.currency;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRateRepository extends JpaRepository<CurrencyRateEntity, String> {
}
