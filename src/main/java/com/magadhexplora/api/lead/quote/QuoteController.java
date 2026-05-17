package com.magadhexplora.api.lead.quote;

import com.magadhexplora.api.lead.StatusUpdateRequest;
import com.magadhexplora.api.lead.abandoned.RecoveryAttributionService;
import com.magadhexplora.api.mail.EmailService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class QuoteController {

    private final QuoteRepository repo;
    private final EmailService email;
    private final RecoveryAttributionService recoveryAttribution;

    public QuoteController(QuoteRepository repo,
                           EmailService email,
                           RecoveryAttributionService recoveryAttribution) {
        this.repo = repo;
        this.email = email;
        this.recoveryAttribution = recoveryAttribution;
    }

    @PostMapping("/api/quote")
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteDto submit(@Valid @RequestBody QuoteDto req) {
        QuoteEntity saved = repo.save(req.toEntity());
        email.sendQuoteEmails(saved);
        recoveryAttribution.markConverted(saved.getEmail());
        return QuoteDto.from(saved);
    }

    @GetMapping("/api/admin/quotes")
    public Page<QuoteDto> adminList(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return repo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(QuoteDto::from);
    }

    @PatchMapping("/api/admin/quotes/{id}/status")
    public QuoteDto updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest req) {
        QuoteEntity q = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote not found"));
        q.setStatus(req.getStatus().toUpperCase());
        return QuoteDto.from(repo.save(q));
    }
}
