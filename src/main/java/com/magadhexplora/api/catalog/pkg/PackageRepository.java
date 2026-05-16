package com.magadhexplora.api.catalog.pkg;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PackageRepository extends JpaRepository<PackageEntity, Long> {

    Optional<PackageEntity> findBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCase(String slug);

    @Query("""
        select distinct p from PackageEntity p
        left join p.categories c
        where p.published = true
          and (:q is null or lower(p.title) like lower(concat('%', :q, '%'))
                          or lower(coalesce(p.summary,'')) like lower(concat('%', :q, '%')))
          and (:mode is null or p.mode = :mode)
          and (:categorySlug is null or c.slug = :categorySlug)
          and (:travelerType is null or p.travelerTypes like concat('%', :travelerType, '%'))
          and (:minDays is null or p.durationDays >= :minDays)
          and (:maxDays is null or p.durationDays <= :maxDays)
        order by p.featured desc, p.createdAt desc
        """)
    List<PackageEntity> findPublic(@Param("q") String q,
                                   @Param("mode") String mode,
                                   @Param("categorySlug") String categorySlug,
                                   @Param("travelerType") String travelerType,
                                   @Param("minDays") Integer minDays,
                                   @Param("maxDays") Integer maxDays);

    @Query("select p from PackageEntity p order by p.createdAt desc")
    List<PackageEntity> findAllForAdmin();
}
