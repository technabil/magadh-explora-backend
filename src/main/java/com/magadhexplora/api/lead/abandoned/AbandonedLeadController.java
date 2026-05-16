package com.magadhexplora.api.lead.abandoned;

import com.magadhexplora.api.lead.StatusUpdateRequest;
import com.magadhexplora.api.mail.EmailService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@RestController
public class AbandonedLeadController {

    private static final Set<String> ALLOWED_SOURCES = Set.of(
            "quote-modal", "book-now-modal", "package-detail-quote", "contact-page"
    );

    private final AbandonedLeadRepository repo;
    private final EmailService email;

    public AbandonedLeadController(AbandonedLeadRepository repo, EmailService email) {
        this.repo = repo;
        this.email = email;
    }

    @PostMapping("/api/leads/abandoned")
    @ResponseStatus(HttpStatus.CREATED)
    public AbandonedLeadDto capture(@Valid @RequestBody AbandonedLeadDto req) {
        String src = req.getSource() == null ? "" : req.getSource().trim().toLowerCase();
        if (!ALLOWED_SOURCES.contains(src)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown source");
        }

        String emailNorm = (req.getEmail() == null || req.getEmail().isBlank()) ? null
                : req.getEmail().trim().toLowerCase();
        String mobileNorm = (req.getMobile() == null || req.getMobile().isBlank()) ? null
                : req.getMobile().trim();

        if (emailNorm == null && mobileNorm == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email or mobile required");
        }

        // Dedup (1h window): prefer email-based lookup, fall back to mobile when email missing
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        var existing = (emailNorm != null)
                ? repo.findFirstByEmailIgnoreCaseAndSourceAndCreatedAtAfterOrderByCreatedAtDesc(
                        emailNorm, src, oneHourAgo)
                : repo.findFirstByMobileAndSourceAndCreatedAtAfterOrderByCreatedAtDesc(
                        mobileNorm, src, oneHourAgo);

        if (existing.isPresent()) {
            AbandonedLeadEntity e = existing.get();
            if (req.getName() != null && !req.getName().isBlank()) e.setName(req.getName().trim());
            if (emailNorm != null) e.setEmail(emailNorm);
            if (mobileNorm != null) e.setMobile(mobileNorm);
            if (req.getFormState() != null) e.setFormState(req.getFormState());
            return AbandonedLeadDto.from(repo.save(e));
        }

        AbandonedLeadEntity saved = repo.save(req.toEntity());
        email.sendAbandonedLeadNotification(saved);
        return AbandonedLeadDto.from(saved);
    }

    @GetMapping("/api/admin/leads/abandoned")
    public Page<AbandonedLeadDto> adminList(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return repo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(AbandonedLeadDto::from);
    }

    @PatchMapping("/api/admin/leads/abandoned/{id}/status")
    public AbandonedLeadDto updateStatus(@PathVariable Long id,
                                         @Valid @RequestBody StatusUpdateRequest req) {
        AbandonedLeadEntity e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));
        e.setStatus(req.getStatus().toUpperCase());
        return AbandonedLeadDto.from(repo.save(e));
    }

    @DeleteMapping("/api/admin/leads/abandoned/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found");
        }
        repo.deleteById(id);
    }
}
