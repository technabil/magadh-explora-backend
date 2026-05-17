package com.magadhexplora.api.lead.abandoned;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "abandoned_leads")
public class AbandonedLeadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Where in the UI the user abandoned: "quote-modal", "book-now-modal", "package-detail-quote", "contact-page" */
    @Column(nullable = false, length = 40)
    private String source;

    @Column(length = 120)
    private String name;

    @Column(length = 180)
    private String email;

    @Column(length = 32)
    private String mobile;

    /** JSON snapshot of form fields beyond name/email/phone */
    @Column(name = "form_state", columnDefinition = "MEDIUMTEXT")
    private String formState;

    @Column(nullable = false, length = 32)
    private String status = "NEW";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "last_touched_at")
    private Instant lastTouchedAt;

    @Column(name = "next_touch_at")
    private Instant nextTouchAt;

    @Column(name = "last_touch_channel", length = 20)
    private String lastTouchChannel;

    @Column(name = "recovery_token", length = 64, unique = true)
    private String recoveryToken;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

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

    public String getRecoveryToken() { return recoveryToken; }
    public void setRecoveryToken(String recoveryToken) { this.recoveryToken = recoveryToken; }
}
