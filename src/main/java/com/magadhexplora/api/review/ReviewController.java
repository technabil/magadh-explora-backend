package com.magadhexplora.api.review;

import com.magadhexplora.api.catalog.pkg.PackageEntity;
import com.magadhexplora.api.catalog.pkg.PackageRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@RestController
public class ReviewController {

    private final ReviewRepository reviews;
    private final PackageRepository packages;

    public ReviewController(ReviewRepository reviews, PackageRepository packages) {
        this.reviews = reviews;
        this.packages = packages;
    }

    @PostMapping("/api/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto submit(@Valid @RequestBody ReviewDto req) {
        if (!packages.existsById(req.getPackageId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown package");
        }
        ReviewEntity e = req.toEntity();
        e.setApproved(false);
        return ReviewDto.from(reviews.save(e));
    }

    @GetMapping("/api/packages/{slug}/reviews")
    public List<ReviewDto> listForPackage(@PathVariable String slug) {
        PackageEntity pkg = packages.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        return reviews.findByPackageIdAndApprovedTrueOrderByCreatedAtDesc(pkg.getId())
                .stream().map(ReviewDto::from).toList();
    }

    @GetMapping("/api/admin/reviews")
    public Page<ReviewDto> adminList(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) Boolean approved) {
        var pageable = PageRequest.of(page, size);
        Page<ReviewEntity> result = (approved == null)
                ? reviews.findAllByOrderByCreatedAtDesc(pageable)
                : reviews.findByApprovedOrderByCreatedAtDesc(approved, pageable);
        return result.map(ReviewDto::from);
    }

    @PatchMapping("/api/admin/reviews/{id}/approve")
    @Transactional
    public ReviewDto approve(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean approved) {
        ReviewEntity r = reviews.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        r.setApproved(approved);
        ReviewEntity saved = reviews.save(r);
        recomputePackageRating(saved.getPackageId());
        return ReviewDto.from(saved);
    }

    @DeleteMapping("/api/admin/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        ReviewEntity r = reviews.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        Long packageId = r.getPackageId();
        reviews.deleteById(id);
        recomputePackageRating(packageId);
    }

    private void recomputePackageRating(Long packageId) {
        var stats = reviews.statsForPackage(packageId);
        PackageEntity pkg = packages.findById(packageId).orElse(null);
        if (pkg == null) return;
        long count = stats == null || stats.getCount() == null ? 0L : stats.getCount();
        Double avg = stats == null ? null : stats.getAvgRating();
        pkg.setReviewsCount((int) count);
        pkg.setRating(avg == null ? null
                : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        packages.save(pkg);
    }
}
