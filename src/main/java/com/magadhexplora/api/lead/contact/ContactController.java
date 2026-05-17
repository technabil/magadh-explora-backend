package com.magadhexplora.api.lead.contact;

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
public class ContactController {

    private final ContactRepository repo;
    private final EmailService email;
    private final RecoveryAttributionService recoveryAttribution;

    public ContactController(ContactRepository repo,
                             EmailService email,
                             RecoveryAttributionService recoveryAttribution) {
        this.repo = repo;
        this.email = email;
        this.recoveryAttribution = recoveryAttribution;
    }

    @PostMapping("/api/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactDto submit(@Valid @RequestBody ContactDto req) {
        ContactEntity saved = repo.save(req.toEntity());
        email.sendContactEmails(saved);
        recoveryAttribution.markConverted(saved.getEmail());
        return ContactDto.from(saved);
    }

    @GetMapping("/api/admin/contacts")
    public Page<ContactDto> adminList(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return repo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(ContactDto::from);
    }

    @PatchMapping("/api/admin/contacts/{id}/status")
    public ContactDto updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest req) {
        ContactEntity c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
        c.setStatus(req.getStatus().toUpperCase());
        return ContactDto.from(repo.save(c));
    }
}
