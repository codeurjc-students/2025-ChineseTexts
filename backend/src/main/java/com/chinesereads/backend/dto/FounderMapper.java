package com.chinesereads.backend.dto;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.chinesereads.backend.Model.FounderItem;
import com.chinesereads.backend.Model.FounderProfile;
import com.chinesereads.backend.Model.FounderSection;
import com.chinesereads.backend.Model.FounderSocial;

/**
 * Mapea las entidades del perfil a sus DTO (sólo lectura → API).
 *
 * Las imágenes (Blob) NO se serializan: se exponen por endpoints propios
 * (`/photo`, `/items/{id}/logo`). En su lugar el DTO lleva un booleano
 * `hasPhoto` / `hasLogo` para que el frontend sepa si debe pedir la imagen.
 * Las listas anidadas se mapean automáticamente en el orden de `@OrderBy`.
 */
@Mapper(componentModel = "spring")
public interface FounderMapper {

    @Mapping(target = "hasPhoto", expression = "java(profile.getPhoto() != null)")
    FounderProfileDTO toDTO(FounderProfile profile);

    FounderSocialDTO toDTO(FounderSocial social);

    FounderSectionDTO toDTO(FounderSection section);

    @Mapping(target = "hasLogo", expression = "java(item.getLogo() != null)")
    FounderItemDTO toDTO(FounderItem item);

    List<FounderSocialDTO> toSocialDTOs(List<FounderSocial> socials);

    List<FounderSectionDTO> toSectionDTOs(List<FounderSection> sections);

    List<FounderItemDTO> toItemDTOs(List<FounderItem> items);
}
