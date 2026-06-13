package com.magadhexplora.api.alerts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/alerts")
public class AlertController {

    private final AlertRepository repo;
    private final AlertScanner scanner;

    public AlertController(AlertRepository repo, AlertScanner scanner) {
        this.repo = repo;
        this.scanner = scanner;
    }

    @GetMapping
    public Page<AlertEntity> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return repo.findAllByOrderByReadAscCreatedAtDesc(PageRequest.of(page, size));
    }

    @GetMapping("/unread-count")
    public Map<String, Object> unread() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("unread", repo.countByReadFalse());
        return out;
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<AlertEntity> markRead(@PathVariable Long id) {
        return repo.findById(id).map(a -> {
            a.setRead(true);
            return ResponseEntity.ok(repo.save(a));
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/read-all")
    public Map<String, Object> markAllRead() {
        int updated = 0;
        for (AlertEntity a : repo.findAll()) {
            if (!a.isRead()) { a.setRead(true); repo.save(a); updated++; }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("marked", updated);
        return out;
    }

    /** Manually run the scanner — useful for QA and after rule changes. */
    @PostMapping("/scan")
    public Map<String, Object> scan() {
        scanner.runOnce();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        return out;
    }
}
