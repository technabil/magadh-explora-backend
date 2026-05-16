package com.magadhexplora.api.i18n;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TranslationRepository extends JpaRepository<TranslationEntity, Long> {

    List<TranslationEntity> findByEntityTypeAndEntityIdAndLang(String entityType, Long entityId, String lang);

    Optional<TranslationEntity> findByEntityTypeAndEntityIdAndLangAndField(
            String entityType, Long entityId, String lang, String field);

    @Transactional
    void deleteByEntityTypeAndEntityIdAndLangAndField(
            String entityType, Long entityId, String lang, String field);

    List<TranslationEntity> findByEntityTypeAndEntityId(String entityType, Long entityId);
}
