package com.magadhexplora.api.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TrackingEventRepository extends JpaRepository<TrackingEventEntity, Long> {

    @Query("SELECT COUNT(DISTINCT e.visitorId) FROM TrackingEventEntity e " +
            "WHERE e.createdAt >= :from AND e.createdAt < :to")
    long countUniqueVisitors(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(DISTINCT e.visitorId) FROM TrackingEventEntity e " +
            "WHERE e.createdAt >= :since")
    long countLiveVisitors(@Param("since") Instant since);

    /**
     * Top traffic sources today. Returns rows of [source, visitors]; falls back to
     * the literal "direct" bucket for sessions with no UTM source.
     */
    @Query("SELECT COALESCE(NULLIF(e.utmSource, ''), 'direct') AS src, " +
            "COUNT(DISTINCT e.visitorId) AS visitors " +
            "FROM TrackingEventEntity e " +
            "WHERE e.createdAt >= :from AND e.createdAt < :to " +
            "GROUP BY src ORDER BY visitors DESC")
    List<Object[]> topSources(@Param("from") Instant from, @Param("to") Instant to);

    /** Top countries in a range. Returns [countryCode, country, visitors]. */
    @Query("SELECT COALESCE(NULLIF(e.countryCode, ''), 'XX') AS cc, " +
            "COALESCE(NULLIF(e.country, ''), 'Unknown') AS cn, " +
            "COUNT(DISTINCT e.visitorId) AS visitors " +
            "FROM TrackingEventEntity e " +
            "WHERE e.createdAt >= :from AND e.createdAt < :to " +
            "GROUP BY cc, cn ORDER BY visitors DESC")
    List<Object[]> topCountries(@Param("from") Instant from, @Param("to") Instant to);

    /** Daily unique visitor counts for a window. Returns [yyyy-mm-dd, visitors]. */
    @Query(value =
            "SELECT DATE(created_at) AS d, COUNT(DISTINCT visitor_id) AS visitors " +
            "FROM tracking_events " +
            "WHERE created_at >= :from AND created_at < :to " +
            "GROUP BY DATE(created_at) ORDER BY d ASC",
            nativeQuery = true)
    List<Object[]> dailyVisitorCounts(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * Per-stage distinct-visitor counts with optional filters. Returns one row of:
     * [visits, packageViews, formStarts, formSubmits, whatsappClicks].
     */
    @Query(value =
            "SELECT " +
            "  COUNT(DISTINCT CASE WHEN event_type='pageview' THEN visitor_id END), " +
            "  COUNT(DISTINCT CASE WHEN event_type='package_view' THEN visitor_id END), " +
            "  COUNT(DISTINCT CASE WHEN event_type='form_start' THEN visitor_id END), " +
            "  COUNT(DISTINCT CASE WHEN event_type='form_submit' THEN visitor_id END), " +
            "  COUNT(DISTINCT CASE WHEN event_type='whatsapp_click' THEN visitor_id END) " +
            "FROM tracking_events " +
            "WHERE created_at >= :from AND created_at < :to " +
            "  AND (:country IS NULL OR country_code = :country) " +
            "  AND (:source IS NULL OR utm_source = :source) " +
            "  AND (:device IS NULL OR device = :device)",
            nativeQuery = true)
    Object[] funnelCounts(@Param("from") Instant from,
                          @Param("to") Instant to,
                          @Param("country") String country,
                          @Param("source") String source,
                          @Param("device") String device);

    /**
     * Top countries with both unique-visitor count and form-submit count for
     * computing per-country conversion. Returns [code, name, visitors, submits].
     */
    @Query(value =
            "SELECT " +
            "  COALESCE(NULLIF(country_code, ''), 'XX') AS cc, " +
            "  COALESCE(NULLIF(country, ''), 'Unknown') AS cn, " +
            "  COUNT(DISTINCT visitor_id) AS visitors, " +
            "  COUNT(DISTINCT CASE WHEN event_type='form_submit' THEN visitor_id END) AS submits " +
            "FROM tracking_events " +
            "WHERE created_at >= :from AND created_at < :to " +
            "GROUP BY cc, cn ORDER BY visitors DESC",
            nativeQuery = true)
    List<Object[]> countriesWithConversion(@Param("from") Instant from, @Param("to") Instant to);

    /** Top cities for a country. Returns [city, visitors, submits]. */
    @Query(value =
            "SELECT " +
            "  COALESCE(NULLIF(city, ''), 'Unknown') AS cy, " +
            "  COUNT(DISTINCT visitor_id) AS visitors, " +
            "  COUNT(DISTINCT CASE WHEN event_type='form_submit' THEN visitor_id END) AS submits " +
            "FROM tracking_events " +
            "WHERE created_at >= :from AND created_at < :to " +
            "  AND country_code = :country " +
            "GROUP BY cy ORDER BY visitors DESC",
            nativeQuery = true)
    List<Object[]> citiesByCountry(@Param("from") Instant from,
                                   @Param("to") Instant to,
                                   @Param("country") String country);

    /** Top viewed packages. Returns [packageId, views, uniqueVisitors]. */
    @Query(value =
            "SELECT " +
            "  CAST(JSON_UNQUOTE(JSON_EXTRACT(properties, '$.packageId')) AS UNSIGNED) AS pid, " +
            "  COUNT(*) AS views, " +
            "  COUNT(DISTINCT visitor_id) AS uniq " +
            "FROM tracking_events " +
            "WHERE event_type = 'package_view' " +
            "  AND created_at >= :from AND created_at < :to " +
            "  AND JSON_EXTRACT(properties, '$.packageId') IS NOT NULL " +
            "GROUP BY pid " +
            "ORDER BY views DESC",
            nativeQuery = true)
    List<Object[]> packageViews(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * Per-package view-to-enquiry conversion: distinct visitors who viewed the
     * package vs distinct visitors who form-submitted in the same range.
     * Returns [packageId, viewers, submitters].
     */
    @Query(value =
            "SELECT " +
            "  CAST(JSON_UNQUOTE(JSON_EXTRACT(pv.properties, '$.packageId')) AS UNSIGNED) AS pid, " +
            "  COUNT(DISTINCT pv.visitor_id) AS viewers, " +
            "  COUNT(DISTINCT fs.visitor_id) AS submitters " +
            "FROM tracking_events pv " +
            "LEFT JOIN tracking_events fs " +
            "  ON fs.visitor_id = pv.visitor_id " +
            "  AND fs.event_type = 'form_submit' " +
            "  AND fs.created_at >= :from AND fs.created_at < :to " +
            "WHERE pv.event_type = 'package_view' " +
            "  AND pv.created_at >= :from AND pv.created_at < :to " +
            "  AND JSON_EXTRACT(pv.properties, '$.packageId') IS NOT NULL " +
            "GROUP BY pid " +
            "ORDER BY viewers DESC",
            nativeQuery = true)
    List<Object[]> packageConversion(@Param("from") Instant from, @Param("to") Instant to);

    /** Top country per package by viewer count. Returns [packageId, countryCode, country, viewers]. */
    @Query(value =
            "SELECT pid, country_code, country, viewers FROM ( " +
            "  SELECT " +
            "    CAST(JSON_UNQUOTE(JSON_EXTRACT(properties, '$.packageId')) AS UNSIGNED) AS pid, " +
            "    COALESCE(NULLIF(country_code, ''), 'XX') AS country_code, " +
            "    COALESCE(NULLIF(country, ''), 'Unknown') AS country, " +
            "    COUNT(DISTINCT visitor_id) AS viewers, " +
            "    ROW_NUMBER() OVER (PARTITION BY JSON_EXTRACT(properties, '$.packageId') ORDER BY COUNT(DISTINCT visitor_id) DESC) AS rn " +
            "  FROM tracking_events " +
            "  WHERE event_type = 'package_view' " +
            "    AND created_at >= :from AND created_at < :to " +
            "    AND JSON_EXTRACT(properties, '$.packageId') IS NOT NULL " +
            "  GROUP BY pid, country_code, country " +
            ") ranked WHERE rn = 1",
            nativeQuery = true)
    List<Object[]> packageTopCountry(@Param("from") Instant from, @Param("to") Instant to);

    /** Top pageview paths. Returns [path, views, uniqueVisitors]. */
    @Query(value =
            "SELECT path, COUNT(*) AS views, COUNT(DISTINCT visitor_id) AS uniq " +
            "FROM tracking_events " +
            "WHERE event_type = 'pageview' " +
            "  AND created_at >= :from AND created_at < :to " +
            "  AND path IS NOT NULL " +
            "GROUP BY path " +
            "ORDER BY views DESC",
            nativeQuery = true)
    List<Object[]> topPaths(@Param("from") Instant from, @Param("to") Instant to);

    /** Top landing paths (entry pages). Returns [path, sessions]. */
    @Query(value =
            "SELECT landing_path, COUNT(*) AS n " +
            "FROM tracking_sessions " +
            "WHERE started_at >= :from AND started_at < :to " +
            "  AND landing_path IS NOT NULL AND landing_path <> '' " +
            "GROUP BY landing_path " +
            "ORDER BY n DESC",
            nativeQuery = true)
    List<Object[]> topLandingPaths(@Param("from") Instant from, @Param("to") Instant to);

    /** WhatsApp clicks total in range. */
    @Query("SELECT COUNT(e) FROM TrackingEventEntity e " +
            "WHERE e.eventType = 'whatsapp_click' " +
            "  AND e.createdAt >= :from AND e.createdAt < :to")
    long whatsappClicksTotal(@Param("from") Instant from, @Param("to") Instant to);

    /** WhatsApp clicks grouped by country in range. Returns [countryCode, country, clicks]. */
    @Query(value =
            "SELECT COALESCE(NULLIF(country_code, ''), 'XX') AS cc, " +
            "       COALESCE(NULLIF(country, ''), 'Unknown') AS cn, " +
            "       COUNT(*) AS n " +
            "FROM tracking_events " +
            "WHERE event_type = 'whatsapp_click' " +
            "  AND created_at >= :from AND created_at < :to " +
            "GROUP BY cc, cn ORDER BY n DESC",
            nativeQuery = true)
    List<Object[]> whatsappClicksByCountry(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * email -> visitorId mapping from form_submit events that stamped their email.
     * Returns [email, visitorId]. Used to link bookings back to tracking sessions.
     */
    @Query(value =
            "SELECT DISTINCT " +
            "  JSON_UNQUOTE(JSON_EXTRACT(properties, '$.email')) AS email, " +
            "  visitor_id " +
            "FROM tracking_events " +
            "WHERE event_type = 'form_submit' " +
            "  AND JSON_EXTRACT(properties, '$.email') IS NOT NULL",
            nativeQuery = true)
    List<Object[]> emailToVisitor();

    /**
     * Per-form abandonment: distinct visitors who started a form vs distinct
     * visitors who submitted, grouped by the `form` property.
     * Returns [form, started, submitted].
     */
    @Query(value =
            "SELECT " +
            "  JSON_UNQUOTE(JSON_EXTRACT(properties, '$.form')) AS form, " +
            "  COUNT(DISTINCT CASE WHEN event_type='form_start' THEN visitor_id END) AS started, " +
            "  COUNT(DISTINCT CASE WHEN event_type='form_submit' THEN visitor_id END) AS submitted " +
            "FROM tracking_events " +
            "WHERE event_type IN ('form_start','form_submit') " +
            "  AND created_at >= :from AND created_at < :to " +
            "  AND JSON_EXTRACT(properties, '$.form') IS NOT NULL " +
            "GROUP BY form " +
            "ORDER BY started DESC",
            nativeQuery = true)
    List<Object[]> formAbandonment(@Param("from") Instant from, @Param("to") Instant to);
}
