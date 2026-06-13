package com.magadhexplora.api.analytics;

import com.magadhexplora.api.lead.booking.BookingEntity;
import com.magadhexplora.api.lead.contact.ContactEntity;
import com.magadhexplora.api.lead.quote.QuoteEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Heuristic lead quality scoring (0–100). The fields used live on the lead
 * itself — country, budget, mobile, package value, group size — so we can
 * score without needing to join visitor-event history (which lacks email
 * attribution today).
 */
@Service
public class LeadScoringService {

    /** ISO codes — premium foreign markets per the requirements doc. */
    private static final Set<String> PREMIUM_FOREIGN = Set.of(
            "lk", "th", "jp", "mm", "vn", "kr",       // Buddhist circuit
            "us", "gb", "au", "ca", "de", "fr", "sg", "ae"
    );

    public enum Tier { HOT, WARM, COLD }

    public record Score(int value, Tier tier, String reason) {}

    public Score scoreQuote(QuoteEntity q) {
        if (q == null) return new Score(0, Tier.COLD, "no data");
        int s = 20;
        StringBuilder why = new StringBuilder();
        String cc = countryCode(q.getCountry());
        if (cc != null && !"in".equals(cc)) {
            if (PREMIUM_FOREIGN.contains(cc)) { s += 30; why.append("premium country; "); }
            else                              { s += 20; why.append("foreign country; "); }
        }
        String budget = lower(q.getBudget());
        if (budget != null) {
            if (budget.contains("luxury") || budget.contains("premium")
                    || budget.contains("100k") || budget.contains("100000")
                    || budget.contains("1l") || budget.contains("1 lakh")) {
                s += 25; why.append("premium budget; ");
            } else if (budget.contains("50k") || budget.contains("50000") || budget.contains("high")) {
                s += 15; why.append("high budget; ");
            }
        }
        String tier = lower(q.getPackageTier());
        if (tier != null && (tier.contains("luxury") || tier.contains("premium"))) {
            s += 15; why.append("premium tier; ");
        }
        if (q.getNumTravelers() != null && q.getNumTravelers() >= 4) { s += 10; why.append("group of 4+; "); }
        if (q.getTravelDate() != null)        { s += 5;  why.append("date set; "); }
        if (notBlank(q.getMobile()))          { s += 5;  why.append("mobile given; "); }
        if (q.getMessage() != null && q.getMessage().length() > 100) { s += 5; why.append("detailed message; "); }
        return finalize(s, why.toString());
    }

    public Score scoreContact(ContactEntity c) {
        if (c == null) return new Score(0, Tier.COLD, "no data");
        int s = 30;
        StringBuilder why = new StringBuilder();
        if (notBlank(c.getMobile())) { s += 10; why.append("mobile given; "); }
        if (c.getMessage() != null && c.getMessage().length() > 200) { s += 10; why.append("detailed message; "); }
        String subject = lower(c.getSubject());
        if (subject != null && (subject.contains("booking") || subject.contains("quote") || subject.contains("price"))) {
            s += 15; why.append("commercial intent in subject; ");
        }
        return finalize(s, why.toString());
    }

    public Score scoreBooking(BookingEntity b) {
        if (b == null) return new Score(0, Tier.COLD, "no data");
        if ("PAID".equals(b.getPaymentStatus())) return new Score(100, Tier.HOT, "paid booking");

        int s = 50;
        StringBuilder why = new StringBuilder();
        BigDecimal amount = b.getTotalAmountInr();
        if (amount != null) {
            if (amount.compareTo(BigDecimal.valueOf(100_000)) >= 0) {
                s += 35; why.append("amount >= ₹1L; ");
            } else if (amount.compareTo(BigDecimal.valueOf(50_000)) >= 0) {
                s += 25; why.append("amount >= ₹50K; ");
            } else if (amount.compareTo(BigDecimal.valueOf(20_000)) >= 0) {
                s += 10; why.append("amount >= ₹20K; ");
            }
        }
        if (b.getNumTravelers() >= 4) { s += 10; why.append("group of 4+; "); }
        if ("CONFIRMED".equals(b.getStatus()) || "IN_PROGRESS".equals(b.getStatus())) {
            s += 5; why.append("confirmed; ");
        }
        if (notBlank(b.getMobile())) { s += 5; why.append("mobile given; "); }
        return finalize(s, why.toString());
    }

    private static Score finalize(int s, String why) {
        int clamped = Math.max(0, Math.min(100, s));
        Tier t = clamped >= 70 ? Tier.HOT : clamped >= 40 ? Tier.WARM : Tier.COLD;
        String reason = why.isBlank() ? "baseline" : why.trim();
        return new Score(clamped, t, reason);
    }

    private static String countryCode(String s) {
        if (s == null) return null;
        String c = s.trim().toLowerCase();
        if (c.isEmpty()) return null;
        // Already an ISO code?
        if (c.length() == 2) return c;
        // Common name → code
        return switch (c) {
            case "india" -> "in";
            case "sri lanka" -> "lk";
            case "thailand" -> "th";
            case "japan" -> "jp";
            case "myanmar", "burma" -> "mm";
            case "vietnam" -> "vn";
            case "korea", "south korea" -> "kr";
            case "united states", "usa", "us" -> "us";
            case "united kingdom", "uk" -> "gb";
            case "australia" -> "au";
            case "canada" -> "ca";
            case "germany" -> "de";
            case "france" -> "fr";
            case "singapore" -> "sg";
            case "uae", "united arab emirates" -> "ae";
            default -> null;
        };
    }

    private static String lower(String s) { return s == null ? null : s.toLowerCase(); }
    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
