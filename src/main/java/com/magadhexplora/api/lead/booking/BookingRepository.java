package com.magadhexplora.api.lead.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
    Page<BookingEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<BookingEntity> findByEmailIgnoreCaseOrderByCreatedAtDesc(String email);
    Optional<BookingEntity> findByViewToken(String viewToken);

    @Query("""
        select b from BookingEntity b
        where (:status is null or b.status = :status)
          and (:paymentStatus is null or b.paymentStatus = :paymentStatus)
          and (:from is null or b.createdAt >= :from)
          and (:to is null or b.createdAt < :to)
        """)
    Page<BookingEntity> findFiltered(@Param("status") String status,
                                     @Param("paymentStatus") String paymentStatus,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to,
                                     Pageable pageable);
}
