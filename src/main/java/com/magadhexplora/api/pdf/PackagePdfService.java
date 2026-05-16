package com.magadhexplora.api.pdf;

import com.magadhexplora.api.catalog.pkg.PackageEntity;
import com.magadhexplora.api.catalog.pkg.PackageImageEntity;
import com.magadhexplora.api.catalog.category.CategoryEntity;
import com.magadhexplora.api.catalog.destination.DestinationEntity;
import com.magadhexplora.api.config.SiteProperties;
import com.magadhexplora.api.config.UploadProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders a styled package brochure PDF from a Thymeleaf template.
 * Image URLs that start with the configured upload prefix are resolved to local files for speed.
 */
@Service
public class PackagePdfService {

    private static final Logger log = LoggerFactory.getLogger(PackagePdfService.class);

    private final SpringTemplateEngine templateEngine;
    private final UploadProperties uploadProps;
    private final SiteProperties siteProps;

    public PackagePdfService(SpringTemplateEngine templateEngine,
                             UploadProperties uploadProps,
                             SiteProperties siteProps) {
        this.templateEngine = templateEngine;
        this.uploadProps = uploadProps;
        this.siteProps = siteProps;
    }

    /** Render a package to PDF bytes. Throws RuntimeException on failure. */
    public byte[] render(PackageEntity pkg) {
        Context ctx = new Context();
        ctx.setVariables(buildModel(pkg));

        String html = templateEngine.process("pdf/package-brochure", ctx);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024)) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html, siteProps.publicUrlClean() + "/");
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("PDF render failed for package " + pkg.getId(), ex);
        }
    }

    private Map<String, Object> buildModel(PackageEntity pkg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pkg", pkg);
        m.put("brand", "Magadh Explora");
        m.put("siteUrl", siteProps.publicUrlClean());

        m.put("heroUrl", absoluteOrBase64(pkg.getHeroImageUrl(), pickPrimary(pkg.getImages())));
        m.put("galleryUrls",
                pkg.getImages().stream()
                        .map(PackageImageEntity::getUrl)
                        .filter(u -> u != null && !u.isBlank())
                        .map(u -> absoluteOrBase64(u, null))
                        .limit(4)
                        .toList());

        m.put("itineraryLines", bulletize(pkg.getItinerary()));
        m.put("inclusionsLines", bulletize(pkg.getInclusions()));
        m.put("exclusionsLines", bulletize(pkg.getExclusions()));

        m.put("tierName",
                pkg.getCategories().stream()
                        .filter(c -> "TIER".equalsIgnoreCase(c.getKind()))
                        .map(CategoryEntity::getName).findFirst().orElse(null));
        m.put("themeNames",
                pkg.getCategories().stream()
                        .filter(c -> "THEME".equalsIgnoreCase(c.getKind()))
                        .map(CategoryEntity::getName).toList());

        m.put("destinationNames",
                pkg.getDestinations().stream().map(DestinationEntity::getName).toList());

        return m;
    }

    private String pickPrimary(List<PackageImageEntity> images) {
        return images.stream()
                .filter(PackageImageEntity::isPrimary)
                .map(PackageImageEntity::getUrl)
                .findFirst()
                .orElseGet(() -> images.isEmpty() ? null : images.get(0).getUrl());
    }

    /**
     * For local /uploads/* URLs, inline as base64 so Flying Saucer doesn't try to fetch over HTTP.
     * Otherwise return as-is (Flying Saucer's user agent will try to load it).
     */
    private String absoluteOrBase64(String url, String fallback) {
        String value = (url != null && !url.isBlank()) ? url : fallback;
        if (value == null || value.isBlank()) return null;

        if (value.startsWith(uploadProps.getUrlPrefix())) {
            String rel = value.substring(uploadProps.getUrlPrefix().length());
            if (rel.startsWith("/")) rel = rel.substring(1);
            Path file = Paths.get(uploadProps.getDir(), rel).toAbsolutePath().normalize();
            if (Files.exists(file)) {
                try {
                    byte[] bytes = Files.readAllBytes(file);
                    String mime = guessMime(file.toString());
                    return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
                } catch (Exception ex) {
                    log.warn("Could not inline upload {} into PDF: {}", file, ex.toString());
                }
            } else {
                log.warn("PDF upload file not found: {}", file);
            }
        }
        return value;
    }

    private static List<String> bulletize(String text) {
        if (text == null) return List.of();
        return java.util.Arrays.stream(text.split("\\r?\\n"))
                .map(s -> s.replaceFirst("^[-•*\\d.\\s]+", "").trim())
                .filter(s -> !s.isBlank())
                .toList();
    }

    private static String guessMime(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".gif")) return "image/gif";
        if (p.endsWith(".webp")) return "image/webp";
        if (p.endsWith(".svg")) return "image/svg+xml";
        return "image/jpeg";
    }
}
