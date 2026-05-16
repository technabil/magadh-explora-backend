package com.magadhexplora.api.lead.quote;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "quotes")
public class QuoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(length = 32)
    private String mobile;

    @Column(length = 80)
    private String country;

    @Column(name = "traveler_type", length = 40)
    private String travelerType;

    @Column(name = "package_tier", length = 40)
    private String packageTier;

    @Column(length = 500)
    private String destinations;

    @Column(length = 40)
    private String budget;

    @Column(name = "package_id")
    private Long packageId;

    @Column(name = "num_travelers")
    private Integer numTravelers;

    @Column(name = "travel_date")
    private LocalDate travelDate;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 32)
    private String status = "NEW";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getTravelerType() { return travelerType; }
    public void setTravelerType(String travelerType) { this.travelerType = travelerType; }

    public String getPackageTier() { return packageTier; }
    public void setPackageTier(String packageTier) { this.packageTier = packageTier; }

    public String getDestinations() { return destinations; }
    public void setDestinations(String destinations) { this.destinations = destinations; }

    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public Integer getNumTravelers() { return numTravelers; }
    public void setNumTravelers(Integer numTravelers) { this.numTravelers = numTravelers; }

    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
