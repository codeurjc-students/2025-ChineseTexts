package com.chinesereads.backend.dto;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.chinesereads.backend.Model.HallOfFameEntry;
import com.chinesereads.backend.Model.HallOfFameSocial;

/**
 * Mapea las entidades del Hall of Fame a sus DTO (sólo lectura → API).
 *
 * La foto (Blob) NO se serializa: se expone por su endpoint propio
 * (`/{id}/photo`). En su lugar el DTO lleva un booleano `hasPhoto` para que
 * el frontend sepa si debe pedir la imagen. Las listas anidadas se mapean
 * automáticamente en el orden de `@OrderBy`.
 */
@Mapper(componentModel = "spring")
public interface HallOfFameMapper {

    @Mapping(target = "hasPhoto", expression = "java(entry.getPhoto() != null)")
    HallOfFameEntryDTO toDTO(HallOfFameEntry entry);

    HallOfFameSocialDTO toDTO(HallOfFameSocial social);

    List<HallOfFameEntryDTO> toEntryDTOs(List<HallOfFameEntry> entries);

    List<HallOfFameSocialDTO> toSocialDTOs(List<HallOfFameSocial> socials);
}
