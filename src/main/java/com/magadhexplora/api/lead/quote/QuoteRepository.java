package com.magadhexplora.api.lead.quote;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteRepository extends JpaRepository<QuoteEntity, Long> {
    Page<QuoteEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
