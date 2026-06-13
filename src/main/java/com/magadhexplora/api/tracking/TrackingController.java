package com.magadhexplora.api.tracking;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/track")
public class TrackingController {

    private final TrackingService tracking;

    public TrackingController(TrackingService tracking) {
        this.tracking = tracking;
    }

    /**
     * Anonymous event ingest. Accepts a fire-and-forget POST from the
     * frontend tracker once cookie consent has been granted.
     */
    @PostMapping("/event")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void event(@RequestBody TrackingEventRequest req, HttpServletRequest http) {
        tracking.record(req, http);
    }
}
