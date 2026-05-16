package com.magadhexplora.api.geo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GeoController {

    private final GeoService geoService;

    public GeoController(GeoService geoService) {
        this.geoService = geoService;
    }

    @GetMapping("/api/geo")
    public GeoResponse geo(HttpServletRequest req) {
        return geoService.lookup(req);
    }
}
