package com.magadhexplora.api.analytics;

import com.magadhexplora.api.lead.booking.BookingEntity;
import com.magadhexplora.api.lead.booking.BookingRepository;
import com.magadhexplora.api.lead.contact.ContactRepository;
import com.magadhexplora.api.lead.quote.QuoteRepository;
import com.magadhexplora.api.catalog.pkg.PackageEntity;
import com.magadhexplora.api.catalog.pkg.PackageRepository;
import com.magadhexplora.api.tracking.TrackingEventRepository;
import com.magadhexplora.api.tracking.TrackingSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lightweight analytics — compute aggregates in-app over loaded entities.
 * Fine up to ~10k bookings. For more, swap to JPA aggregate queries.
 */
@RestController
@RequestMapping("/api/admin/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final BookingRepository bookings;
    private final QuoteRepository quotes;
    private final ContactRepository contacts;
    private final PackageRepository packages;
    private final TrackingEventRepository trackingEvents;
    private final TrackingSessionRepository trackingSessions;
    private final LeadScoringService leadScoring;
    private final ForecastService forecast;

    public AnalyticsController(BookingRepository bookings,
                               QuoteRepository quotes,
                               ContactRepository contacts,
                               PackageRepository packages,
                               TrackingEventRepository trackingEvents,
                               TrackingSessionRepository trackingSessions,
                               LeadScoringService leadScoring,
                               ForecastService forecast) {
        this.bookings = bookings;
        this.quotes = quotes;
        this.contacts = contacts;
        this.packages = packages;
        this.trackingEvents = trackingEvents;
        this.trackingSessions = trackingSessions;
        this.leadScoring = leadScoring;
        this.forecast = forecast;
    }

    /**
     * Dashboard Overview KPIs — "today" snapshot plus WoW deltas.
     * Visitor/live numbers return null until the tracking layer ships; the
     * {@code trackingEnabled} flag tells the UI whether to render them.
     */
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Instant todayStart  = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant todayEnd    = todayStart.plusSeconds(86_400);
        Instant weekAgoStart = todayStart.minusSeconds(7L * 86_400);
        Instant weekAgoEnd   = weekAgoStart.plusSeconds(86_400);

        List<BookingEntity> allBookings;
        try {
            allBookings = bookings.findAll();
        } catch (Exception ex) {
            log.error("Overview: loading bookings failed", ex);
            allBookings = List.of();
        }

        long bookingsToday   = allBookings.stream().filter(b -> inRange(b.getCreatedAt(), todayStart, todayEnd)).count();
        long bookingsWeekAgo = allBookings.stream().filter(b -> inRange(b.getCreatedAt(), weekAgoStart, weekAgoEnd)).count();

        BigDecimal revenueToday = allBookings.stream()
                .filter(b -> "PAID".equals(b.getPaymentStatus()))
                .filter(b -> inRange(b.getPaidAt() != null ? b.getPaidAt() : b.getCreatedAt(), todayStart, todayEnd))
                .map(BookingEntity::getTotalAmountInr)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal revenueWeekAgo = allBookings.stream()
                .filter(b -> "PAID".equals(b.getPaymentStatus()))
                .filter(b -> inRange(b.getPaidAt() != null ? b.getPaidAt() : b.getCreatedAt(), weekAgoStart, weekAgoEnd))
                .map(BookingEntity::getTotalAmountInr)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long contactsToday = safeCount(() -> countCreatedInRange(
                contacts.findAll().stream().map(c -> c.getCreatedAt()).toList(), todayStart, todayEnd));
        long quotesToday   = safeCount(() -> countCreatedInRange(
                quotes.findAll().stream().map(q -> q.getCreatedAt()).toList(), todayStart, todayEnd));
        long enquiriesToday = contactsToday + quotesToday;

        long contactsWeekAgo = safeCount(() -> countCreatedInRange(
                contacts.findAll().stream().map(c -> c.getCreatedAt()).toList(), weekAgoStart, weekAgoEnd));
        long quotesWeekAgo = safeCount(() -> countCreatedInRange(
                quotes.findAll().stream().map(q -> q.getCreatedAt()).toList(), weekAgoStart, weekAgoEnd));
        long enquiriesWeekAgo = contactsWeekAgo + quotesWeekAgo;

        long visitorsToday = 0;
        long visitorsWeekAgo = 0;
        long liveActive = 0;
        boolean trackingEnabled = false;
        Map<String, Object> topSource = null;
        try {
            visitorsToday   = trackingEvents.countUniqueVisitors(todayStart, todayEnd);
            visitorsWeekAgo = trackingEvents.countUniqueVisitors(weekAgoStart, weekAgoEnd);
            liveActive      = trackingEvents.countLiveVisitors(Instant.now().minusSeconds(300));
            trackingEnabled = true;
            List<Object[]> sources = trackingEvents.topSources(todayStart, todayEnd);
            if (sources != null && !sources.isEmpty()) {
                Object[] row = sources.get(0);
                Map<String, Object> ts = new LinkedHashMap<>();
                ts.put("name", String.valueOf(row[0]));
                ts.put("visitors", ((Number) row[1]).longValue());
                topSource = ts;
            }
        } catch (Exception ex) {
            log.warn("Overview: tracking queries failed, falling back: {}", ex.toString());
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("trackingEnabled", trackingEnabled);
        out.put("visitorsToday", trackingEnabled ? visitorsToday : null);
        out.put("visitorsDeltaPct", trackingEnabled ? pctDelta(visitorsToday, visitorsWeekAgo) : null);
        out.put("liveActiveUsers", trackingEnabled ? liveActive : null);
        out.put("topSource", topSource);

        out.put("enquiriesToday", enquiriesToday);
        out.put("enquiriesDeltaPct", pctDelta(enquiriesToday, enquiriesWeekAgo));

        out.put("bookingsToday", bookingsToday);
        out.put("bookingsDeltaPct", pctDelta(bookingsToday, bookingsWeekAgo));

        out.put("revenueTodayInr", revenueToday);
        out.put("revenueDeltaPct", pctDelta(revenueToday, revenueWeekAgo));

        return out;
    }

    /** Daily unique visitor count for the last {range} days. */
    @GetMapping("/visitors-series")
    public List<Map<String, Object>> visitorsSeries(@RequestParam(defaultValue = "30") int range) {
        int days = Math.max(1, Math.min(range, 365));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant from = today.minusDays(days - 1L).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to   = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<String, Long> byDay = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            byDay.put(today.minusDays(days - 1L - i).toString(), 0L);
        }
        try {
            for (Object[] row : trackingEvents.dailyVisitorCounts(from, to)) {
                String day = row[0].toString();
                if (day.length() > 10) day = day.substring(0, 10);
                byDay.put(day, ((Number) row[1]).longValue());
            }
        } catch (Exception ex) {
            log.warn("visitors-series query failed: {}", ex.toString());
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (var e : byDay.entrySet()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("date", e.getKey());
            r.put("visitors", e.getValue());
            out.add(r);
        }
        return out;
    }

    /** Top traffic sources today (anonymous visitors per source). */
    @GetMapping("/top-sources")
    public List<Map<String, Object>> topSources(@RequestParam(defaultValue = "5") int limit) {
        Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant todayEnd   = todayStart.plusSeconds(86_400);
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            List<Object[]> rows = trackingEvents.topSources(todayStart, todayEnd);
            for (Object[] r : rows) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("source", String.valueOf(r[0]));
                row.put("visitors", ((Number) r[1]).longValue());
                out.add(row);
                if (out.size() >= limit) break;
            }
        } catch (Exception ex) {
            log.warn("top-sources query failed: {}", ex.toString());
        }
        return out;
    }

    /**
     * Full 7-stage funnel with filters. Stages 2 (engagement) and 6 (quoteReviewed)
     * are not yet instrumented — they return null so the UI can render "pending".
     */
    @GetMapping("/funnel-v2")
    public Map<String, Object> funnelV2(@RequestParam(required = false) String from,
                                        @RequestParam(required = false) String to,
                                        @RequestParam(required = false) String country,
                                        @RequestParam(required = false) String source,
                                        @RequestParam(required = false) String device) {
        Instant fromI = parseDate(from, true);
        Instant toI = parseDate(to, false);
        if (fromI == null) fromI = LocalDate.now(ZoneOffset.UTC).minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (toI == null)   toI   = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        long visits = 0, packageViews = 0, formStarts = 0, formSubmits = 0, whatsapp = 0;
        try {
            Object[] row = trackingEvents.funnelCounts(fromI, toI,
                    blankToNull(country), blankToNull(source), blankToNull(device));
            if (row != null && row.length >= 5) {
                visits       = num(row[0]);
                packageViews = num(row[1]);
                formStarts   = num(row[2]);
                formSubmits  = num(row[3]);
                whatsapp     = num(row[4]);
            }
        } catch (Exception ex) {
            log.warn("funnel-v2 query failed: {}", ex.toString());
        }

        long bookingsInRange;
        try {
            final Instant ff = fromI, tt = toI;
            bookingsInRange = bookings.findAll().stream()
                    .filter(b -> inRange(b.getCreatedAt(), ff, tt)).count();
        } catch (Exception ex) {
            bookingsInRange = 0;
        }

        List<Map<String, Object>> stages = new ArrayList<>();
        stages.add(stage("visit",          "Visit",            visits,        true));
        stages.add(stage("engagement",     "Engagement",       null,          false));
        stages.add(stage("packageView",    "Package View",     packageViews,  true));
        stages.add(stage("formStart",      "Form Start",       formStarts,    true));
        stages.add(stage("formSubmit",     "Form Submit",      formSubmits,   true));
        stages.add(stage("quoteReviewed",  "Quote Reviewed",   null,          false));
        stages.add(stage("booking",        "Booking",          bookingsInRange, true));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("stages", stages);
        out.put("whatsappClicks", whatsapp);
        return out;
    }

    /** Top countries with visitor count + form-submit count for conversion display. */
    @GetMapping("/geo/countries")
    public List<Map<String, Object>> geoCountries(@RequestParam(required = false) String from,
                                                  @RequestParam(required = false) String to,
                                                  @RequestParam(defaultValue = "20") int limit) {
        Instant fromI = parseDate(from, true);
        Instant toI = parseDate(to, false);
        if (fromI == null) fromI = LocalDate.now(ZoneOffset.UTC).minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (toI == null)   toI   = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Map<String, Object>> out = new ArrayList<>();
        try {
            for (Object[] r : trackingEvents.countriesWithConversion(fromI, toI)) {
                long v = num(r[2]);
                long s = num(r[3]);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("countryCode", String.valueOf(r[0]).toLowerCase());
                row.put("country", String.valueOf(r[1]));
                row.put("visitors", v);
                row.put("submits", s);
                row.put("conversionPct", v == 0 ? null :
                        BigDecimal.valueOf(s).multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(v), 1, RoundingMode.HALF_UP));
                out.add(row);
                if (out.size() >= limit) break;
            }
        } catch (Exception ex) {
            log.warn("geo/countries query failed: {}", ex.toString());
        }
        return out;
    }

    /** Top cities for a given country (default IN). */
    @GetMapping("/geo/cities")
    public List<Map<String, Object>> geoCities(@RequestParam(required = false) String from,
                                               @RequestParam(required = false) String to,
                                               @RequestParam(defaultValue = "IN") String country,
                                               @RequestParam(defaultValue = "20") int limit) {
        Instant fromI = parseDate(from, true);
        Instant toI = parseDate(to, false);
        if (fromI == null) fromI = LocalDate.now(ZoneOffset.UTC).minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (toI == null)   toI   = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Map<String, Object>> out = new ArrayList<>();
        try {
            for (Object[] r : trackingEvents.citiesByCountry(fromI, toI, country.toUpperCase())) {
                long v = num(r[1]);
                long s = num(r[2]);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("city", String.valueOf(r[0]));
                row.put("visitors", v);
                row.put("submits", s);
                row.put("conversionPct", v == 0 ? null :
                        BigDecimal.valueOf(s).multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(v), 1, RoundingMode.HALF_UP));
                out.add(row);
                if (out.size() >= limit) break;
            }
        } catch (Exception ex) {
            log.warn("geo/cities query failed: {}", ex.toString());
        }
        return out;
    }

    /** Per-package view counts. */
    @GetMapping("/packages/views")
    public List<Map<String, Object>> packageViews(@RequestParam(required = false) String from,
                                                  @RequestParam(required = false) String to,
                                                  @RequestParam(defaultValue = "10") int limit) {
        Instant fromI = parseDate(from, true);
        Instant toI   = parseDate(to, false);
        if (fromI == null) fromI = LocalDate.now(ZoneOffset.UTC).minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (toI == null)   toI   = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<Long, PackageEntity> byId = new HashMap<>();
        try {
            for (PackageEntity p : packages.findAll()) byId.put(p.getId(), p);
        } catch (Exception ignored) { /* best effort */ }

        List<Map<String, Object>> out = new ArrayList<>();
        try {
            for (Object[] r : trackingEvents.packageViews(fromI, toI)) {
                if (r[0] == null) continue;
                Long pid = ((Number) r[0]).longValue();
                PackageEntity p = byId.get(pid);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("packageId", pid);
                row.put("title", p == null ? "Package #" + pid : p.getTitle());
                row.put("slug", p == null ? null : p.getSlug());
                row.put("views", num(r[1]));
                row.put("uniqueVisitors", num(r[2]));
                out.add(row);
                if (out.size() >= limit) break;
            }
        } catch (Exception ex) {
            log.warn("packages/views query failed: {}", ex.toString());
        }
        return out;
    }

    /** Per-package view-to-enquiry conversion. */
    @GetMapping("/packages/conversion")
    public List<Map<String, Object>> packageConversion(@RequestParam(required = false) String from,
                                                      @RequestParam(required = false) String to,
                                                      @RequestParam(defaultValue = "10") int limit) {
        Instant fromI = parseDate(from, true);
        Instant toI   = parseDate(to, false);
        if (fromI == null) fromI = LocalDate.now(ZoneOffset.UTC).minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (toI == null)   toI   = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<Long, PackageEntity> byId = new HashMap<>();
        try {
            for (PackageEntity p : packages.findAll()) byId.put(p.getId(), p);
        } catch (Exception ignored) { /* best effort */ }

        List<Map<String, Object>> out = new ArrayList<>();
        try {
            for (Object[] r : trackingEvents.packageConversion(fromI, toI)) {
                if (r[0] == null) continue;
                Long pid = ((Number) r[0]).longValue();
                long viewers = num(r[1]);
                long submitters = num(r[2]);
                PackageEntity p = byId.get(pid);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("packageId", pid);
                row.put("title", p == null ? "Package #" + pid : p.getTitle());
                row.put("slug", p == null ? null : p.getSlug());
                row.put("viewers", viewers);
                row.put("submitters", submitters);
                row.put("conversionPct", viewers == 0 ? null :
                        BigDecimal.valueOf(submitters).multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(viewers), 1, RoundingMode.HALF_UP));
                out.add(row);
                if (out.size() >= limit) break;
            }
        } catch (Exception ex) {
            log.warn("packages/conversion query failed: {}", ex.toString());
        }
        return out;
    }

    /** Top country per package. */
    @GetMapping("/packages/countries")
    public List<Map<String, Object>> packageCountries(@RequestParam(required = false) String from,
                                                     @RequestParam(required = false) String to,
                                                     @RequestParam(defaultValue = "10") int limit) {
        Instant fromI = parseDate(from, true);
        Instant toI   = parseDate(to, false);
        if (fromI == null) fromI = LocalDate.now(ZoneOffset.UTC).minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (toI == null)   toI   = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<Long, PackageEntity> byId = new HashMap<>();
        try {
            for (PackageEntity p : packages.findAll()) byId.put(p.getId(), p);
        } catch (Exception ignored) { /* best effort */ }

        List<Map<String, Object>> out = new ArrayList<>();
        try {
            for (Object[] r : trackingEvents.packageTopCountry(fromI, toI)) {
                if (r[0] == null) continue;
                Long pid = ((Number) r[0]).longValue();
                PackageEntity p = byId.get(pid);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("packageId", pid);
                row.put("title", p == null ? "Package #" + pid : p.getTitle());
                row.put("countryCode", String.valueOf(r[1]).toLowerCase());
                row.put("country", String.valueOf(r[2]));
                row.put("viewers", num(r[3]));
                out.add(row);
                if (out.size() >= limit) break;
            }
        } catch (Exception ex) {
            log.warn("packages/countries query failed: {}", ex.toString());
        }
        return out;
    }

    /** Top pageview paths + top landing paths. */
    @GetMapping("/behavior/pages")
    public Map<String, Object> behaviorPages(@RequestParam(required = false) String from,
                                             @RequestParam(required = false) String to,
                                             @RequestParam(defaultValue = "10") int limit) {
        Instant fromI = parseDate(from, true);
        Instant toI   = parseDate(to, false);
        if (fromI == null) fromI = LocalDate.now(ZoneOffset.UTC).minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (toI == null)   toI   = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Map<String, Object>> paths = new ArrayList<>();
        List<Map<String, Object>> landing = new ArrayList<>();
        try {
            for (Object[] r : trackingEvents.topPaths(fromI, toI)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("path", String.valueOf(r[0]));
                row.put("views", num(r[1]));
                row.put("uniqueVisitors", num(r[2]));
                paths.add(row);
                if (paths.size() >= limit) break;
            }
        } catch (Exception ex) {
            log.warn("behavior/pages topPaths failed: {}", ex.toString());
        }
        try {
            for (Object[] r : trackingEvents.topLandingPaths(fromI, toI)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("path", String.valueOf(r[0]));
                row.put("sessions", num(r[1]));
                landing.add(row);
                if (landing.size() >= limit) break;
            }
        } catch (Exception ex) {
            log.warn("behavior/pages topLanding failed: {}", ex.toString());
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("topPaths", paths);
        out.put("topLandingPaths", landing);
        return out;
    }

    /** WhatsApp click stats. */
    @GetMapping("/behavior/whatsapp")
    public Map<String, Object> behaviorWhatsapp(@RequestParam(required = false) String from,
                                                @RequestParam(required = false) String to,
                                                @RequestParam(defaultValue = "10") int limit) {
        Instant fromI = parseDate(from, true);
        Instant toI   = parseDate(to, false);
        if (fromI == null) fromI = LocalDate.now(ZoneOffset.UTC).minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (toI == null)   toI   = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        long total = 0;
        List<Map<String, Object>> byCountry = new ArrayList<>();
        try {
            total = trackingEvents.whatsappClicksTotal(fromI, toI);
            for (Object[] r : trackingEvents.whatsappClicksByCountry(fromI, toI)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("countryCode", String.valueOf(r[0]).toLowerCase());
                row.put("country", String.valueOf(r[1]));
                row.put("clicks", num(r[2]));
                byCountry.add(row);
                if (byCountry.size() >= limit) break;
            }
        } catch (Exception ex) {
            log.warn("behavior/whatsapp query failed: {}", ex.toString());
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", total);
        out.put("byCountry", byCountry);
        return out;
    }

    /** Per-form abandonment rate. */
    @GetMapping("/behavior/form-abandonment")
    public List<Map<String, Object>> behaviorFormAbandonment(@RequestParam(required = false) String from,
                                                             @RequestParam(required = false) String to) {
        Instant fromI = parseDate(from, true);
        Instant toI   = parseDate(to, false);
        if (fromI == null) fromI = LocalDate.now(ZoneOffset.UTC).minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (toI == null)   toI   = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Map<String, Object>> out = new ArrayList<>();
        try {
            for (Object[] r : trackingEvents.formAbandonment(fromI, toI)) {
                String form = r[0] == null ? "(unknown)" : String.valueOf(r[0]);
                long started = num(r[1]);
                long submitted = num(r[2]);
                long abandoned = Math.max(0, started - submitted);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("form", form);
                row.put("started", started);
                row.put("submitted", submitted);
                row.put("abandoned", abandoned);
                row.put("abandonmentPct", started == 0 ? null :
                        BigDecimal.valueOf(abandoned).multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(started), 1, RoundingMode.HALF_UP));
                out.add(row);
            }
        } catch (Exception ex) {
            log.warn("behavior/form-abandonment query failed: {}", ex.toString());
        }
        return out;
    }

    /**
     * Top-scored leads across quotes / contacts / bookings, freshest first.
     * Computes scores on the fly using {@link LeadScoringService}.
     */
    @GetMapping("/hot-leads")
    public List<Map<String, Object>> hotLeads(@RequestParam(defaultValue = "10") int limit) {
        Instant cutoff = Instant.now().minusSeconds(30L * 86_400);  // last 30 days
        List<Map<String, Object>> all = new ArrayList<>();

        try {
            for (var q : quotes.findAll()) {
                if (q.getCreatedAt() == null || q.getCreatedAt().isBefore(cutoff)) continue;
                LeadScoringService.Score s = leadScoring.scoreQuote(q);
                all.add(leadRow("quote", q.getId(), q.getName(), q.getEmail(), q.getCountry(),
                        s, q.getCreatedAt(), "/admin/queries/quotes"));
            }
        } catch (Exception ex) { log.warn("hot-leads quotes failed: {}", ex.toString()); }

        try {
            for (var c : contacts.findAll()) {
                if (c.getCreatedAt() == null || c.getCreatedAt().isBefore(cutoff)) continue;
                LeadScoringService.Score s = leadScoring.scoreContact(c);
                all.add(leadRow("contact", c.getId(), c.getName(), c.getEmail(), null,
                        s, c.getCreatedAt(), "/admin/queries/contacts"));
            }
        } catch (Exception ex) { log.warn("hot-leads contacts failed: {}", ex.toString()); }

        try {
            for (var b : bookings.findAll()) {
                if (b.getCreatedAt() == null || b.getCreatedAt().isBefore(cutoff)) continue;
                LeadScoringService.Score s = leadScoring.scoreBooking(b);
                all.add(leadRow("booking", b.getId(), b.getName(), b.getEmail(), null,
                        s, b.getCreatedAt(), "/admin/bookings/" + b.getId()));
            }
        } catch (Exception ex) { log.warn("hot-leads bookings failed: {}", ex.toString()); }

        // Score desc, then createdAt desc
        all.sort((a, b) -> {
            int byScore = Integer.compare((int) b.get("score"), (int) a.get("score"));
            if (byScore != 0) return byScore;
            Instant ai = (Instant) a.get("createdAt");
            Instant bi = (Instant) b.get("createdAt");
            return bi.compareTo(ai);
        });
        return all.stream().limit(limit).toList();
    }

    private static Map<String, Object> leadRow(String type, Long id, String name, String email,
                                               String country, LeadScoringService.Score s,
                                               Instant createdAt, String href) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("type", type);
        row.put("id", id);
        row.put("name", name);
        row.put("email", email);
        row.put("country", country);
        row.put("score", s.value());
        row.put("tier", s.tier().name());
        row.put("reason", s.reason());
        row.put("createdAt", createdAt);
        row.put("href", href);
        return row;
    }

    /** Top countries today by unique visitors. */
    @GetMapping("/top-countries")
    public List<Map<String, Object>> topCountries(@RequestParam(defaultValue = "5") int limit) {
        Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant todayEnd   = todayStart.plusSeconds(86_400);
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            List<Object[]> rows = trackingEvents.topCountries(todayStart, todayEnd);
            for (Object[] r : rows) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("countryCode", String.valueOf(r[0]).toLowerCase());
                row.put("country", String.valueOf(r[1]));
                row.put("visitors", ((Number) r[2]).longValue());
                out.add(row);
                if (out.size() >= limit) break;
            }
        } catch (Exception ex) {
            log.warn("top-countries query failed: {}", ex.toString());
        }
        return out;
    }

    /** Headline KPI cards. */
    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam(required = false) String from,
                                       @RequestParam(required = false) String to) {
        Instant fromI = parseDate(from, true);
        Instant toI = parseDate(to, false);

        List<BookingEntity> bs;
        try {
            bs = bookings.findAll().stream()
                    .filter(b -> inRange(b.getCreatedAt(), fromI, toI))
                    .toList();
        } catch (Exception ex) {
            log.error("Analytics summary failed loading bookings", ex);
            throw ex;
        }

        long totalBookings = bs.size();
        long confirmed = bs.stream().filter(AnalyticsController::isConfirmed).count();
        long cancelled = bs.stream().filter(b -> "CANCELLED".equals(b.getStatus())).count();
        long paid = bs.stream().filter(b -> "PAID".equals(b.getPaymentStatus())).count();

        BigDecimal totalRevenueInr = bs.stream()
                .filter(b -> "PAID".equals(b.getPaymentStatus()))
                .map(BookingEntity::getTotalAmountInr)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pipelineInr = bs.stream()
                .filter(b -> !"CANCELLED".equals(b.getStatus()) && !"PAID".equals(b.getPaymentStatus()))
                .map(BookingEntity::getTotalAmountInr)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgBookingValueInr = totalBookings == 0 ? BigDecimal.ZERO :
                bs.stream().map(BookingEntity::getTotalAmountInr).filter(Objects::nonNull)
                  .reduce(BigDecimal.ZERO, BigDecimal::add)
                  .divide(BigDecimal.valueOf(totalBookings), 2, RoundingMode.HALF_UP);

        long contactCount = safeCount(() -> countCreatedInRange(
                contacts.findAll().stream().map(c -> c.getCreatedAt()).toList(), fromI, toI));
        long quoteCount = safeCount(() -> countCreatedInRange(
                quotes.findAll().stream().map(q -> q.getCreatedAt()).toList(), fromI, toI));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalBookings", totalBookings);
        out.put("confirmed", confirmed);
        out.put("cancelled", cancelled);
        out.put("paid", paid);
        out.put("totalRevenueInr", totalRevenueInr);
        out.put("pipelineInr", pipelineInr);
        out.put("avgBookingValueInr", avgBookingValueInr);
        out.put("contactCount", contactCount);
        out.put("quoteCount", quoteCount);
        out.put("conversionRate", contactCount == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf(totalBookings).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(contactCount), 1, RoundingMode.HALF_UP));
        return out;
    }

    /** Time series of bookings + revenue (paid) per day. */
    @GetMapping("/revenue-series")
    public List<Map<String, Object>> revenueSeries(@RequestParam(required = false) String from,
                                                   @RequestParam(required = false) String to,
                                                   @RequestParam(defaultValue = "day") String groupBy) {
        Instant fromI = parseDate(from, true);
        Instant toI = parseDate(to, false);

        Map<String, List<BookingEntity>> bucketed = new TreeMap<>();
        for (BookingEntity b : bookings.findAll()) {
            if (!inRange(b.getCreatedAt(), fromI, toI)) continue;
            String key = bucketKey(b.getCreatedAt(), groupBy);
            bucketed.computeIfAbsent(key, k -> new ArrayList<>()).add(b);
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (var e : bucketed.entrySet()) {
            BigDecimal revenue = e.getValue().stream()
                    .filter(b -> "PAID".equals(b.getPaymentStatus()))
                    .map(BookingEntity::getTotalAmountInr)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", e.getKey());
            row.put("bookings", e.getValue().size());
            row.put("revenueInr", revenue);
            out.add(row);
        }
        return out;
    }

    /** Revenue/bookings grouped by package. */
    @GetMapping("/by-package")
    public List<Map<String, Object>> byPackage(@RequestParam(required = false) String from,
                                               @RequestParam(required = false) String to,
                                               @RequestParam(defaultValue = "10") int limit) {
        Instant fromI = parseDate(from, true);
        Instant toI = parseDate(to, false);

        Map<Long, String> titles = new HashMap<>();
        for (PackageEntity p : packages.findAll()) titles.put(p.getId(), p.getTitle());

        Map<Long, long[]> counts = new HashMap<>();
        Map<Long, BigDecimal> revenues = new HashMap<>();

        for (BookingEntity b : bookings.findAll()) {
            if (!inRange(b.getCreatedAt(), fromI, toI)) continue;
            long[] c = counts.computeIfAbsent(b.getPackageId(), k -> new long[]{0, 0});
            c[0]++;
            if ("PAID".equals(b.getPaymentStatus())) {
                c[1]++;
                revenues.merge(b.getPackageId(),
                        b.getTotalAmountInr() == null ? BigDecimal.ZERO : b.getTotalAmountInr(),
                        BigDecimal::add);
            }
        }

        return counts.entrySet().stream()
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("packageId", e.getKey());
                    row.put("title", titles.getOrDefault(e.getKey(), "Package #" + e.getKey()));
                    row.put("bookings", e.getValue()[0]);
                    row.put("paidBookings", e.getValue()[1]);
                    row.put("revenueInr", revenues.getOrDefault(e.getKey(), BigDecimal.ZERO));
                    return row;
                })
                .sorted((a, b) -> ((BigDecimal) b.get("revenueInr")).compareTo((BigDecimal) a.get("revenueInr")))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /** Distribution of bookings across lifecycle + payment statuses. */
    @GetMapping("/status-distribution")
    public Map<String, Object> statusDistribution(@RequestParam(required = false) String from,
                                                  @RequestParam(required = false) String to) {
        Instant fromI = parseDate(from, true);
        Instant toI = parseDate(to, false);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        Map<String, Long> byPayment = new LinkedHashMap<>();

        for (BookingEntity b : bookings.findAll()) {
            if (!inRange(b.getCreatedAt(), fromI, toI)) continue;
            byStatus.merge(nullSafe(b.getStatus(), "UNKNOWN"), 1L, Long::sum);
            byPayment.merge(nullSafe(b.getPaymentStatus(), "UNPAID"), 1L, Long::sum);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("byStatus", byStatus);
        out.put("byPayment", byPayment);
        return out;
    }

    /** Funnel: contacts → quotes → bookings → paid. */
    @GetMapping("/funnel")
    public Map<String, Object> funnel(@RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to) {
        Instant fromI = parseDate(from, true);
        Instant toI = parseDate(to, false);

        long contactCount = safeCount(() -> countCreatedInRange(
                contacts.findAll().stream().map(c -> c.getCreatedAt()).toList(), fromI, toI));
        long quoteCount = safeCount(() -> countCreatedInRange(
                quotes.findAll().stream().map(q -> q.getCreatedAt()).toList(), fromI, toI));
        long bookingCount = bookings.findAll().stream()
                .filter(b -> inRange(b.getCreatedAt(), fromI, toI)).count();
        long paidCount = bookings.findAll().stream()
                .filter(b -> inRange(b.getCreatedAt(), fromI, toI))
                .filter(b -> "PAID".equals(b.getPaymentStatus()))
                .count();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("contacts", contactCount);
        out.put("quotes", quoteCount);
        out.put("bookings", bookingCount);
        out.put("paid", paidCount);
        return out;
    }

    /**
     * Monthly booking cohort. Cohort = customers grouped by the month of their
     * first booking. Cells = % of cohort that booked again N months later.
     * Returns: { cohorts: [{ month, size, retention: [{offset, count, pct}] }] }
     */
    @GetMapping("/retention/cohort")
    public Map<String, Object> cohort(@RequestParam(defaultValue = "6") int months) {
        int horizon = Math.max(1, Math.min(months, 12));
        Map<String, Map<String, List<Instant>>> byMonthByEmail = new TreeMap<>();
        Map<String, String> firstMonthByEmail = new HashMap<>();

        try {
            for (BookingEntity b : bookings.findAll()) {
                if (b.getCreatedAt() == null || b.getEmail() == null) continue;
                String month = b.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1).toString();
                firstMonthByEmail.merge(b.getEmail(), month, (a, c) -> a.compareTo(c) < 0 ? a : c);
            }
            for (BookingEntity b : bookings.findAll()) {
                if (b.getCreatedAt() == null || b.getEmail() == null) continue;
                String firstMonth = firstMonthByEmail.get(b.getEmail());
                if (firstMonth == null) continue;
                byMonthByEmail
                        .computeIfAbsent(firstMonth, k -> new HashMap<>())
                        .computeIfAbsent(b.getEmail(), k -> new ArrayList<>())
                        .add(b.getCreatedAt());
            }
        } catch (Exception ex) {
            log.warn("cohort load failed: {}", ex.toString());
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (var cohortEntry : byMonthByEmail.entrySet()) {
            String cohortMonth = cohortEntry.getKey();
            LocalDate cohortStart = LocalDate.parse(cohortMonth);
            int size = cohortEntry.getValue().size();

            List<Map<String, Object>> retention = new ArrayList<>();
            for (int offset = 1; offset <= horizon; offset++) {
                LocalDate windowStart = cohortStart.plusMonths(offset);
                LocalDate windowEnd   = cohortStart.plusMonths(offset + 1L);
                Instant ws = windowStart.atStartOfDay().toInstant(ZoneOffset.UTC);
                Instant we = windowEnd.atStartOfDay().toInstant(ZoneOffset.UTC);

                int returned = 0;
                for (List<Instant> bookings : cohortEntry.getValue().values()) {
                    for (Instant t : bookings) {
                        if (!t.isBefore(ws) && t.isBefore(we)) { returned++; break; }
                    }
                }
                Map<String, Object> cell = new LinkedHashMap<>();
                cell.put("offset", offset);
                cell.put("count", returned);
                cell.put("pct", size == 0 ? 0 :
                        BigDecimal.valueOf(returned).multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(size), 1, RoundingMode.HALF_UP));
                retention.add(cell);
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", cohortMonth);
            row.put("size", size);
            row.put("retention", retention);
            rows.add(row);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("horizonMonths", horizon);
        out.put("cohorts", rows);
        return out;
    }

    /** Top customers by lifetime paid revenue + summary statistics. */
    @GetMapping("/retention/ltv")
    public Map<String, Object> ltv(@RequestParam(defaultValue = "10") int limit) {
        Map<String, BigDecimal> revenueByEmail = new HashMap<>();
        Map<String, Integer> bookingsByEmail = new HashMap<>();
        Map<String, String> nameByEmail = new HashMap<>();

        try {
            for (BookingEntity b : bookings.findAll()) {
                if (b.getEmail() == null) continue;
                bookingsByEmail.merge(b.getEmail(), 1, Integer::sum);
                nameByEmail.putIfAbsent(b.getEmail(), b.getName());
                if ("PAID".equals(b.getPaymentStatus()) && b.getTotalAmountInr() != null) {
                    revenueByEmail.merge(b.getEmail(), b.getTotalAmountInr(), BigDecimal::add);
                }
            }
        } catch (Exception ex) { log.warn("ltv load failed: {}", ex.toString()); }

        long customers = bookingsByEmail.size();
        long repeat = bookingsByEmail.values().stream().filter(v -> v > 1).count();
        BigDecimal totalRev = revenueByEmail.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgLtv = customers == 0 ? BigDecimal.ZERO :
                totalRev.divide(BigDecimal.valueOf(customers), 2, RoundingMode.HALF_UP);

        List<Map<String, Object>> top = revenueByEmail.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("email", e.getKey());
                    row.put("name", nameByEmail.getOrDefault(e.getKey(), e.getKey()));
                    row.put("bookings", bookingsByEmail.getOrDefault(e.getKey(), 0));
                    row.put("revenueInr", e.getValue());
                    return row;
                })
                .toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalCustomers", customers);
        out.put("repeatCustomers", repeat);
        out.put("repeatRatePct", customers == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf(repeat).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(customers), 1, RoundingMode.HALF_UP));
        out.put("totalRevenueInr", totalRev);
        out.put("avgLtvInr", avgLtv);
        out.put("topCustomers", top);
        return out;
    }

    /** Bookings + revenue per month for the last N months. */
    @GetMapping("/retention/seasonality")
    public List<Map<String, Object>> seasonality(@RequestParam(defaultValue = "12") int months) {
        int range = Math.max(1, Math.min(months, 24));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = today.minusMonths(range - 1L).withDayOfMonth(1);

        Map<String, long[]> byMonth = new TreeMap<>();
        Map<String, BigDecimal> revByMonth = new TreeMap<>();
        for (int i = 0; i < range; i++) {
            String key = start.plusMonths(i).toString();
            byMonth.put(key, new long[]{0});
            revByMonth.put(key, BigDecimal.ZERO);
        }
        try {
            for (BookingEntity b : bookings.findAll()) {
                if (b.getCreatedAt() == null) continue;
                LocalDate d = b.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
                if (d.isBefore(start)) continue;
                String key = d.withDayOfMonth(1).toString();
                if (!byMonth.containsKey(key)) continue;
                byMonth.get(key)[0]++;
                if ("PAID".equals(b.getPaymentStatus()) && b.getTotalAmountInr() != null) {
                    revByMonth.merge(key, b.getTotalAmountInr(), BigDecimal::add);
                }
            }
        } catch (Exception ex) { log.warn("seasonality load failed: {}", ex.toString()); }

        List<Map<String, Object>> out = new ArrayList<>();
        for (var e : byMonth.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", e.getKey());
            row.put("bookings", e.getValue()[0]);
            row.put("revenueInr", revByMonth.get(e.getKey()));
            out.add(row);
        }
        return out;
    }

    /**
     * Multi-touch attribution. For each PAID booking we look up the customer's
     * visitor sessions via email → form_submit event → visitor_id → sessions,
     * then take first-touch and last-touch utm_source. Counts get aggregated
     * per source with revenue.
     */
    @GetMapping("/retention/attribution")
    public Map<String, Object> attribution(@RequestParam(required = false) String from,
                                           @RequestParam(required = false) String to) {
        Instant fromI = parseDate(from, true);
        Instant toI   = parseDate(to, false);
        if (fromI == null) fromI = LocalDate.now(ZoneOffset.UTC).minusDays(90).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (toI == null)   toI   = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        // email -> set of visitorIds
        Map<String, Set<String>> emailToVisitors = new HashMap<>();
        try {
            for (Object[] r : trackingEvents.emailToVisitor()) {
                if (r[0] == null || r[1] == null) continue;
                emailToVisitors.computeIfAbsent(String.valueOf(r[0]), k -> new HashSet<>())
                        .add(String.valueOf(r[1]));
            }
        } catch (Exception ex) { log.warn("attribution emailToVisitor failed: {}", ex.toString()); }

        // visitorId -> ordered list of (utm_source, startedAt)
        Set<String> allVisitors = emailToVisitors.values().stream()
                .flatMap(Set::stream).collect(Collectors.toSet());
        Map<String, List<Object[]>> sessionsByVisitor = new HashMap<>();
        if (!allVisitors.isEmpty()) {
            try {
                for (Object[] r : trackingSessions.sessionsForVisitors(allVisitors)) {
                    sessionsByVisitor.computeIfAbsent(String.valueOf(r[0]), k -> new ArrayList<>()).add(r);
                }
            } catch (Exception ex) { log.warn("attribution sessions load failed: {}", ex.toString()); }
        }

        Map<String, long[]> firstTouchCount = new HashMap<>();
        Map<String, BigDecimal> firstTouchRevenue = new HashMap<>();
        Map<String, long[]> lastTouchCount = new HashMap<>();
        Map<String, BigDecimal> lastTouchRevenue = new HashMap<>();
        long attributed = 0, unattributed = 0;
        BigDecimal totalRev = BigDecimal.ZERO;

        for (BookingEntity b : bookings.findAll()) {
            if (!"PAID".equals(b.getPaymentStatus())) continue;
            if (!inRange(b.getCreatedAt(), fromI, toI)) continue;
            if (b.getEmail() == null) continue;
            BigDecimal rev = b.getTotalAmountInr() == null ? BigDecimal.ZERO : b.getTotalAmountInr();
            totalRev = totalRev.add(rev);

            Set<String> visitors = emailToVisitors.get(b.getEmail());
            if (visitors == null || visitors.isEmpty()) { unattributed++; continue; }

            String firstSrc = null, lastSrc = null;
            Instant earliest = null, latest = null;
            for (String vid : visitors) {
                for (Object[] r : sessionsByVisitor.getOrDefault(vid, List.of())) {
                    Object srcObj = r[1];
                    Object tsObj  = r[2];
                    if (tsObj == null) continue;
                    Instant t = (tsObj instanceof Instant ti) ? ti :
                                (tsObj instanceof java.sql.Timestamp ts) ? ts.toInstant() : null;
                    if (t == null) continue;
                    String src = srcObj == null || String.valueOf(srcObj).isBlank() ? "direct" : String.valueOf(srcObj);
                    if (earliest == null || t.isBefore(earliest)) { earliest = t; firstSrc = src; }
                    if (latest == null || t.isAfter(latest))      { latest = t;   lastSrc = src; }
                }
            }
            if (firstSrc == null) { unattributed++; continue; }
            attributed++;
            firstTouchCount.computeIfAbsent(firstSrc, k -> new long[]{0})[0]++;
            firstTouchRevenue.merge(firstSrc, rev, BigDecimal::add);
            lastTouchCount.computeIfAbsent(lastSrc, k -> new long[]{0})[0]++;
            lastTouchRevenue.merge(lastSrc, rev, BigDecimal::add);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("attributedBookings", attributed);
        out.put("unattributedBookings", unattributed);
        out.put("totalRevenueInr", totalRev);
        out.put("firstTouch", buildAttributionRows(firstTouchCount, firstTouchRevenue));
        out.put("lastTouch", buildAttributionRows(lastTouchCount, lastTouchRevenue));
        return out;
    }

    private static List<Map<String, Object>> buildAttributionRows(Map<String, long[]> counts,
                                                                  Map<String, BigDecimal> revenue) {
        return counts.entrySet().stream()
                .sorted((a, b) -> b.getValue()[0] != a.getValue()[0]
                        ? Long.compare(b.getValue()[0], a.getValue()[0])
                        : revenue.getOrDefault(b.getKey(), BigDecimal.ZERO)
                                 .compareTo(revenue.getOrDefault(a.getKey(), BigDecimal.ZERO)))
                .map(e -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("source", e.getKey());
                    r.put("bookings", e.getValue()[0]);
                    r.put("revenueInr", revenue.getOrDefault(e.getKey(), BigDecimal.ZERO));
                    return r;
                }).toList();
    }

    /** Daily-visitor forecast. */
    @GetMapping("/forecast/visitors")
    public Map<String, Object> forecastVisitors(@RequestParam(defaultValue = "90") int historyDays,
                                                @RequestParam(defaultValue = "30") int horizonDays) {
        int hist = Math.max(7, Math.min(historyDays, 365));
        int hori = Math.max(1, Math.min(horizonDays, 90));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant from = today.minusDays(hist - 1L).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to   = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<String, Long> byDay = new LinkedHashMap<>();
        for (int i = 0; i < hist; i++) byDay.put(today.minusDays(hist - 1L - i).toString(), 0L);
        try {
            for (Object[] row : trackingEvents.dailyVisitorCounts(from, to)) {
                String day = row[0].toString();
                if (day.length() > 10) day = day.substring(0, 10);
                byDay.put(day, ((Number) row[1]).longValue());
            }
        } catch (Exception ex) { log.warn("forecast/visitors load failed: {}", ex.toString()); }

        List<ForecastService.Point> series = new ArrayList<>();
        for (var e : byDay.entrySet()) {
            series.add(new ForecastService.Point(LocalDate.parse(e.getKey()), e.getValue().doubleValue()));
        }
        return forecast.toJson(forecast.forecast(series, hori));
    }

    /** Daily-booking forecast (from bookings.createdAt). */
    @GetMapping("/forecast/bookings")
    public Map<String, Object> forecastBookings(@RequestParam(defaultValue = "90") int historyDays,
                                                @RequestParam(defaultValue = "30") int horizonDays) {
        int hist = Math.max(7, Math.min(historyDays, 365));
        int hori = Math.max(1, Math.min(horizonDays, 90));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        Map<String, Long> byDay = new LinkedHashMap<>();
        for (int i = 0; i < hist; i++) byDay.put(today.minusDays(hist - 1L - i).toString(), 0L);

        try {
            for (BookingEntity b : bookings.findAll()) {
                if (b.getCreatedAt() == null) continue;
                LocalDate d = b.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
                if (d.isBefore(today.minusDays(hist - 1L)) || d.isAfter(today)) continue;
                byDay.merge(d.toString(), 1L, Long::sum);
            }
        } catch (Exception ex) { log.warn("forecast/bookings load failed: {}", ex.toString()); }

        List<ForecastService.Point> series = new ArrayList<>();
        for (var e : byDay.entrySet()) {
            series.add(new ForecastService.Point(LocalDate.parse(e.getKey()), e.getValue().doubleValue()));
        }
        return forecast.toJson(forecast.forecast(series, hori));
    }

    /** Daily paid-revenue forecast (₹). */
    @GetMapping("/forecast/revenue")
    public Map<String, Object> forecastRevenue(@RequestParam(defaultValue = "90") int historyDays,
                                               @RequestParam(defaultValue = "30") int horizonDays) {
        int hist = Math.max(7, Math.min(historyDays, 365));
        int hori = Math.max(1, Math.min(horizonDays, 90));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        Map<String, BigDecimal> byDay = new LinkedHashMap<>();
        for (int i = 0; i < hist; i++) byDay.put(today.minusDays(hist - 1L - i).toString(), BigDecimal.ZERO);

        try {
            for (BookingEntity b : bookings.findAll()) {
                if (!"PAID".equals(b.getPaymentStatus())) continue;
                Instant t = b.getPaidAt() != null ? b.getPaidAt() : b.getCreatedAt();
                if (t == null) continue;
                LocalDate d = t.atZone(ZoneOffset.UTC).toLocalDate();
                if (d.isBefore(today.minusDays(hist - 1L)) || d.isAfter(today)) continue;
                BigDecimal amt = b.getTotalAmountInr() == null ? BigDecimal.ZERO : b.getTotalAmountInr();
                byDay.merge(d.toString(), amt, BigDecimal::add);
            }
        } catch (Exception ex) { log.warn("forecast/revenue load failed: {}", ex.toString()); }

        List<ForecastService.Point> series = new ArrayList<>();
        for (var e : byDay.entrySet()) {
            series.add(new ForecastService.Point(LocalDate.parse(e.getKey()), e.getValue().doubleValue()));
        }
        return forecast.toJson(forecast.forecast(series, hori));
    }

    /** Top-level projections vs the equivalent prior window. */
    @GetMapping("/forecast/summary")
    public Map<String, Object> forecastSummary(@RequestParam(defaultValue = "30") int horizonDays) {
        Map<String, Object> visitors = forecastVisitors(90, horizonDays);
        Map<String, Object> bookings = forecastBookings(90, horizonDays);
        Map<String, Object> revenue  = forecastRevenue(90, horizonDays);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("horizonDays", horizonDays);
        out.put("visitors", projectionCard(visitors, horizonDays));
        out.put("bookings", projectionCard(bookings, horizonDays));
        out.put("revenue",  projectionCard(revenue, horizonDays));
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> projectionCard(Map<String, Object> fc, int horizonDays) {
        Map<String, Object> card = new LinkedHashMap<>();
        boolean ok = Boolean.TRUE.equals(fc.get("sufficient"));
        card.put("sufficient", ok);
        if (!ok) {
            card.put("note", fc.get("note"));
            card.put("horizonTotal", null);
            card.put("priorPeriodTotal", null);
            card.put("deltaPct", null);
            return card;
        }
        Object horizonTotal = fc.get("horizonTotal");
        card.put("horizonTotal", horizonTotal);

        // Prior-period: sum of actuals over the last `horizonDays` history points
        double prior = 0;
        for (Map<String, Object> p : (List<Map<String, Object>>) fc.get("series")) {
            Object actual = p.get("actual");
            if (actual != null) prior += ((Number) actual).doubleValue();
        }
        // The above sums ALL history; we want only the last horizonDays of it
        List<Map<String, Object>> hist = ((List<Map<String, Object>>) fc.get("series")).stream()
                .filter(p -> p.get("actual") != null).toList();
        int n = hist.size();
        int start = Math.max(0, n - horizonDays);
        double priorWindow = 0;
        for (int i = start; i < n; i++) {
            Object v = hist.get(i).get("actual");
            if (v != null) priorWindow += ((Number) v).doubleValue();
        }
        card.put("priorPeriodTotal", BigDecimal.valueOf(priorWindow).setScale(2, RoundingMode.HALF_UP));
        double projected = horizonTotal == null ? 0 : ((Number) horizonTotal).doubleValue();
        BigDecimal delta;
        if (priorWindow == 0) {
            delta = projected == 0 ? BigDecimal.ZERO : null;
        } else {
            delta = BigDecimal.valueOf((projected - priorWindow) * 100 / priorWindow)
                    .setScale(1, RoundingMode.HALF_UP);
        }
        card.put("deltaPct", delta);
        return card;
    }

    /**
     * Sales-specific extras: payment-method mix, booking lead-time histogram,
     * cancellation stats, currency mix. All computed in-app from bookings.
     */
    @GetMapping("/sales/extras")
    public Map<String, Object> salesExtras(@RequestParam(required = false) String from,
                                           @RequestParam(required = false) String to) {
        Instant fromI = parseDate(from, true);
        Instant toI   = parseDate(to, false);

        List<BookingEntity> bs;
        try {
            bs = bookings.findAll().stream()
                    .filter(b -> inRange(b.getCreatedAt(), fromI, toI))
                    .toList();
        } catch (Exception ex) {
            log.warn("sales/extras load failed: {}", ex.toString());
            bs = List.of();
        }

        // Payment method mix (count + paid revenue per method)
        Map<String, long[]> pmCount = new LinkedHashMap<>();
        Map<String, BigDecimal> pmRevenue = new LinkedHashMap<>();
        for (BookingEntity b : bs) {
            String m = b.getPaymentMethod();
            if (m == null || m.isBlank()) m = "unknown";
            pmCount.computeIfAbsent(m, k -> new long[]{0})[0]++;
            if ("PAID".equals(b.getPaymentStatus()) && b.getTotalAmountInr() != null) {
                pmRevenue.merge(m, b.getTotalAmountInr(), BigDecimal::add);
            }
        }
        List<Map<String, Object>> paymentMix = pmCount.entrySet().stream()
                .map(e -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("method", e.getKey());
                    r.put("bookings", e.getValue()[0]);
                    r.put("revenueInr", pmRevenue.getOrDefault(e.getKey(), BigDecimal.ZERO));
                    return r;
                })
                .sorted((a, b) -> Long.compare((long) b.get("bookings"), (long) a.get("bookings")))
                .toList();

        // Lead time histogram: days between createdAt and travelDate
        long lt0_7 = 0, lt7_30 = 0, lt30_60 = 0, lt60_plus = 0, ltUnknown = 0;
        long ltSum = 0, ltSamples = 0;
        for (BookingEntity b : bs) {
            if (b.getCreatedAt() == null || b.getTravelDate() == null) { ltUnknown++; continue; }
            LocalDate created = b.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            long days = ChronoUnit.DAYS.between(created, b.getTravelDate());
            if (days < 0) { ltUnknown++; continue; }
            ltSum += days; ltSamples++;
            if      (days < 7)   lt0_7++;
            else if (days < 30)  lt7_30++;
            else if (days < 60)  lt30_60++;
            else                 lt60_plus++;
        }
        List<Map<String, Object>> leadTime = new ArrayList<>();
        leadTime.add(leadBucket("<7d",      lt0_7));
        leadTime.add(leadBucket("7–30d",    lt7_30));
        leadTime.add(leadBucket("30–60d",   lt30_60));
        leadTime.add(leadBucket("60d+",     lt60_plus));
        if (ltUnknown > 0) leadTime.add(leadBucket("unknown", ltUnknown));

        // Cancellation + win-rate stats
        long total = bs.size();
        long cancelled = bs.stream().filter(b -> "CANCELLED".equals(b.getStatus())).count();
        long paid = bs.stream().filter(b -> "PAID".equals(b.getPaymentStatus())).count();
        BigDecimal cancelledRevenue = bs.stream()
                .filter(b -> "CANCELLED".equals(b.getStatus()))
                .map(BookingEntity::getTotalAmountInr)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Currency mix
        Map<String, long[]> curCount = new LinkedHashMap<>();
        for (BookingEntity b : bs) {
            String c = b.getCurrency() == null || b.getCurrency().isBlank() ? "INR" : b.getCurrency();
            curCount.computeIfAbsent(c, k -> new long[]{0})[0]++;
        }
        List<Map<String, Object>> currencyMix = curCount.entrySet().stream()
                .map(e -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("currency", e.getKey());
                    r.put("bookings", e.getValue()[0]);
                    return r;
                })
                .sorted((a, b) -> Long.compare((long) b.get("bookings"), (long) a.get("bookings")))
                .toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("paymentMix", paymentMix);
        out.put("leadTime", leadTime);
        Map<String, Object> leadStats = new LinkedHashMap<>();
        leadStats.put("avgDays", ltSamples == 0 ? null :
                BigDecimal.valueOf(ltSum).divide(BigDecimal.valueOf(ltSamples), 1, RoundingMode.HALF_UP));
        leadStats.put("samples", ltSamples);
        out.put("leadTimeStats", leadStats);
        Map<String, Object> cancel = new LinkedHashMap<>();
        cancel.put("cancelled", cancelled);
        cancel.put("total", total);
        cancel.put("ratePct", total == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf(cancelled).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP));
        cancel.put("revenueLostInr", cancelledRevenue);
        out.put("cancellation", cancel);
        Map<String, Object> winRate = new LinkedHashMap<>();
        winRate.put("paid", paid);
        winRate.put("total", total);
        winRate.put("ratePct", total == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf(paid).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP));
        out.put("winRate", winRate);
        out.put("currencyMix", currencyMix);
        return out;
    }

    private static Map<String, Object> leadBucket(String label, long count) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bucket", label);
        m.put("count", count);
        return m;
    }

    // ---- helpers ----

    private static boolean isConfirmed(BookingEntity b) {
        String s = b.getStatus();
        return "CONFIRMED".equals(s) || "IN_PROGRESS".equals(s) || "COMPLETED".equals(s)
                || "CONVERTED".equals(s);
    }

    private static boolean inRange(Instant t, Instant from, Instant to) {
        if (t == null) return false;
        if (from != null && t.isBefore(from)) return false;
        if (to != null && !t.isBefore(to)) return false;
        return true;
    }

    private static long countCreatedInRange(List<Instant> times, Instant from, Instant to) {
        return times.stream().filter(t -> inRange(t, from, to)).count();
    }

    private static long safeCount(java.util.function.LongSupplier s) {
        try { return s.getAsLong(); } catch (Exception ex) {
            log.warn("Analytics: count subquery failed: {}", ex.toString());
            return 0L;
        }
    }

    private static Instant parseDate(String iso, boolean startOfDay) {
        if (iso == null || iso.isBlank()) return null;
        try {
            LocalDate d = LocalDate.parse(iso.trim());
            return (startOfDay ? d.atStartOfDay() : d.plusDays(1).atStartOfDay()).toInstant(ZoneOffset.UTC);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String bucketKey(Instant ts, String groupBy) {
        LocalDate d = ts.atZone(ZoneOffset.UTC).toLocalDate();
        return switch (groupBy.toLowerCase()) {
            case "month" -> d.withDayOfMonth(1).toString();
            case "week"  -> d.minusDays(d.getDayOfWeek().getValue() - 1L).toString();
            default      -> d.toString();
        };
    }

    private static String nullSafe(String s, String def) { return s == null ? def : s; }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s; }

    private static long num(Object o) { return o == null ? 0L : ((Number) o).longValue(); }

    private static Map<String, Object> stage(String id, String label, Long count, boolean instrumented) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("label", label);
        m.put("count", count);
        m.put("instrumented", instrumented);
        return m;
    }

    private static BigDecimal pctDelta(long now, long prev) {
        if (prev == 0) return now == 0 ? BigDecimal.ZERO : null;
        return BigDecimal.valueOf(now - prev)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(prev), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal pctDelta(BigDecimal now, BigDecimal prev) {
        if (prev == null || prev.signum() == 0) return now == null || now.signum() == 0 ? BigDecimal.ZERO : null;
        return now.subtract(prev)
                .multiply(BigDecimal.valueOf(100))
                .divide(prev, 1, RoundingMode.HALF_UP);
    }
}
