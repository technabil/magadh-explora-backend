package com.magadhexplora.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {
    private String from = "no-reply@magadhexplora.local";
    private String adminTo = "admin@magadhexplora.local";
    private String brand = "Magadh Explora";

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getAdminTo() { return adminTo; }
    public void setAdminTo(String adminTo) { this.adminTo = adminTo; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
}
