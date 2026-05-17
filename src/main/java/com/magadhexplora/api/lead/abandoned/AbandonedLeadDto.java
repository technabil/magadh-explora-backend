package com.magadhexplora.api.lead.abandoned;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class AbandonedLeadDto {

    private Long id;

    @NotBlank @Size(max = 40)
    private String source;

    @Size(max = 120)
    private String name;

    @Email @Size(max = 180)
    private String email;

    @Size(max = 32)
    private String mobile;

    private String formState;
    private String status;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    // Recovery sequence state — surfaced to admin so they know who's been touched
    private int attempts;
    private Instant lastTouchedAt;
    private Instant nextTouchAt;
    private String lastTouchChannel;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getFormState() { return formState; }
    public void setFormState(String formState) { this.formState = formState; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public Instant getLastTouchedAt() { return lastTouchedAt; }
    public void setLastTouchedAt(Instant lastTouchedAt) { this.lastTouchedAt = lastTouchedAt; }

    public Instant getNextTouchAt() { return nextTouchAt; }
    public void setNextTouchAt(Instant nextTouchAt) { this.nextTouchAt = nextTouchAt; }

    public String getLastTouchChannel() { return lastTouchChannel; }
    public void setLastTouchChannel(String lastTouchChannel) { this.lastTouchChannel = lastTouchChannel; }

    public static AbandonedLeadDto from(AbandonedLeadEntity e) {
        AbandonedLeadDto d = new AbandonedLeadDto();
        d.id = e.getId();
        d.source = e.getSource();
        d.name = e.getName();
        d.email = e.getEmail();
        d.mobile = e.getMobile();
        d.formState = e.getFormState();
        d.status = e.getStatus();
        d.notes = e.getNotes();
        d.createdAt = e.getCreatedAt();
        d.updatedAt = e.getUpdatedAt();
        d.attempts = e.getAttempts();
        d.lastTouchedAt = e.getLastTouchedAt();
        d.nextTouchAt = e.getNextTouchAt();
        d.lastTouchChannel = e.getLastTouchChannel();
        return d;
    }

    public AbandonedLeadEntity toEntity() {
        AbandonedLeadEntity e = new AbandonedLeadEntity();
        e.setSource(source == null ? null : source.trim().toLowerCase());
        e.setName(name == null || name.isBlank() ? null : name.trim());
        e.setEmail(email == null || email.isBlank() ? null : email.trim().toLowerCase());
        e.setMobile(mobile == null || mobile.isBlank() ? null : mobile.trim());
        e.setFormState(formState);
        return e;
    }
}
