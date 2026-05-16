package com.magadhexplora.api.lead.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<ContactEntity, Long> {
    Page<ContactEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
