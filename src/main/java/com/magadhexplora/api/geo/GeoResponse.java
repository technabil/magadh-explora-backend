package com.magadhexplora.api.geo;

public class GeoResponse {
    private String ip;
    private String countryCode;
    private String country;
    private String currency;
    private String suggestedLang;
    private String city;
    private String region;

    public GeoResponse() {}

    public GeoResponse(String ip, String countryCode, String country, String currency, String suggestedLang) {
        this.ip = ip;
        this.countryCode = countryCode;
        this.country = country;
        this.currency = currency;
        this.suggestedLang = suggestedLang;
    }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getSuggestedLang() { return suggestedLang; }
    public void setSuggestedLang(String suggestedLang) { this.suggestedLang = suggestedLang; }
}
