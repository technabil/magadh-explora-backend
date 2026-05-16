package com.magadhexplora.api.lead.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
    Page<BookingEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<BookingEntity> findByEmailIgnoreCaseOrderByCreatedAtDesc(String email);
}
