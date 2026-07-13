package com.chinesereads.backend.Service;

import java.io.IOException;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.proxy.BlobProxy;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.chinesereads.backend.Model.FounderItem;
import com.chinesereads.backend.Model.FounderProfile;
import com.chinesereads.backend.Model.FounderSection;
import com.chinesereads.backend.Model.FounderSocial;
import com.chinesereads.backend.Repository.FounderItemRepository;
import com.chinesereads.backend.Repository.FounderProfileRepository;
import com.chinesereads.backend.Repository.FounderSectionRepository;
import com.chinesereads.backend.Repository.FounderSocialRepository;
import com.chinesereads.backend.dto.FounderItemDTO;
import com.chinesereads.backend.dto.FounderMapper;
import com.chinesereads.backend.dto.FounderProfileDTO;
import com.chinesereads.backend.dto.FounderSectionDTO;
import com.chinesereads.backend.dto.FounderSocialDTO;

/**
 * Lógica del perfil del creador (/founder). Sólo existe un perfil; el resto de
 * operaciones cuelgan de él. Las imágenes se guardan como Blob en MySQL,
 * siguiendo el mismo patrón que {@link TextService}.
 */
@Service
@Transactional
public class FounderService {

    private final FounderProfileRepository profileRepository;

    private final FounderSocialRepository socialRepository;

    private final FounderSectionRepository sectionRepository;

    private final FounderItemRepository itemRepository;

    private final FounderMapper founderMapper;

    public FounderService(FounderProfileRepository profileRepository, FounderSocialRepository socialRepository, FounderSectionRepository sectionRepository, FounderItemRepository itemRepository, FounderMapper founderMapper) {
        this.profileRepository = profileRepository;
        this.socialRepository = socialRepository;
        this.sectionRepository = sectionRepository;
        this.itemRepository = itemRepository;
        this.founderMapper = founderMapper;
    }

    // ---------- Perfil ----------

    /** Devuelve el perfil único, creándolo vacío la primera vez si no existe. */
    public FounderProfile getOrCreateProfile() {
        return profileRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> profileRepository.save(new FounderProfile()));
    }

    public FounderProfileDTO getProfile() {
        return founderMapper.toDTO(getOrCreateProfile());
    }

    public FounderProfileDTO updateProfile(FounderProfileDTO data) {
        FounderProfile profile = getOrCreateProfile();
        profile.setName(data.name());
        profile.setRole(data.role());
        profile.setTagline(data.tagline());
        profile.setLocation(data.location());
        profile.setSummary(data.summary());
        return founderMapper.toDTO(profileRepository.save(profile));
    }

    // ---------- Foto de perfil ----------

    public Resource getPhoto() {
        FounderProfile profile = getOrCreateProfile();
        if (profile.getPhoto() == null) {
            throw new RuntimeException("Founder profile has no photo");
        }
        try {
            return new InputStreamResource(profile.getPhoto().getBinaryStream());
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving founder photo", e);
        }
    }

    public void setPhoto(MultipartFile image) throws IOException {
        FounderProfile profile = getOrCreateProfile();
        profile.setPhoto(BlobProxy.generateProxy(image.getInputStream(), image.getSize()));
        profileRepository.save(profile);
    }

    public void deletePhoto() {
        FounderProfile profile = getOrCreateProfile();
        profile.setPhoto(null);
        profileRepository.save(profile);
    }

    // ---------- Enlaces / redes ----------

    public FounderSocialDTO addSocial(FounderSocialDTO data) {
        FounderProfile profile = getOrCreateProfile();
        FounderSocial social = new FounderSocial();
        social.setProfile(profile);
        social.setLabel(data.label());
        social.setIcon(data.icon());
        social.setUrl(data.url());
        social.setDisplayOrder(profile.getSocials().size());
        return founderMapper.toDTO(socialRepository.save(social));
    }

    public FounderSocialDTO updateSocial(long id, FounderSocialDTO data) {
        FounderSocial social = socialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Social link not found with id: " + id));
        social.setLabel(data.label());
        social.setIcon(data.icon());
        social.setUrl(data.url());
        if (data.displayOrder() != null) {
            social.setDisplayOrder(data.displayOrder());
        }
        return founderMapper.toDTO(socialRepository.save(social));
    }

    public void deleteSocial(long id) {
        if (!socialRepository.existsById(id)) {
            throw new RuntimeException("Social link not found with id: " + id);
        }
        socialRepository.deleteById(id);
    }

    // ---------- Secciones ----------

    public FounderSectionDTO addSection(FounderSectionDTO data) {
        FounderProfile profile = getOrCreateProfile();
        FounderSection section = new FounderSection();
        section.setProfile(profile);
        section.setTitle(data.title());
        section.setType(data.type() != null ? data.type() : "CUSTOM");
        section.setDisplayOrder(profile.getSections().size());
        return founderMapper.toDTO(sectionRepository.save(section));
    }

    public FounderSectionDTO updateSection(long id, FounderSectionDTO data) {
        FounderSection section = sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Section not found with id: " + id));
        section.setTitle(data.title());
        if (data.type() != null) {
            section.setType(data.type());
        }
        if (data.displayOrder() != null) {
            section.setDisplayOrder(data.displayOrder());
        }
        return founderMapper.toDTO(sectionRepository.save(section));
    }

    public void deleteSection(long id) {
        if (!sectionRepository.existsById(id)) {
            throw new RuntimeException("Section not found with id: " + id);
        }
        sectionRepository.deleteById(id);
    }

    // ---------- Ítems ----------

    public FounderItemDTO addItem(long sectionId, FounderItemDTO data) {
        FounderSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found with id: " + sectionId));
        FounderItem item = new FounderItem();
        item.setSection(section);
        applyItemFields(item, data);
        item.setDisplayOrder(section.getItems().size());
        return founderMapper.toDTO(itemRepository.save(item));
    }

    public FounderItemDTO updateItem(long id, FounderItemDTO data) {
        FounderItem item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found with id: " + id));
        applyItemFields(item, data);
        if (data.displayOrder() != null) {
            item.setDisplayOrder(data.displayOrder());
        }
        return founderMapper.toDTO(itemRepository.save(item));
    }

    public void deleteItem(long id) {
        if (!itemRepository.existsById(id)) {
            throw new RuntimeException("Item not found with id: " + id);
        }
        itemRepository.deleteById(id);
    }

    private void applyItemFields(FounderItem item, FounderItemDTO data) {
        item.setHeading(data.heading());
        item.setSubheading(data.subheading());
        item.setPeriod(data.period());
        item.setLocation(data.location());
        item.setDescription(data.description());
        item.setLinkUrl(data.linkUrl());
        item.setLinkLabel(data.linkLabel());
    }

    // ---------- Logo de un ítem ----------

    public Resource getItemLogo(long itemId) {
        FounderItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found with id: " + itemId));
        if (item.getLogo() == null) {
            throw new RuntimeException("Item has no logo");
        }
        try {
            return new InputStreamResource(item.getLogo().getBinaryStream());
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving item logo", e);
        }
    }

    public void setItemLogo(long itemId, MultipartFile image) throws IOException {
        FounderItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found with id: " + itemId));
        item.setLogo(BlobProxy.generateProxy(image.getInputStream(), image.getSize()));
        itemRepository.save(item);
    }

    public void deleteItemLogo(long itemId) {
        FounderItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found with id: " + itemId));
        item.setLogo(null);
        itemRepository.save(item);
    }
}
