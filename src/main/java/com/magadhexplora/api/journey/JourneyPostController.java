package com.magadhexplora.api.journey;

import com.magadhexplora.api.config.UploadProperties;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class JourneyPostController {

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final JourneyPostRepository repo;
    private final UploadProperties uploadProps;

    public JourneyPostController(JourneyPostRepository repo, UploadProperties uploadProps) {
        this.repo = repo;
        this.uploadProps = uploadProps;
    }

    // ---- public ----

    /** Public image upload for journey submissions. Returns a relative URL. */
    @PostMapping("/api/journey-posts/media")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> uploadMedia(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only image uploads allowed");
        }

        String ext = extensionFor(contentType, file.getOriginalFilename());
        LocalDate today = LocalDate.now();
        String relPath = String.format("journey/%04d/%02d/%s%s",
                today.getYear(), today.getMonthValue(), UUID.randomUUID(), ext);

        Path target = Paths.get(uploadProps.getDir(), relPath).toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        file.transferTo(target.toFile());

        String urlPrefix = uploadProps.getUrlPrefix();
        if (!urlPrefix.endsWith("/")) urlPrefix = urlPrefix + "/";
        return Map.of("url", urlPrefix + relPath);
    }

    /** Public submit — always starts unapproved, awaiting moderation. */
    @PostMapping("/api/journey-posts")
    @ResponseStatus(HttpStatus.CREATED)
    public JourneyPostDto submit(@Valid @RequestBody JourneyPostDto req) {
        JourneyPostEntity e = req.toEntity();
        e.setApproved(false);
        e.setLikes(0);
        return JourneyPostDto.from(repo.save(e));
    }

    /** Public list of approved posts, optionally filtered by media type. */
    @GetMapping("/api/journey-posts")
    public List<JourneyPostDto> listPublic(@RequestParam(required = false) String type) {
        List<JourneyPostEntity> rows = (type == null || type.isBlank())
                ? repo.findByApprovedTrueOrderByCreatedAtDesc()
                : repo.findByApprovedTrueAndMediaTypeOrderByCreatedAtDesc(JourneyPostDto.normalizeType(type));
        return rows.stream().map(JourneyPostDto::from).toList();
    }

    // ---- admin ----

    @GetMapping("/api/admin/journey-posts")
    public Page<JourneyPostDto> adminList(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) Boolean approved) {
        var pageable = PageRequest.of(page, size);
        Page<JourneyPostEntity> result = (approved == null)
                ? repo.findAllByOrderByCreatedAtDesc(pageable)
                : repo.findByApprovedOrderByCreatedAtDesc(approved, pageable);
        return result.map(JourneyPostDto::from);
    }

    @GetMapping("/api/admin/journey-posts/pending-count")
    public Map<String, Object> pendingCount() {
        return Map.of("pending", repo.countByApprovedFalse());
    }

    @PatchMapping("/api/admin/journey-posts/{id}/approve")
    public JourneyPostDto approve(@PathVariable Long id,
                                  @RequestParam(defaultValue = "true") boolean approved) {
        JourneyPostEntity e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        e.setApproved(approved);
        return JourneyPostDto.from(repo.save(e));
    }

    @DeleteMapping("/api/admin/journey-posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
        repo.deleteById(id);
    }

    private static String extensionFor(String contentType, String original) {
        if (original != null) {
            int dot = original.lastIndexOf('.');
            if (dot >= 0 && dot < original.length() - 1) {
                String e = original.substring(dot).toLowerCase();
                if (e.matches("\\.(jpe?g|png|gif|webp|avif)")) return e;
            }
        }
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/gif"  -> ".gif";
            case "image/webp" -> ".webp";
            case "image/avif" -> ".avif";
            default -> ".bin";
        };
    }
}
