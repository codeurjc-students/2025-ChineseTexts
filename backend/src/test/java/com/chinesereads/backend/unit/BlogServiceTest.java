package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.chinesereads.backend.Model.BlogPost;
import com.chinesereads.backend.Repository.BlogImageRepository;
import com.chinesereads.backend.Repository.BlogPostRepository;
import com.chinesereads.backend.Service.BlogService;
import com.chinesereads.backend.dto.BlogMapper;
import com.chinesereads.backend.dto.BlogPostUpsertDTO;

/**
 * Unit tests de BlogService con mocks a mano (molde HallOfFameServiceTest):
 * reglas de slug, semántica de publicación, update parcial, saneado jsoup y
 * borrado en cascada de imágenes.
 */
public class BlogServiceTest {

    private BlogPostRepository postRepository;
    private BlogImageRepository imageRepository;
    private BlogService blogService;

    @BeforeEach
    public void setUp() {
        postRepository = mock(BlogPostRepository.class);
        imageRepository = mock(BlogImageRepository.class);
        blogService = new BlogService(postRepository, imageRepository, mock(BlogMapper.class));
        when(postRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static BlogPostUpsertDTO dto(String slug, String titleEn, Boolean published) {
        return new BlogPostUpsertDTO(slug, titleEn, null, null, null, null, null, published);
    }

    @Test
    @DisplayName("create derives the slug from the title, stripping diacritics")
    public void createDerivesSlug() {
        when(postRepository.existsBySlug(any())).thenReturn(false);

        blogService.create(dto(null, "Cómo leer en chino: guía rápida", null));

        ArgumentCaptor<BlogPost> captor = ArgumentCaptor.forClass(BlogPost.class);
        verify(postRepository).save(captor.capture());
        assertEquals("como-leer-en-chino-guia-rapida", captor.getValue().getSlug());
        assertFalse(captor.getValue().isPublished());
    }

    @Test
    @DisplayName("A Chinese-only title falls back to the 'post' base and collisions auto-suffix")
    public void chineseTitleFallsBackAndSuffixes() {
        when(postRepository.existsBySlug("post")).thenReturn(true);
        when(postRepository.existsBySlug("post-2")).thenReturn(false);

        blogService.create(dto(null, "中文博客", null));

        ArgumentCaptor<BlogPost> captor = ArgumentCaptor.forClass(BlogPost.class);
        verify(postRepository).save(captor.capture());
        assertEquals("post-2", captor.getValue().getSlug());
    }

    @Test
    @DisplayName("An explicit duplicate slug throws; a missing title throws")
    public void invalidCreatesThrow() {
        when(postRepository.existsBySlug("taken")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> blogService.create(dto("taken", "Title", null)));

        assertThrows(IllegalArgumentException.class,
                () -> blogService.create(dto(null, "   ", null)));
    }

    @Test
    @DisplayName("publishedOn is set on first publish only and survives unpublish/republish")
    public void publishedOnIsSetOnce() {
        BlogPost post = new BlogPost();
        post.setSlug("my-post");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        blogService.update(1L, dto(null, null, true));
        LocalDate firstDate = post.getPublishedOn();
        assertNotNull(firstDate);
        assertTrue(post.isPublished());

        blogService.update(1L, dto(null, null, false));
        assertFalse(post.isPublished());
        assertEquals(firstDate, post.getPublishedOn());

        blogService.update(1L, dto(null, null, true));
        assertEquals(firstDate, post.getPublishedOn());
    }

    @Test
    @DisplayName("Partial update: null fields stay unchanged, non-null fields are applied")
    public void partialUpdate() {
        BlogPost post = new BlogPost();
        post.setSlug("keep-me");
        post.setTitleEn("Old title");
        post.setContentEn("<p>Old body</p>");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        blogService.update(1L, new BlogPostUpsertDTO(null, null, "Título nuevo",
                null, null, null, null, null));

        assertEquals("Old title", post.getTitleEn());
        assertEquals("Título nuevo", post.getTitleEs());
        assertEquals("<p>Old body</p>", post.getContentEn());
        assertEquals("keep-me", post.getSlug());
        assertNotNull(post.getUpdatedAt());
    }

    @Test
    @DisplayName("create sanitizes the content: keeps allowed formatting, strips scripts, handlers, external images and non-ql classes")
    public void sanitizeEnforcesSafelist() {
        when(postRepository.existsBySlug(any())).thenReturn(false);
        String dirty = "<h2 class=\"ql-align-center evil\">Title</h2>"
                + "<script>alert(1)</script>"
                + "<p onclick=\"x()\" style=\"color:red\">Hi <strong>there</strong></p>"
                + "<ol><li>One</li></ol>"
                + "<a href=\"https://example.com\">link</a>"
                + "<img src=\"https://evil.example.com/a.png\">"
                + "<img src=\"/api/blog/images/12\" alt=\"ok\">";

        blogService.create(new BlogPostUpsertDTO(null, "Sanitize me", null,
                null, null, dirty, null, null));

        ArgumentCaptor<BlogPost> captor = ArgumentCaptor.forClass(BlogPost.class);
        verify(postRepository).save(captor.capture());
        String clean = captor.getValue().getContentEn();

        assertTrue(clean.contains("<strong>there</strong>"));
        assertTrue(clean.contains("<ol>"));
        assertTrue(clean.contains("https://example.com"));
        assertTrue(clean.contains("/api/blog/images/12"));
        assertTrue(clean.contains("class=\"ql-align-center\""));
        assertFalse(clean.contains("script"));
        assertFalse(clean.contains("onclick"));
        assertFalse(clean.contains("style="));
        assertFalse(clean.contains("evil.example.com"));
        assertFalse(clean.contains("evil\""));
    }

    @Test
    @DisplayName("sanitize normalizes Quill's export: nbsp -> space and empty <p> -> <p><br></p>")
    public void sanitizeNormalizesQuillExport() {
        when(postRepository.existsBySlug(any())).thenReturn(false);

        blogService.create(new BlogPostUpsertDTO(null, "Typography", null, null, null,
                "<p>Uno&nbsp;dos&nbsp;tres</p><p></p><p>Fin</p>", null, null));

        ArgumentCaptor<BlogPost> captor = ArgumentCaptor.forClass(BlogPost.class);
        verify(postRepository).save(captor.capture());
        String clean = captor.getValue().getContentEn();

        assertTrue(clean.contains("Uno dos tres"));
        assertFalse(clean.contains("\u00A0"));
        assertFalse(clean.contains("&nbsp;"));
        assertTrue(clean.contains("<p><br></p>"));
    }

    @Test
    @DisplayName("delete removes the post's inline images first")
    public void deleteCascadesImages() {
        BlogPost post = new BlogPost();
        when(postRepository.findById(5L)).thenReturn(Optional.of(post));

        blogService.delete(5L);

        verify(imageRepository).deleteByPost(post);
        verify(postRepository).delete(post);
    }
}
