package com.magadhexplora.api.catalog.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    List<CategoryEntity> findAllByOrderByKindAscDisplayOrderAsc();
    List<CategoryEntity> findByKindOrderByDisplayOrderAsc(String kind);
    Optional<CategoryEntity> findBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCase(String slug);

    @Query("select c from CategoryEntity c where c.id in :ids")
    List<CategoryEntity> findAllByIdIn(List<Long> ids);
}
