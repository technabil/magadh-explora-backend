package com.magadhexplora.api.homepage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HomepageSectionRepository extends JpaRepository<HomepageSectionEntity, Long> {

    List<HomepageSectionEntity> findAllByOrderByDisplayOrderAsc();

    List<HomepageSectionEntity> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<HomepageSectionEntity> findBySectionKey(String sectionKey);

    boolean existsBySectionKey(String sectionKey);
}
