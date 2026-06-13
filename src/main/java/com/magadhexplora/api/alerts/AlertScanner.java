package com.magadhexplora.api.alerts;

import com.magadhexplora.api.analytics.LeadScoringService;
import com.magadhexplora.api.lead.booking.BookingEntity;
import com.magadhexplora.api.lead.booking.BookingRepository;
import com.magadhexplora.api.lead.contact.ContactRepository;
import com.magadhexplora.api.lead.quote.QuoteEntity;
import com.magadhexplora.api.lead.quote.QuoteRepository;
import com.magadhexplora.api.tracking.TrackingEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
public class AlertScanner {

    private static final Logger log = LoggerFactory.getLogger(AlertScanner.class);

    private final AlertService alerts;
    private final TrackingEventRepository trackingEvents;
    private final QuoteRepository quotes;
    private final ContactRepository contacts;
    private final BookingRepository bookings;
    private final LeadScoringService scoring;

    public AlertScanner(AlertService alerts,
                        TrackingEventRepository trackingEvents,
                        QuoteRepository quotes,
                        ContactRepository contacts,
                        BookingRepository bookings,
                        LeadScoringService scoring) {
        this.alerts = alerts;
        this.trackingEvents = trackingEvents;
        this.quotes = quotes;
        this.contacts = contacts;
        this.bookings = bookings;
        this.scoring = scoring;
    }

    /** Runs hourly at minute 5 of every hour. */
    @Scheduled(cron = "0 5 * * * *")
    public void scan() {
        log.info("AlertScanner: scanning rules");
        try { ruleTrafficDrop();   } catch (Exception ex) { log.warn("rule trafficDrop failed: {}", ex.toString()); }
        try { ruleHotLeadToday();  } catch (Exception ex) { log.warn("rule hotLead failed: {}", ex.toString()); }
        try { ruleHighValueBooking(); } catch (Exception ex) { log.warn("rule highValueBooking failed: {}", ex.toString()); }
    }

    /** Manual trigger (called from the controller for testing). */
    public void runOnce() { scan(); }

    /** Daily traffic dropped >=30% vs same day last week. Fires once per day. */
    private void ruleTrafficDrop() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant todayStart = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant now        = Instant.now();
        Instant weekAgoStart = today.minusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant weekAgoEnd   = weekAgoStart.plusSeconds(86_400);

        long todayCount = trackingEvents.countUniqueVisitors(todayStart, now);
        long lastWeek   = trackingEvents.countUniqueVisitors(weekAgoStart, weekAgoEnd);
        if (lastWeek < 20) return;  // ignore noise on small samples
        double dropPct = ((lastWeek - todayCount) * 100.0) / lastWeek;
        if (dropPct < 30.0) return;

        String dedupe = "trafficDrop:" + today;
        alerts.raise(
                "trafficDrop", "WARNING", dedupe,
                "Daily traffic dropped " + String.format("%.0f", dropPct) + "% vs last week",
                "Today's unique visitors: " + todayCount + " · Same day last week: " + lastWeek
                        + ". Check ad spend, site availability and recent deploys.",
                null
        );
    }

    /** A new lead today scored >= 70 (HOT). Fires per-lead. */
    private void ruleHotLeadToday() {
        Instant since = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        try {
            for (var q : quotes.findAll()) {
                if (q.getCreatedAt() == null || q.getCreatedAt().isBefore(since)) continue;
                LeadScoringService.Score s = scoring.scoreQuote(q);
                if (s.value() < 70) continue;
                alerts.raise(
                        "hotLead", "INFO", "hotLead:quote:" + q.getId(),
                        "Hot lead — quote from " + q.getName() + " (score " + s.value() + ")",
                        format(q, s),
                        null
                );
            }
        } catch (Exception ex) { log.debug("hotLead quotes scan: {}", ex.toString()); }

        try {
            for (var c : contacts.findAll()) {
                if (c.getCreatedAt() == null || c.getCreatedAt().isBefore(since)) continue;
                LeadScoringService.Score s = scoring.scoreContact(c);
                if (s.value() < 70) continue;
                alerts.raise(
                        "hotLead", "INFO", "hotLead:contact:" + c.getId(),
                        "Hot lead — contact from " + c.getName() + " (score " + s.value() + ")",
                        "Email: " + c.getEmail()
                                + (c.getMobile() == null ? "" : " · Mobile: " + c.getMobile())
                                + " · " + s.reason(),
                        null
                );
            }
        } catch (Exception ex) { log.debug("hotLead contacts scan: {}", ex.toString()); }
    }

    /** Any booking with total >= ₹50K created today. */
    private void ruleHighValueBooking() {
        Instant since = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        for (BookingEntity b : bookings.findAll()) {
            if (b.getCreatedAt() == null || b.getCreatedAt().isBefore(since)) continue;
            BigDecimal amt = b.getTotalAmountInr();
            if (amt == null || amt.compareTo(BigDecimal.valueOf(50_000)) < 0) continue;
            alerts.raise(
                    "highValueBooking", "INFO", "highValueBooking:" + b.getId(),
                    "High-value booking ₹" + amt.toPlainString() + " — " + b.getName(),
                    "Booking #" + b.getId() + " · " + b.getNumTravelers() + " travelers · status "
                            + b.getStatus() + " / " + b.getPaymentStatus()
                            + ". Make sure the customer is contacted within an hour.",
                    null
            );
        }
    }

    private static String format(QuoteEntity q, LeadScoringService.Score s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Email: ").append(q.getEmail());
        if (q.getMobile() != null) sb.append(" · Mobile: ").append(q.getMobile());
        if (q.getCountry() != null) sb.append(" · Country: ").append(q.getCountry());
        if (q.getBudget() != null) sb.append(" · Budget: ").append(q.getBudget());
        sb.append(" · ").append(s.reason());
        return sb.toString();
    }
}
