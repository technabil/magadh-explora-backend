package com.magadhexplora.api.lead.quote;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class QuoteDto {

    private Long id;

    @NotBlank @Size(max = 120)
    private String name;

    @NotBlank @Email @Size(max = 180)
    private String email;

    @JsonAlias({"phone"})
    @Size(max = 32)
    private String mobile;

    @Size(max = 80)
    private String country;

    @Size(max = 40)
    private String travelerType;

    @Size(max = 40)
    private String packageTier;

    private String destinations;

    @Size(max = 40)
    private String budget;

    private Long packageId;

    @JsonAlias({"groupSize"})
    private Integer numTravelers;

    @JsonAlias({"travelDates", "travelDate"})
    private LocalDate travelDate;

    @JsonAlias({"requirements"})
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

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getTravelerType() { return travelerType; }
    public void setTravelerType(String travelerType) { this.travelerType = travelerType; }

    public String getPackageTier() { return packageTier; }
    public void setPackageTier(String packageTier) { this.packageTier = packageTier; }

    public String getDestinations() { return destinations; }

    public void setDestinations(Object value) {
        if (value == null) { this.destinations = null; return; }
        if (value instanceof List<?> list) {
            this.destinations = String.join(", ",
                    list.stream().filter(java.util.Objects::nonNull).map(Object::toString).toList());
        } else {
            this.destinations = value.toString();
        }
    }

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

    public static QuoteDto from(QuoteEntity e) {
        QuoteDto d = new QuoteDto();
        d.id = e.getId();
        d.name = e.getName();
        d.email = e.getEmail();
        d.mobile = e.getMobile();
        d.country = e.getCountry();
        d.travelerType = e.getTravelerType();
        d.packageTier = e.getPackageTier();
        d.destinations = e.getDestinations();
        d.budget = e.getBudget();
        d.packageId = e.getPackageId();
        d.numTravelers = e.getNumTravelers();
        d.travelDate = e.getTravelDate();
        d.message = e.getMessage();
        d.status = e.getStatus();
        d.createdAt = e.getCreatedAt();
        return d;
    }

    public QuoteEntity toEntity() {
        QuoteEntity e = new QuoteEntity();
        e.setName(name.trim());
        e.setEmail(email.trim().toLowerCase());
        e.setMobile(mobile);
        e.setCountry(country);
        e.setTravelerType(travelerType);
        e.setPackageTier(packageTier);
        e.setDestinations(destinations);
        e.setBudget(budget);
        e.setPackageId(packageId);
        e.setNumTravelers(numTravelers);
        e.setTravelDate(travelDate);
        e.setMessage(message);
        return e;
    }
}
