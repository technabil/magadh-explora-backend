package com.magadhexplora.api.analytics;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple linear-trend forecaster. Given a daily series, fits y = m·x + b by
 * least squares, projects N days forward, and surfaces a residual-std-dev
 * confidence band. Honest about insufficient data — refuses to forecast when
 * fewer than 14 history points are available.
 */
@Service
public class ForecastService {

    private static final int MIN_HISTORY = 14;
    private static final double Z_95 = 1.96;

    public record Point(LocalDate date, double value) {}

    public record ForecastPoint(LocalDate date, Double actual, Double projected, Double lower, Double upper) {}

    public record ForecastResult(
            List<ForecastPoint> series,
            int historyDays,
            int horizonDays,
            double slopePerDay,
            double r2,
            boolean sufficient,
            String note
    ) {}

    public ForecastResult forecast(List<Point> history, int horizonDays) {
        int n = history.size();
        List<ForecastPoint> out = new ArrayList<>();
        if (n < MIN_HISTORY) {
            for (Point p : history) {
                out.add(new ForecastPoint(p.date(), p.value(), null, null, null));
            }
            return new ForecastResult(out, n, horizonDays, 0, 0, false,
                    "Need at least " + MIN_HISTORY + " days of history to forecast (have " + n + ").");
        }

        // Day index 0..n-1
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = history.get(i).value();
            sumX += x; sumY += y; sumXY += x * y; sumXX += x * x;
        }
        double denom = n * sumXX - sumX * sumX;
        double slope = denom == 0 ? 0 : (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;

        // Residual std dev + R² (against mean baseline)
        double meanY = sumY / n;
        double ssRes = 0, ssTot = 0;
        for (int i = 0; i < n; i++) {
            double pred = slope * i + intercept;
            double res  = history.get(i).value() - pred;
            ssRes += res * res;
            ssTot += (history.get(i).value() - meanY) * (history.get(i).value() - meanY);
        }
        double sigma = Math.sqrt(ssRes / Math.max(1, n - 2));
        double r2    = ssTot == 0 ? 1 : 1 - (ssRes / ssTot);
        double band  = Z_95 * sigma;

        for (int i = 0; i < n; i++) {
            out.add(new ForecastPoint(history.get(i).date(), history.get(i).value(), null, null, null));
        }
        LocalDate lastDay = history.get(n - 1).date();
        for (int j = 1; j <= horizonDays; j++) {
            double pred = slope * (n - 1 + j) + intercept;
            double lower = Math.max(0, pred - band);
            double upper = Math.max(0, pred + band);
            double projected = Math.max(0, pred);
            out.add(new ForecastPoint(lastDay.plusDays(j), null, projected, lower, upper));
        }

        return new ForecastResult(out, n, horizonDays, slope, r2, true, null);
    }

    /** Convert a ForecastResult to a JSON-friendly payload. */
    public Map<String, Object> toJson(ForecastResult r) {
        List<Map<String, Object>> series = new ArrayList<>();
        for (ForecastPoint p : r.series()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", p.date().toString());
            row.put("actual", p.actual());
            row.put("projected", p.projected());
            row.put("lower", p.lower());
            row.put("upper", p.upper());
            series.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sufficient", r.sufficient());
        out.put("historyDays", r.historyDays());
        out.put("horizonDays", r.horizonDays());
        out.put("slopePerDay", BigDecimal.valueOf(r.slopePerDay()).setScale(3, RoundingMode.HALF_UP));
        out.put("r2", BigDecimal.valueOf(Math.max(0, r.r2())).setScale(3, RoundingMode.HALF_UP));
        out.put("note", r.note());
        out.put("series", series);

        // Convenience: projected total over the horizon window
        double horizonTotal = r.series().stream()
                .filter(p -> p.projected() != null)
                .mapToDouble(ForecastPoint::projected).sum();
        out.put("horizonTotal", BigDecimal.valueOf(horizonTotal).setScale(2, RoundingMode.HALF_UP));
        return out;
    }
}
