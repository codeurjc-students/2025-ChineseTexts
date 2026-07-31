package com.chinesereads.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.chinesereads.backend.Model.HallOfFameEntry;

public interface HallOfFameEntryRepository extends JpaRepository<HallOfFameEntry, Long> {

    List<HallOfFameEntry> findAllByOrderByDisplayOrderAscIdAsc();

    boolean existsBySlug(String slug);

    /** Página detalle pública /hall-of-fame/:slug. */
    Optional<HallOfFameEntry> findBySlug(String slug);

    /**
     * Fila mínima para el sitemap dinámico. Proyección imprescindible: la
     * entidad lleva la foto como @Lob y un findAll materializaría todos los
     * Blobs solo para listar slugs. Sin fecha de modificación en la entidad,
     * así que el sitemap va sin lastmod (appendUrl lo tolera).
     */
    interface HallOfFameSitemapRow {
        String getSlug();
    }

    @Query("SELECT e.slug AS slug FROM HallOfFameEntry e ORDER BY e.displayOrder, e.id")
    List<HallOfFameSitemapRow> findSitemapRows();
}
