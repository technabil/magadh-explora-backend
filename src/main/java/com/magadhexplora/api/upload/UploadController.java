package com.magadhexplora.api.upload;

import com.magadhexplora.api.config.UploadProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
public class UploadController {

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final UploadProperties props;

    public UploadController(UploadProperties props) {
        this.props = props;
    }

    @PostMapping("/api/admin/uploads")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
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
        String relPath = String.format("%04d/%02d/%s%s",
                today.getYear(), today.getMonthValue(), UUID.randomUUID(), ext);

        Path target = Paths.get(props.getDir(), relPath).toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        file.transferTo(target.toFile());

        String urlPrefix = props.getUrlPrefix();
        if (!urlPrefix.endsWith("/")) urlPrefix = urlPrefix + "/";
        return Map.of("url", urlPrefix + relPath);
    }

    private static String extensionFor(String contentType, String original) {
        if (original != null) {
            int dot = original.lastIndexOf('.');
            if (dot >= 0 && dot < original.length() - 1) {
                String e = original.substring(dot).toLowerCase();
                if (e.matches("\\.(jpe?g|png|gif|webp|svg|avif)")) return e;
            }
        }
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/gif"  -> ".gif";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            case "image/avif" -> ".avif";
            default -> ".bin";
        };
    }
}
