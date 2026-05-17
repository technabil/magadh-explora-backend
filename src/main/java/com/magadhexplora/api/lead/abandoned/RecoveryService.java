package com.magadhexplora.api.lead.abandoned;

import com.magadhexplora.api.mail.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 3-touch email recovery for abandoned leads.
 *
 *   Touch 1: ~1 hour after capture   (set at capture time)
 *   Touch 2: ~24 hours after touch 1
 *   Touch 3: ~72 hours after touch 2
 *
 * After 3 attempts the lead is marked COLD and no further touches are scheduled.
 * Conversion attribution (lead returns and submits) flips status to CONVERTED
 * from the create endpoints, which excludes them from {@link AbandonedLeadRepository#findDueForTouch}.
 */
@Service
public class RecoveryService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryService.class);

    private static final Duration[] NEXT_DELAYS = new Duration[] {
            Duration.ofHours(24),  // After touch 1 → wait 24h for touch 2
            Duration.ofHours(72),  // After touch 2 → wait 72h for touch 3
            null                    // After touch 3 → no further touches
    };

    private final AbandonedLeadRepository repo;
    private final EmailService email;

    public RecoveryService(AbandonedLeadRepository repo, EmailService email) {
        this.repo = repo;
        this.email = email;
    }

    /** Runs every 10 minutes. fixedDelay so we don't pile up if a run is slow. */
    @Scheduled(fixedDelay = 10 * 60 * 1000L, initialDelay = 60 * 1000L)
    @Transactional
    public void runRecoveryBatch() {
        Instant now = Instant.now();
        List<AbandonedLeadEntity> due = repo.findDueForTouch(now);
        if (due.isEmpty()) return;

        log.info("Recovery batch: {} leads due", due.size());

        for (AbandonedLeadEntity lead : due) {
            try {
                sendTouchAndAdvance(lead, lead.getAttempts() + 1, now);
            } catch (Exception ex) {
                log.warn("Recovery touch failed for lead {}: {}", lead.getId(), ex.toString());
            }
        }
    }

    /**
     * Send a specific touch (or the next one) for a single lead — used by both the
     * scheduler and the admin "send reminder" endpoint. Advances scheduler state
     * the same way either way so we never double-send.
     */
    @Transactional
    public AbandonedLeadEntity sendTouchManual(Long leadId, Integer touchOverride) {
        AbandonedLeadEntity lead = repo.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));
        if (!"NEW".equals(lead.getStatus()) && !"COLD".equals(lead.getStatus())) {
            // Already CONVERTED / CANCELLED — refuse to send
            throw new IllegalStateException("Lead status is " + lead.getStatus() + "; cannot send recovery email.");
        }
        int touch = touchOverride != null ? touchOverride : Math.min(lead.getAttempts() + 1, 3);
        if (touch < 1 || touch > 3) {
            throw new IllegalArgumentException("Touch must be 1, 2, or 3");
        }
        return sendTouchAndAdvance(lead, touch, Instant.now());
    }

    private AbandonedLeadEntity sendTouchAndAdvance(AbandonedLeadEntity lead, int touch, Instant now) {
        email.sendRecoveryTouch(lead, touch);

        // attempts only moves forward — manual re-sends of the same touch don't double-count
        lead.setAttempts(Math.max(lead.getAttempts(), touch));
        lead.setLastTouchedAt(now);
        lead.setLastTouchChannel("email");

        Duration nextDelay = touch <= NEXT_DELAYS.length ? NEXT_DELAYS[touch - 1] : null;
        if (nextDelay == null) {
            lead.setNextTouchAt(null);
            if ("NEW".equals(lead.getStatus())) lead.setStatus("COLD");
        } else {
            lead.setNextTouchAt(now.plus(nextDelay));
            // If admin sends after the lead went COLD, bring it back into the sequence
            if ("COLD".equals(lead.getStatus())) lead.setStatus("NEW");
        }
        return repo.save(lead);
    }
}
