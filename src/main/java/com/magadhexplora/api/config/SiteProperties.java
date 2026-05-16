package com.magadhexplora.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.site")
public class SiteProperties {
    private String publicUrl = "http://localhost:5173";

    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }

    public String publicUrlClean() {
        if (publicUrl == null) return "";
        return publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }
}
