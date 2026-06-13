package com.magadhexplora.api.geo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeoService {

    private static final Logger log = LoggerFactory.getLogger(GeoService.class);
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final RestClient http = RestClient.builder()
            .baseUrl("http://ip-api.com")
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public GeoResponse lookup(HttpServletRequest request) {
        String ip = clientIp(request);
        if (isPrivateOrLoopback(ip)) {
            return defaultResponse(ip);
        }

        CacheEntry cached = cache.get(ip);
        if (cached != null && cached.isFresh()) {
            return cached.response;
        }

        try {
            String body = http.get()
                    .uri("/json/{ip}?fields=status,country,countryCode,regionName,city,query", ip)
                    .retrieve()
                    .body(String.class);
            JsonNode json = mapper.readTree(body);
            if (!"success".equals(json.path("status").asText())) {
                return defaultResponse(ip);
            }
            String code = json.path("countryCode").asText("IN");
            GeoResponse out = new GeoResponse(
                    ip,
                    code,
                    json.path("country").asText(""),
                    CountryMaps.currencyFor(code),
                    CountryMaps.languageFor(code)
            );
            out.setCity(json.path("city").asText(null));
            out.setRegion(json.path("regionName").asText(null));
            cache.put(ip, new CacheEntry(out, Instant.now()));
            return out;
        } catch (Exception ex) {
            log.warn("Geo lookup failed for {}: {}", ip, ex.toString());
            return defaultResponse(ip);
        }
    }

    private GeoResponse defaultResponse(String ip) {
        return new GeoResponse(ip, "IN", "India",
                CountryMaps.DEFAULT_CURRENCY, CountryMaps.DEFAULT_LANG);
    }

    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return req.getRemoteAddr();
    }

    private static boolean isPrivateOrLoopback(String ip) {
        if (ip == null || ip.isBlank()) return true;
        if (ip.equals("127.0.0.1") || ip.equals("::1")) return true;
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) return true;
        if (ip.startsWith("172.")) {
            try {
                int second = Integer.parseInt(ip.split("\\.")[1]);
                if (second >= 16 && second <= 31) return true;
            } catch (Exception ignored) { }
        }
        return false;
    }

    private record CacheEntry(GeoResponse response, Instant fetchedAt) {
        boolean isFresh() {
            return Duration.between(fetchedAt, Instant.now()).compareTo(CACHE_TTL) < 0;
        }
    }
}
