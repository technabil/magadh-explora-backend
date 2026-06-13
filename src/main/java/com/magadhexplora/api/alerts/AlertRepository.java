package com.magadhexplora.api.alerts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlertRepository extends JpaRepository<AlertEntity, Long> {
    Optional<AlertEntity> findByDedupeKey(String dedupeKey);
    Page<AlertEntity> findAllByOrderByReadAscCreatedAtDesc(Pageable pageable);
    long countByReadFalse();
}
