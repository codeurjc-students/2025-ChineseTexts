package com.chinesereads.backend.dto;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.chinesereads.backend.Model.Collection;

@Mapper(componentModel = "spring")
public interface CollectionMapper {

    CollectionDTO toDTO(Collection collection);

    List<CollectionDTO> toDTO(List<Collection> collections);

    @Mapping(target = "flashcards", ignore = true)
    @Mapping(target = "user", ignore = true)
    Collection toDomain(CollectionDTO collectionDTO);
}