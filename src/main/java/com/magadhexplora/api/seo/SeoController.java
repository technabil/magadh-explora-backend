package com.magadhexplora.api.seo;

import com.magadhexplora.api.blog.BlogRepository;
import com.magadhexplora.api.catalog.destination.DestinationRepository;
import com.magadhexplora.api.catalog.pkg.PackageRepository;
import com.magadhexplora.api.config.SiteProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@RestController
public class SeoController {

    private static final DateTimeFormatter ISO_DATE =
            DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

    private final SiteProperties site;
    private final PackageRepository packages;
    private final DestinationRepository destinations;
    private final BlogRepository blogs;

    public SeoController(SiteProperties site, PackageRepository packages,
                         DestinationRepository destinations, BlogRepository blogs) {
        this.site = site;
        this.packages = packages;
        this.destinations = destinations;
        this.blogs = blogs;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robots() {
        String base = site.publicUrlClean();
        String body = """
                User-agent: *
                Allow: /
                Disallow: /admin
                Disallow: /admin/

                Sitemap: %s/sitemap.xml
                """.formatted(base);
        return ResponseEntity.ok(body);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        String base = site.publicUrlClean();
        StringBuilder sb = new StringBuilder(2048);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Static pages
        appendUrl(sb, base + "/", null, "1.0", "weekly");
        appendUrl(sb, base + "/packages", null, "0.9", "weekly");
        appendUrl(sb, base + "/destinations", null, "0.9", "weekly");
        appendUrl(sb, base + "/blog", null, "0.8", "daily");
        appendUrl(sb, base + "/customize", null, "0.6", "monthly");
        appendUrl(sb, base + "/buddhist-tours", null, "0.8", "monthly");
        appendUrl(sb, base + "/jain-tours", null, "0.8", "monthly");
        appendUrl(sb, base + "/history-of-magadh", null, "0.7", "monthly");
        appendUrl(sb, base + "/contact", null, "0.5", "monthly");

        // Packages
        packages.findPublic(null, null, null, null, null, null)
                .forEach(p -> appendUrl(sb,
                        base + "/packages/" + p.getSlug(),
                        p.getUpdatedAt(),
                        "0.8",
                        "weekly"));

        // Destinations (active only)
        destinations.findByActiveOrderByNameAsc(true)
                .forEach(d -> appendUrl(sb,
                        base + "/destinations/" + d.getSlug(),
                        d.getUpdatedAt(),
                        "0.7",
                        "monthly"));

        // Blog posts (published only)
        blogs.findByPublishedTrueOrderByPublishedAtDesc()
                .forEach(b -> appendUrl(sb,
                        base + "/blog/" + b.getSlug(),
                        b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getPublishedAt(),
                        "0.7",
                        "monthly"));

        sb.append("</urlset>\n");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(sb.toString());
    }

    private static void appendUrl(StringBuilder sb, String loc, Instant lastmod,
                                  String priority, String changefreq) {
        sb.append("  <url>\n");
        sb.append("    <loc>").append(escape(loc)).append("</loc>\n");
        if (lastmod != null) {
            sb.append("    <lastmod>").append(ISO_DATE.format(lastmod)).append("</lastmod>\n");
        }
        sb.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        sb.append("    <priority>").append(priority).append("</priority>\n");
        sb.append("  </url>\n");
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
