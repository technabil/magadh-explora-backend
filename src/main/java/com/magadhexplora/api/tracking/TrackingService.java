package com.magadhexplora.api.tracking;

import com.magadhexplora.api.geo.GeoResponse;
import com.magadhexplora.api.geo.GeoService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);

    private final TrackingEventRepository events;
    private final TrackingSessionRepository sessions;
    private final GeoService geo;

    public TrackingService(TrackingEventRepository events,
                           TrackingSessionRepository sessions,
                           GeoService geo) {
        this.events = events;
        this.sessions = sessions;
        this.geo = geo;
    }

    @Transactional
    public void record(TrackingEventRequest req, HttpServletRequest http) {
        if (req == null || req.getVisitorId() == null || req.getVisitorId().isBlank()
                || req.getSessionId() == null || req.getSessionId().isBlank()) {
            return;
        }

        String eventType = req.getEventType() == null || req.getEventType().isBlank()
                ? "pageview"
                : req.getEventType();

        String countryCode = null;
        String country = null;
        String city = null;
        try {
            GeoResponse g = geo.lookup(http);
            if (g != null) {
                countryCode = g.getCountryCode();
                country = g.getCountry();
                city = g.getCity();
            }
        } catch (Exception ex) {
            log.debug("Tracking: geo lookup skipped: {}", ex.toString());
        }

        TrackingSessionEntity session = sessions.findBySessionId(req.getSessionId()).orElse(null);
        if (session == null) {
            session = new TrackingSessionEntity();
            session.setSessionId(req.getSessionId());
            session.setVisitorId(req.getVisitorId());
            session.setCountryCode(countryCode);
            session.setCountry(country);
            session.setCity(city);
            session.setDevice(trim(req.getDevice(), 20));
            session.setOs(trim(req.getOs(), 40));
            session.setBrowser(trim(req.getBrowser(), 40));
            session.setLanguage(trim(req.getLanguage(), 16));
            session.setReferrer(trim(req.getReferrer(), 500));
            session.setUtmSource(trim(req.getUtmSource(), 120));
            session.setUtmMedium(trim(req.getUtmMedium(), 120));
            session.setUtmCampaign(trim(req.getUtmCampaign(), 160));
            session.setUtmTerm(trim(req.getUtmTerm(), 160));
            session.setUtmContent(trim(req.getUtmContent(), 160));
            session.setLandingPath(trim(req.getPath(), 500));
        }
        session.setEventCount(session.getEventCount() + 1);
        sessions.save(session);

        TrackingEventEntity event = new TrackingEventEntity();
        event.setSessionId(req.getSessionId());
        event.setVisitorId(req.getVisitorId());
        event.setEventType(trim(eventType, 40));
        event.setPath(trim(req.getPath(), 500));
        event.setReferrer(trim(req.getReferrer(), 500));
        event.setCountryCode(countryCode);
        event.setCountry(country);
        event.setCity(city);
        event.setDevice(trim(req.getDevice(), 20));
        event.setUtmSource(session.getUtmSource());
        event.setUtmMedium(session.getUtmMedium());
        event.setUtmCampaign(session.getUtmCampaign());
        event.setProperties(req.getProperties());
        events.save(event);
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
