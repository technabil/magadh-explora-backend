package com.magadhexplora.api.journey;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JourneyPostRepository extends JpaRepository<JourneyPostEntity, Long> {

    List<JourneyPostEntity> findByApprovedTrueAndMediaTypeOrderByCreatedAtDesc(String mediaType);

    List<JourneyPostEntity> findByApprovedTrueOrderByCreatedAtDesc();

    Page<JourneyPostEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<JourneyPostEntity> findByApprovedOrderByCreatedAtDesc(boolean approved, Pageable pageable);

    long countByApprovedFalse();
}
