package com.magadhexplora.api.lead.booking;

import com.magadhexplora.api.catalog.pkg.PackageEntity;
import com.magadhexplora.api.catalog.pkg.PackageRepository;
import com.magadhexplora.api.currency.CurrencyService;
import com.magadhexplora.api.lead.StatusUpdateRequest;
import com.magadhexplora.api.mail.EmailService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@RestController
public class BookingController {

    private final BookingRepository repo;
    private final PackageRepository packageRepo;
    private final CurrencyService currencyService;
    private final EmailService email;

    public BookingController(BookingRepository repo, PackageRepository packageRepo,
                             CurrencyService currencyService, EmailService email) {
        this.repo = repo;
        this.packageRepo = packageRepo;
        this.currencyService = currencyService;
        this.email = email;
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

        BookingEntity saved = repo.save(b);
        email.sendBookingEmails(saved);
        return BookingDto.from(saved);
    }

    @GetMapping("/api/admin/bookings")
    public Page<BookingDto> adminList(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return repo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(BookingDto::from);
    }

    @PatchMapping("/api/admin/bookings/{id}/status")
    public BookingDto updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest req) {
        BookingEntity b = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        b.setStatus(req.getStatus().toUpperCase());
        return BookingDto.from(repo.save(b));
    }

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
