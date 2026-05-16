package com.magadhexplora.api.analytics;

import com.magadhexplora.api.lead.booking.BookingEntity;
import com.magadhexplora.api.lead.booking.BookingRepository;
import com.magadhexplora.api.lead.contact.ContactRepository;
import com.magadhexplora.api.lead.quote.QuoteRepository;
import com.magadhexplora.api.catalog.pkg.PackageEntity;
import com.magadhexplora.api.catalog.pkg.PackageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

    public AnalyticsController(BookingRepository bookings,
                               QuoteRepository quotes,
                               ContactRepository contacts,
                               PackageRepository packages) {
        this.bookings = bookings;
        this.quotes = quotes;
        this.contacts = contacts;
        this.packages = packages;
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
}
