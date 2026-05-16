package com.magadhexplora.api.lead.booking;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class BookingDto {

    private Long id;
    private Long userId;

    @NotNull
    private Long packageId;

    @NotBlank @Size(max = 120)
    private String name;

    @NotBlank @Email @Size(max = 180)
    private String email;

    @JsonAlias({"phone"})
    @Size(max = 32)
    private String mobile;

    @Min(1)
    private int numTravelers = 1;

    private LocalDate travelDate;

    private BigDecimal totalAmountInr;
    private String currency;
    private BigDecimal totalAmountLocal;

    private String status;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public int getNumTravelers() { return numTravelers; }
    public void setNumTravelers(int numTravelers) { this.numTravelers = numTravelers; }

    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }

    public BigDecimal getTotalAmountInr() { return totalAmountInr; }
    public void setTotalAmountInr(BigDecimal totalAmountInr) { this.totalAmountInr = totalAmountInr; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getTotalAmountLocal() { return totalAmountLocal; }
    public void setTotalAmountLocal(BigDecimal totalAmountLocal) { this.totalAmountLocal = totalAmountLocal; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static BookingDto from(BookingEntity e) {
        BookingDto d = new BookingDto();
        d.id = e.getId();
        d.userId = e.getUserId();
        d.packageId = e.getPackageId();
        d.name = e.getName();
        d.email = e.getEmail();
        d.mobile = e.getMobile();
        d.numTravelers = e.getNumTravelers();
        d.travelDate = e.getTravelDate();
        d.totalAmountInr = e.getTotalAmountInr();
        d.currency = e.getCurrency();
        d.totalAmountLocal = e.getTotalAmountLocal();
        d.status = e.getStatus();
        d.createdAt = e.getCreatedAt();
        return d;
    }

    public BookingEntity toEntity() {
        BookingEntity e = new BookingEntity();
        e.setPackageId(packageId);
        e.setName(name.trim());
        e.setEmail(email.trim().toLowerCase());
        e.setMobile(mobile);
        e.setNumTravelers(numTravelers);
        e.setTravelDate(travelDate);
        if (totalAmountInr != null) e.setTotalAmountInr(totalAmountInr);
        if (currency != null) e.setCurrency(currency);
        if (totalAmountLocal != null) e.setTotalAmountLocal(totalAmountLocal);
        return e;
    }
}
