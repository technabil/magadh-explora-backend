package com.magadhexplora.api.catalog.pkg;

import com.magadhexplora.api.catalog.category.CategoryEntity;
import com.magadhexplora.api.catalog.category.CategoryRepository;
import com.magadhexplora.api.catalog.destination.DestinationEntity;
import com.magadhexplora.api.catalog.destination.DestinationRepository;
import com.magadhexplora.api.catalog.pkg.dto.PackageImageDto;
import com.magadhexplora.api.catalog.pkg.dto.PackageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;

@Service
public class PackageService {

    private final PackageRepository packageRepo;
    private final CategoryRepository categoryRepo;
    private final DestinationRepository destinationRepo;

    public PackageService(PackageRepository packageRepo, CategoryRepository categoryRepo,
                          DestinationRepository destinationRepo) {
        this.packageRepo = packageRepo;
        this.categoryRepo = categoryRepo;
        this.destinationRepo = destinationRepo;
    }

    @Transactional
    public PackageEntity create(PackageRequest req) {
        if (packageRepo.existsBySlugIgnoreCase(req.getSlug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists");
        }
        PackageEntity p = new PackageEntity();
        applyRequest(p, req);
        return packageRepo.save(p);
    }

    @Transactional
    public PackageEntity update(Long id, PackageRequest req) {
        PackageEntity p = packageRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        applyRequest(p, req);
        return packageRepo.save(p);
    }

    @Transactional
    public void delete(Long id) {
        if (!packageRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found");
        }
        packageRepo.deleteById(id);
    }

    private void applyRequest(PackageEntity p, PackageRequest req) {
        p.setSlug(req.getSlug().trim().toLowerCase());
        p.setTitle(req.getTitle().trim());
        p.setSummary(req.getSummary());
        p.setDescription(req.getDescription());
        p.setPriceInr(req.getPriceInr());
        p.setOriginalPriceInr(req.getOriginalPriceInr());
        p.setDurationDays(req.getDurationDays());
        p.setRating(req.getRating());
        p.setReviewsCount(req.getReviewsCount() != null ? req.getReviewsCount() : 0);
        p.setGroupSizeMin(req.getGroupSizeMin());
        p.setGroupSizeMax(req.getGroupSizeMax());
        p.setHeroImageUrl(req.getHeroImageUrl());
        p.setMode(req.getMode().trim().toUpperCase());
        p.setTravelerTypes(req.getTravelerTypes());
        p.setItinerary(req.getItinerary());
        p.setInclusions(req.getInclusions());
        p.setExclusions(req.getExclusions());
        p.setPublished(req.isPublished());
        p.setFeatured(req.isFeatured());

        if (req.getCategoryIds() != null) {
            List<CategoryEntity> cats = categoryRepo.findAllByIdIn(req.getCategoryIds());
            p.setCategories(new HashSet<>(cats));
        } else {
            p.getCategories().clear();
        }

        if (req.getDestinationIds() != null) {
            List<DestinationEntity> dests = destinationRepo.findAllByIdIn(req.getDestinationIds());
            p.setDestinations(new HashSet<>(dests));
        } else {
            p.getDestinations().clear();
        }

        p.getImages().clear();
        if (req.getImages() != null) {
            for (PackageImageDto i : req.getImages()) {
                PackageImageEntity img = new PackageImageEntity();
                img.setPkg(p);
                img.setUrl(i.getUrl());
                img.setAltText(i.getAltText());
                img.setPrimary(i.isPrimary());
                img.setDisplayOrder(i.getDisplayOrder());
                p.getImages().add(img);
            }
            if ((p.getHeroImageUrl() == null || p.getHeroImageUrl().isBlank())) {
                p.getImages().stream().filter(PackageImageEntity::isPrimary).findFirst()
                        .ifPresent(primary -> p.setHeroImageUrl(primary.getUrl()));
            }
        }
    }
}
