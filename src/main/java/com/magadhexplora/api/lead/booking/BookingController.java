package com.magadhexplora.api.lead.booking;

import com.magadhexplora.api.catalog.pkg.PackageEntity;
import com.magadhexplora.api.catalog.pkg.PackageRepository;
import com.magadhexplora.api.currency.CurrencyService;
import com.magadhexplora.api.lead.StatusUpdateRequest;
import com.magadhexplora.api.lead.abandoned.RecoveryAttributionService;
import com.magadhexplora.api.mail.EmailService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
public class BookingController {

    private final BookingRepository repo;
    private final PackageRepository packageRepo;
    private final CurrencyService currencyService;
    private final EmailService email;
    private final RecoveryAttributionService recoveryAttribution;

    public BookingController(BookingRepository repo, PackageRepository packageRepo,
                             CurrencyService currencyService, EmailService email,
                             RecoveryAttributionService recoveryAttribution) {
        this.repo = repo;
        this.packageRepo = packageRepo;
        this.currencyService = currencyService;
        this.email = email;
        this.recoveryAttribution = recoveryAttribution;
    }

    @PostMapping("/api/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDto submit(@Valid @RequestBody BookingDto req) {
        PackageEntity pkg = packageRepo.findById(req.getPackageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown package"));

        BookingEntity b = req.toEntity();

        BigDecimal pricePerTraveler = pkg.getPriceInr() == null ? BigDecimal.ZERO : pkg.getPriceInr();
        BigDecimal totalInr = pricePerTraveler.multiply(BigDecimal.valueOf(b.getNumTravelers()));
        b.setTotalAmountInr(totalInr);

        String currency = (b.getCurrency() == null || b.getCurrency().isBlank()) ? "INR" : b.getCurrency().toUpperCase();
        b.setCurrency(currency);
        b.setTotalAmountLocal(convertWithMarkup(totalInr, currency));

        b.setViewToken(generateViewToken());

        BookingEntity saved = repo.save(b);
        email.sendBookingEmails(saved);
        recoveryAttribution.markConverted(saved.getEmail());
        return BookingDto.from(saved);
    }

    /** Public passwordless view of a single booking. Token is unguessable (~128 bits). */
    @GetMapping("/api/bookings/view/{token}")
    public BookingViewDto viewByToken(@PathVariable String token) {
        if (token == null || token.length() < 16) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        BookingEntity b = repo.findByViewToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        PackageEntity pkg = packageRepo.findById(b.getPackageId()).orElse(null);
        return BookingViewDto.from(b, pkg);
    }

    /**
     * Recover the magic-link token using Booking ID + Email (for users who lost the email).
     * Returns only the token — minimal info leak. Same 404 whether ID is wrong or email mismatches.
     */
    @PostMapping("/api/bookings/lookup")
    public java.util.Map<String, String> lookup(@RequestBody LookupRequest req) {
        if (req == null || req.id == null || req.email == null || req.email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        BookingEntity b = repo.findById(req.id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!b.getEmail().equalsIgnoreCase(req.email.trim())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        return java.util.Map.of("token", b.getViewToken());
    }

    public static class LookupRequest {
        public Long id;
        public String email;
    }

    private String generateViewToken() {
        // 2 UUIDs concatenated as hex = 64 chars / 256 bits — far more than enough
        return (UUID.randomUUID().toString() + UUID.randomUUID().toString()).replace("-", "");
    }

    private static final Set<String> VALID_STATUSES = Set.of(
            "NEW", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED",
            // legacy lead-style statuses still accepted for backward compat
            "PENDING", "CONTACTED", "CONVERTED", "CLOSED"
    );

    private static final Set<String> VALID_PAYMENT_STATUSES = Set.of(
            "UNPAID", "PARTIAL", "PAID", "REFUNDED", "FAILED"
    );

    @GetMapping("/api/admin/bookings")
    public Page<BookingDto> adminList(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String paymentStatus,
                                      @RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repo.findFiltered(emptyToNull(status), emptyToNull(paymentStatus),
                        parseDate(from, true), parseDate(to, false), pageable)
                .map(BookingDto::from);
    }

    @GetMapping("/api/admin/bookings/{id}")
    public BookingDto adminGet(@PathVariable Long id) {
        return BookingDto.from(repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found")));
    }

    @PatchMapping("/api/admin/bookings/{id}/status")
    public BookingDto updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest req) {
        BookingEntity b = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        String s = req.getStatus().toUpperCase();
        if (!VALID_STATUSES.contains(s)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + s);
        }
        b.setStatus(s);
        Instant now = Instant.now();
        if ("CONFIRMED".equals(s) && b.getConfirmedAt() == null) b.setConfirmedAt(now);
        return BookingDto.from(repo.save(b));
    }

    @PatchMapping("/api/admin/bookings/{id}/payment-status")
    public BookingDto updatePaymentStatus(@PathVariable Long id, @RequestBody PaymentStatusRequest req) {
        BookingEntity b = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        String s = req.paymentStatus == null ? "" : req.paymentStatus.toUpperCase();
        if (!VALID_PAYMENT_STATUSES.contains(s)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment status: " + s);
        }
        b.setPaymentStatus(s);
        if ("PAID".equals(s) && b.getPaidAt() == null) b.setPaidAt(Instant.now());
        return BookingDto.from(repo.save(b));
    }

    @PatchMapping("/api/admin/bookings/{id}/notes")
    public BookingDto updateNotes(@PathVariable Long id, @RequestBody NotesRequest req) {
        BookingEntity b = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        b.setInternalNotes(req.notes);
        return BookingDto.from(repo.save(b));
    }

    @PostMapping("/api/admin/bookings/{id}/cancel")
    public BookingDto cancel(@PathVariable Long id, @RequestBody CancelRequest req) {
        BookingEntity b = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        b.setStatus("CANCELLED");
        b.setCancelledAt(Instant.now());
        b.setCancellationReason(req == null ? null : req.reason);
        BookingEntity saved = repo.save(b);
        email.sendBookingCancellation(saved);
        return BookingDto.from(saved);
    }

    @GetMapping(value = "/api/admin/bookings.csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String paymentStatus,
                                            @RequestParam(required = false) String from,
                                            @RequestParam(required = false) String to) {
        var rows = repo.findFiltered(emptyToNull(status), emptyToNull(paymentStatus),
                parseDate(from, true), parseDate(to, false),
                PageRequest.of(0, 5000, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();

        StringBuilder sb = new StringBuilder(4096);
        sb.append("id,createdAt,name,email,mobile,packageId,numTravelers,travelDate,currency,totalAmountInr,paymentMethod,status,paymentStatus\n");
        for (BookingEntity b : rows) {
            sb.append(b.getId()).append(',')
              .append(b.getCreatedAt() == null ? "" : b.getCreatedAt()).append(',')
              .append(csv(b.getName())).append(',')
              .append(csv(b.getEmail())).append(',')
              .append(csv(b.getMobile())).append(',')
              .append(b.getPackageId()).append(',')
              .append(b.getNumTravelers()).append(',')
              .append(b.getTravelDate() == null ? "" : b.getTravelDate()).append(',')
              .append(csv(b.getCurrency())).append(',')
              .append(b.getTotalAmountInr()).append(',')
              .append(csv(b.getPaymentMethod())).append(',')
              .append(csv(b.getStatus())).append(',')
              .append(csv(b.getPaymentStatus())).append('\n');
        }
        String filename = "bookings-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                .body(sb.toString());
    }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim().toUpperCase(); }

    private static Instant parseDate(String iso, boolean startOfDay) {
        if (iso == null || iso.isBlank()) return null;
        try {
            LocalDate d = LocalDate.parse(iso.trim());
            return (startOfDay ? d.atStartOfDay() : d.plusDays(1).atStartOfDay()).toInstant(ZoneOffset.UTC);
        } catch (Exception ex) {
            return null;
        }
    }

    public static class PaymentStatusRequest { public String paymentStatus; }
    public static class NotesRequest { public String notes; }
    public static class CancelRequest { public String reason; }

    @GetMapping("/api/bookings/me")
    public List<BookingDto> mine() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return repo.findByEmailIgnoreCaseOrderByCreatedAtDesc(auth.getName())
                .stream().map(BookingDto::from).toList();
    }

    private BigDecimal convertWithMarkup(BigDecimal inr, String currency) {
        var snap = currencyService.snapshot();
        BigDecimal rateToInr = snap.getRatesToInr().getOrDefault(currency, BigDecimal.ONE);
        if (rateToInr.signum() <= 0) rateToInr = BigDecimal.ONE;
        BigDecimal base = inr.divide(rateToInr, 8, RoundingMode.HALF_UP);
        BigDecimal markupMultiplier = BigDecimal.ONE.add(
                snap.getMarkupPercent() == null
                        ? BigDecimal.ZERO
                        : snap.getMarkupPercent().movePointLeft(2));
        return base.multiply(markupMultiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
