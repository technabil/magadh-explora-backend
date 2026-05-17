package com.magadhexplora.api.lead.abandoned;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * When a real lead (Contact/Quote/Booking) is created, we flip any matching
 * abandoned-lead rows to CONVERTED so the recovery scheduler stops touching them.
 */
@Service
public class RecoveryAttributionService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryAttributionService.class);

    private final AbandonedLeadRepository repo;

    public RecoveryAttributionService(AbandonedLeadRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void markConverted(String email) {
        if (email == null || email.isBlank()) return;
        String norm = email.trim().toLowerCase();
        List<AbandonedLeadEntity> open = repo.findAllByEmailIgnoreCaseAndStatus(norm, "NEW");
        if (open.isEmpty()) return;
        for (AbandonedLeadEntity e : open) {
            e.setStatus("CONVERTED");
            e.setNextTouchAt(null);
        }
        repo.saveAll(open);
        log.info("Recovery attribution: marked {} abandoned lead(s) CONVERTED for {}", open.size(), norm);
    }
}
