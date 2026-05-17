package com.magadhexplora.api.lead.abandoned;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AbandonedLeadRepository extends JpaRepository<AbandonedLeadEntity, Long> {

    Page<AbandonedLeadEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Find a recent (within last hour) abandoned lead for this email+source — used for dedup. */
    Optional<AbandonedLeadEntity> findFirstByEmailIgnoreCaseAndSourceAndCreatedAtAfterOrderByCreatedAtDesc(
            String email, String source, Instant after);

    /** Fallback dedup by mobile when no email is present. */
    Optional<AbandonedLeadEntity> findFirstByMobileAndSourceAndCreatedAtAfterOrderByCreatedAtDesc(
            String mobile, String source, Instant after);

    /** Recovery link resolution. */
    Optional<AbandonedLeadEntity> findByRecoveryToken(String recoveryToken);

    /**
     * Leads ready for the next touch in the 3-step recovery sequence.
     * Email present, status still NEW, attempts < 3, and next_touch_at has passed.
     */
    @Query("""
            SELECT a FROM AbandonedLeadEntity a
            WHERE a.status = 'NEW'
              AND a.email IS NOT NULL
              AND a.attempts < 3
              AND a.nextTouchAt IS NOT NULL
              AND a.nextTouchAt <= :now
            ORDER BY a.nextTouchAt ASC
            """)
    List<AbandonedLeadEntity> findDueForTouch(@Param("now") Instant now);

    /**
     * Attribution lookup: when a Contact/Quote/Booking is created with the same
     * email, mark any open abandoned leads as CONVERTED.
     */
    List<AbandonedLeadEntity> findAllByEmailIgnoreCaseAndStatus(String email, String status);
}
