package com.magadhexplora.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple in-memory token-bucket rate limiter for public POST endpoints.
 * 10 requests / minute / client IP. Returns 429 with a JSON body when exceeded.
 *
 * Suitable for a single-instance deployment. For multi-instance, replace with Redis-backed buckets.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int CAPACITY = 10;
    private static final long REFILL_INTERVAL_MS = 60_000L;

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/contact",
            "/api/quote",
            "/api/bookings",
            "/api/bookings/lookup",
            "/api/reviews",
            "/api/leads/abandoned"
    );

    private final ConcurrentHashMap<String, AtomicReference<Bucket>> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper json = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(req.getMethod()) || !LIMITED_PATHS.contains(req.getRequestURI())) {
            chain.doFilter(req, res);
            return;
        }

        String key = clientIp(req);
        Bucket b = take(key);

        res.setHeader("X-RateLimit-Limit", String.valueOf(CAPACITY));
        res.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, b.tokens)));

        if (b.tokens < 0) {
            long retryAfter = Math.max(1, (b.resetAt - System.currentTimeMillis()) / 1000);
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setHeader("Retry-After", String.valueOf(retryAfter));
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            json.writeValue(res.getWriter(), Map.of(
                    "status", 429,
                    "error", "Too Many Requests",
                    "message", "Rate limit exceeded. Try again in " + retryAfter + "s."
            ));
            log.warn("Rate limit hit: ip={} path={}", key, req.getRequestURI());
            return;
        }

        chain.doFilter(req, res);
    }

    private Bucket take(String key) {
        long now = System.currentTimeMillis();
        AtomicReference<Bucket> ref = buckets.computeIfAbsent(key,
                k -> new AtomicReference<>(new Bucket(CAPACITY - 1, now + REFILL_INTERVAL_MS)));
        return ref.updateAndGet(prev -> {
            if (now >= prev.resetAt) {
                return new Bucket(CAPACITY - 1, now + REFILL_INTERVAL_MS);
            }
            return new Bucket(prev.tokens - 1, prev.resetAt);
        });
    }

    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return req.getRemoteAddr();
    }

    private record Bucket(int tokens, long resetAt) {}
}
