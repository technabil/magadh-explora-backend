package com.magadhexplora.api.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    List<ReviewEntity> findByPackageIdAndApprovedTrueOrderByCreatedAtDesc(Long packageId);

    Page<ReviewEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ReviewEntity> findByApprovedOrderByCreatedAtDesc(boolean approved, Pageable pageable);

    long countByPackageIdAndApprovedTrue(Long packageId);

    interface RatingStats {
        Double getAvgRating();
        Long getCount();
    }

    @org.springframework.data.jpa.repository.Query("""
        select avg(r.rating) as avgRating, count(r.id) as count
        from ReviewEntity r
        where r.packageId = :packageId and r.approved = true
        """)
    RatingStats statsForPackage(@org.springframework.data.repository.query.Param("packageId") Long packageId);
}
