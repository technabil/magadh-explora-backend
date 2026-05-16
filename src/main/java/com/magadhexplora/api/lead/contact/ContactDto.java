package com.magadhexplora.api.lead.contact;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class ContactDto {

    private Long id;

    @NotBlank @Size(max = 120)
    private String name;

    @NotBlank @Email @Size(max = 180)
    private String email;

    @JsonAlias({"phone"})
    @Size(max = 32)
    private String mobile;

    @Size(max = 200)
    private String subject;

    @NotBlank
    private String message;

    private String status;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static ContactDto from(ContactEntity e) {
        ContactDto d = new ContactDto();
        d.id = e.getId();
        d.name = e.getName();
        d.email = e.getEmail();
        d.mobile = e.getMobile();
        d.subject = e.getSubject();
        d.message = e.getMessage();
        d.status = e.getStatus();
        d.createdAt = e.getCreatedAt();
        return d;
    }

    public ContactEntity toEntity() {
        ContactEntity e = new ContactEntity();
        e.setName(name.trim());
        e.setEmail(email.trim().toLowerCase());
        e.setMobile(mobile);
        e.setSubject(subject);
        e.setMessage(message);
        return e;
    }
}
