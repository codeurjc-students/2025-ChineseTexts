package com.chinesereads.backend.dto;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.chinesereads.backend.Model.BlogPost;
import com.chinesereads.backend.Repository.BlogPostRepository.BlogSummaryRow;

/**
 * Mapea el blog a sus DTO (sólo lectura → API). La portada (Blob) NO se
 * serializa: se expone en `/{id}/cover` y el DTO lleva `hasCover`. Los
 * resúmenes se mapean desde la proyección BlogSummaryRow (getters homónimos).
 */
@Mapper(componentModel = "spring")
public interface BlogMapper {

    @Mapping(target = "hasCover", expression = "java(post.getCover() != null)")
    BlogPostDTO toDTO(BlogPost post);

    BlogPostSummaryDTO toSummaryDTO(BlogSummaryRow row);

    List<BlogPostSummaryDTO> toSummaryDTOs(List<BlogSummaryRow> rows);
}
