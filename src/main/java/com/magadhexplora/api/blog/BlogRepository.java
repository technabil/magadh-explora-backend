package com.magadhexplora.api.blog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlogRepository extends JpaRepository<BlogEntity, Long> {

    List<BlogEntity> findByPublishedTrueOrderByPublishedAtDesc();

    List<BlogEntity> findAllByOrderByCreatedAtDesc();

    Optional<BlogEntity> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);
}
