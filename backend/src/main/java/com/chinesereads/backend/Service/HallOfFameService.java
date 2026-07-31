package com.chinesereads.backend.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

import org.hibernate.engine.jdbc.proxy.BlobProxy;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.chinesereads.backend.Model.HallOfFameBadges;
import com.chinesereads.backend.Model.HallOfFameEntry;
import com.chinesereads.backend.Model.HallOfFameSocial;
import com.chinesereads.backend.Repository.HallOfFameEntryRepository;
import com.chinesereads.backend.Repository.HallOfFameSocialRepository;
import com.chinesereads.backend.dto.HallOfFameEntryDTO;
import com.chinesereads.backend.dto.HallOfFameMapper;
import com.chinesereads.backend.dto.HallOfFameSocialDTO;

/**
 * Lógica del Hall of Fame (/hall-of-fame): lista de influencers/colaboradores
 * gestionada por el admin. Las fotos se guardan como Blob en MySQL, siguiendo
 * el mismo patrón que {@link FounderService}.
 */
@Service
@Transactional
public class HallOfFameService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    private final HallOfFameEntryRepository entryRepository;

    private final HallOfFameSocialRepository socialRepository;

    private final HallOfFameMapper hallOfFameMapper;

    public HallOfFameService(HallOfFameEntryRepository entryRepository, HallOfFameSocialRepository socialRepository, HallOfFameMapper hallOfFameMapper) {
        this.entryRepository = entryRepository;
        this.socialRepository = socialRepository;
        this.hallOfFameMapper = hallOfFameMapper;
    }

    // ---------- Entradas ----------

    public List<HallOfFameEntryDTO> getAll() {
        return hallOfFameMapper.toEntryDTOs(entryRepository.findAllByOrderByDisplayOrderAscIdAsc());
    }

    /** Página detalle pública: entrada por slug o NoSuchElementException (→404). */
    public HallOfFameEntryDTO getBySlug(String slug) {
        return hallOfFameMapper.toDTO(entryRepository.findBySlug(slug).orElseThrow());
    }

    public HallOfFameEntryDTO create(HallOfFameEntryDTO data) {
        if (data.name() == null || data.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        HallOfFameEntry entry = new HallOfFameEntry();
        entry.setName(data.name().trim());
        entry.setSlug(resolveSlug(data.slug(), data.name()));
        entry.setTaglineEn(data.taglineEn());
        entry.setTaglineEs(data.taglineEs());
        entry.setBioEn(data.bioEn());
        entry.setBioEs(data.bioEs());
        entry.setDiscountCode(data.discountCode());
        entry.setDisplayOrder((int) entryRepository.count());
        if (data.badges() != null) {
            entry.setBadges(HallOfFameBadges.normalize(data.badges()));
        }
        return hallOfFameMapper.toDTO(entryRepository.save(entry));
    }

    /**
     * Actualización parcial: cada campo sólo se pisa si viene no-null (por eso
     * los DTO usan wrappers). Una lista de badges VACÍA sí borra los badges
     * (vacía != null).
     */
    public HallOfFameEntryDTO update(long id, HallOfFameEntryDTO data) {
        HallOfFameEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hall of Fame entry not found with id: " + id));
        if (data.name() != null) {
            entry.setName(data.name().trim());
        }
        if (data.slug() != null && !data.slug().equals(entry.getSlug())) {
            String slug = normalizeSlug(data.slug());
            if (!SLUG_PATTERN.matcher(slug).matches()) {
                throw new IllegalArgumentException("Invalid slug: " + data.slug());
            }
            if (entryRepository.existsBySlug(slug)) {
                throw new IllegalArgumentException("Slug already in use: " + slug);
            }
            entry.setSlug(slug);
        }
        if (data.taglineEn() != null) {
            entry.setTaglineEn(data.taglineEn());
        }
        if (data.taglineEs() != null) {
            entry.setTaglineEs(data.taglineEs());
        }
        if (data.bioEn() != null) {
            entry.setBioEn(data.bioEn());
        }
        if (data.bioEs() != null) {
            entry.setBioEs(data.bioEs());
        }
        if (data.discountCode() != null) {
            entry.setDiscountCode(data.discountCode());
        }
        if (data.displayOrder() != null) {
            entry.setDisplayOrder(data.displayOrder());
        }
        if (data.badges() != null) {
            entry.setBadges(HallOfFameBadges.normalize(data.badges()));
        }
        return hallOfFameMapper.toDTO(entryRepository.save(entry));
    }

    public void delete(long id) {
        if (!entryRepository.existsById(id)) {
            throw new RuntimeException("Hall of Fame entry not found with id: " + id);
        }
        entryRepository.deleteById(id);
    }

    // ---------- Slug ----------

    /**
     * Resuelve el slug al crear: si el admin no manda uno, se deriva del
     * nombre; si ya está ocupado, se autosufija (-2, -3…). Un slug explícito
     * duplicado o inválido es error (el admin lo eligió a propósito).
     */
    private String resolveSlug(String requested, String name) {
        if (requested != null && !requested.isBlank()) {
            String slug = normalizeSlug(requested);
            if (!SLUG_PATTERN.matcher(slug).matches()) {
                throw new IllegalArgumentException("Invalid slug: " + requested);
            }
            if (entryRepository.existsBySlug(slug)) {
                throw new IllegalArgumentException("Slug already in use: " + slug);
            }
            return slug;
        }
        String base = normalizeSlug(name);
        if (base.isBlank()) {
            base = "entry";
        }
        String slug = base;
        int suffix = 2;
        while (entryRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    /** Minúsculas, sin diacríticos, y todo lo no alfanumérico colapsado a guiones. */
    private String normalizeSlug(String value) {
        String noDiacritics = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noDiacritics.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    // ---------- Foto ----------

    public Resource getPhoto(long id) {
        HallOfFameEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hall of Fame entry not found with id: " + id));
        if (entry.getPhoto() == null) {
            throw new RuntimeException("Entry has no photo");
        }
        try {
            return new InputStreamResource(entry.getPhoto().getBinaryStream());
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving entry photo", e);
        }
    }

    public void setPhoto(long id, MultipartFile image) throws IOException {
        HallOfFameEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hall of Fame entry not found with id: " + id));
        entry.setPhoto(BlobProxy.generateProxy(image.getInputStream(), image.getSize()));
        entryRepository.save(entry);
    }

    public void deletePhoto(long id) {
        HallOfFameEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hall of Fame entry not found with id: " + id));
        entry.setPhoto(null);
        entryRepository.save(entry);
    }

    // ---------- Redes sociales ----------

    public HallOfFameSocialDTO addSocial(long entryId, HallOfFameSocialDTO data) {
        HallOfFameEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Hall of Fame entry not found with id: " + entryId));
        HallOfFameSocial social = new HallOfFameSocial();
        social.setEntry(entry);
        social.setLabel(data.label());
        social.setIcon(data.icon());
        social.setUrl(data.url());
        social.setDisplayOrder(entry.getSocials().size());
        return hallOfFameMapper.toDTO(socialRepository.save(social));
    }

    public HallOfFameSocialDTO updateSocial(long id, HallOfFameSocialDTO data) {
        HallOfFameSocial social = socialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Social link not found with id: " + id));
        if (data.label() != null) {
            social.setLabel(data.label());
        }
        if (data.icon() != null) {
            social.setIcon(data.icon());
        }
        if (data.url() != null) {
            social.setUrl(data.url());
        }
        if (data.displayOrder() != null) {
            social.setDisplayOrder(data.displayOrder());
        }
        return hallOfFameMapper.toDTO(socialRepository.save(social));
    }

    public void deleteSocial(long id) {
        if (!socialRepository.existsById(id)) {
            throw new RuntimeException("Social link not found with id: " + id);
        }
        socialRepository.deleteById(id);
    }
}
