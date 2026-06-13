package com.magadhexplora.api.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrackingSessionRepository extends JpaRepository<TrackingSessionEntity, String> {
    Optional<TrackingSessionEntity> findBySessionId(String sessionId);

    /** All sessions for a set of visitor IDs. Returns [visitorId, utmSource, startedAt]. */
    @Query(value =
            "SELECT visitor_id, utm_source, started_at FROM tracking_sessions " +
            "WHERE visitor_id IN (:visitorIds) ORDER BY started_at ASC",
            nativeQuery = true)
    List<Object[]> sessionsForVisitors(@Param("visitorIds") Collection<String> visitorIds);
}
