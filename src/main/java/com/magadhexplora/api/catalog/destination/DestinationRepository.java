package com.magadhexplora.api.catalog.destination;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DestinationRepository extends JpaRepository<DestinationEntity, Long> {
    List<DestinationEntity> findAllByOrderByNameAsc();
    List<DestinationEntity> findByActiveOrderByNameAsc(boolean active);
    Optional<DestinationEntity> findBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCase(String slug);
    List<DestinationEntity> findAllByIdIn(List<Long> ids);
}
