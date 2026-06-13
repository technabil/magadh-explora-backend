package com.magadhexplora.api.alerts;

import com.magadhexplora.api.mail.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository repo;
    private final EmailService email;

    public AlertService(AlertRepository repo, EmailService email) {
        this.repo = repo;
        this.email = email;
    }

    /**
     * Insert an alert if its dedupeKey hasn't fired yet. Returns the new alert
     * if inserted, or empty if it already exists (no-op).
     */
    @Transactional
    public Optional<AlertEntity> raise(String ruleId, String severity, String dedupeKey,
                                       String title, String message, String payloadJson) {
        Optional<AlertEntity> existing = repo.findByDedupeKey(dedupeKey);
        if (existing.isPresent()) return Optional.empty();

        AlertEntity a = new AlertEntity();
        a.setRuleId(ruleId);
        a.setSeverity(severity);
        a.setDedupeKey(dedupeKey);
        a.setTitle(title);
        a.setMessage(message);
        a.setPayload(payloadJson);
        AlertEntity saved = repo.save(a);

        try {
            String html = "<p>" + escape(message) + "</p>"
                    + "<p style=\"color:#888;font-size:12px;\">Severity: " + escape(severity)
                    + " &middot; Rule: " + escape(ruleId) + "</p>";
            email.sendAdminAlert(title, html);
            saved.setEmailSent(true);
            repo.save(saved);
        } catch (Exception ex) {
            log.warn("Alert email dispatch failed for rule={}: {}", ruleId, ex.toString());
        }
        return Optional.of(saved);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
