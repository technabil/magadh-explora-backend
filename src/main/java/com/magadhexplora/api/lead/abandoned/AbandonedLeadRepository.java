package com.magadhexplora.api.lead.abandoned;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface AbandonedLeadRepository extends JpaRepository<AbandonedLeadEntity, Long> {

    Page<AbandonedLeadEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Find a recent (within last hour) abandoned lead for this email+source — used for dedup. */
    Optional<AbandonedLeadEntity> findFirstByEmailIgnoreCaseAndSourceAndCreatedAtAfterOrderByCreatedAtDesc(
            String email, String source, Instant after);

    /** Fallback dedup by mobile when no email is present. */
    Optional<AbandonedLeadEntity> findFirstByMobileAndSourceAndCreatedAtAfterOrderByCreatedAtDesc(
            String mobile, String source, Instant after);
}
