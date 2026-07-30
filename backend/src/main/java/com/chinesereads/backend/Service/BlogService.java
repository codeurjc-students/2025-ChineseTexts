package com.chinesereads.backend.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.hibernate.engine.jdbc.proxy.BlobProxy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.chinesereads.backend.Model.BlogImage;
import com.chinesereads.backend.Model.BlogPost;
import com.chinesereads.backend.Repository.BlogImageRepository;
import com.chinesereads.backend.Repository.BlogPostRepository;
import com.chinesereads.backend.dto.BlogMapper;
import com.chinesereads.backend.dto.BlogPostDTO;
import com.chinesereads.backend.dto.BlogPostSummaryDTO;
import com.chinesereads.backend.dto.BlogPostUpsertDTO;

/**
 * Blog estilo Medium gestionado por el admin. Reglas clave:
 *
 * - El HTML de los cuerpos llega del editor Quill y se persiste SIEMPRE tras
 *   pasar por {@link #sanitize(String)} (safelist jsoup estricta): sin
 *   scripts, sin estilos inline, imágenes solo de /api/blog/images/{id} y
 *   clases solo ql-* (alineaciones/indent de Quill).
 * - Un post solo es público cuando published es true; publishedOn se fija la
 *   PRIMERA vez que se publica y no se re-fija; updatedAt en cada guardado.
 * - Slug: patrón del Hall of Fame — derivado del título (EN, si no ES) con
 *   normalización sin diacríticos y autosufijo -2, -3…; un slug explícito
 *   inválido o duplicado es un error 400.
 */
@Service
@Transactional
public class BlogService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    /** Solo se permiten imágenes inline subidas al propio blog. */
    private static final Pattern INLINE_IMAGE_SRC = Pattern.compile("/api/blog/images/\\d+");

    /** Solo se conservan las clases de formato de Quill (ql-align-*, ql-indent-*…). */
    private static final Pattern QUILL_CLASS = Pattern.compile("ql-[a-z0-9-]+");

    // Etiquetas que puede producir la toolbar del editor (h1 reservado al
    // título del post; ver blog-editor). Los enlaces relativos se conservan
    // (enlaces internos a /learn, /texts…); los absolutos, solo http/https/mailto.
    private static final Safelist SAFELIST = new Safelist()
            .addTags("p", "br", "strong", "em", "u", "s", "blockquote", "h2", "h3",
                    "ol", "ul", "li", "a", "img", "span", "pre", "code", "sub", "sup")
            .addAttributes("a", "href", "target", "rel")
            .addAttributes("img", "src", "alt", "width", "height")
            .addAttributes(":all", "class")
            .addProtocols("a", "href", "http", "https", "mailto")
            .preserveRelativeLinks(true);

    private final BlogPostRepository postRepository;
    private final BlogImageRepository imageRepository;
    private final BlogMapper mapper;

    public BlogService(BlogPostRepository postRepository, BlogImageRepository imageRepository,
                       BlogMapper mapper) {
        this.postRepository = postRepository;
        this.imageRepository = imageRepository;
        this.mapper = mapper;
    }

    // ---------- Lectura ----------

    public List<BlogPostSummaryDTO> getPublishedSummaries() {
        return mapper.toSummaryDTOs(postRepository.findPublishedSummaries());
    }

    public List<BlogPostSummaryDTO> getAllSummaries() {
        return mapper.toSummaryDTOs(postRepository.findAllSummaries());
    }

    /** Detalle público: solo posts publicados (un borrador es 404 para todos). */
    public BlogPostDTO getPublishedBySlug(String slug) {
        BlogPost post = postRepository.findBySlug(slug)
                .filter(BlogPost::isPublished)
                .orElseThrow(() -> new RuntimeException("Blog post not found with slug: " + slug));
        return mapper.toDTO(post);
    }

    /** Detalle admin (editor): borradores incluidos. */
    public BlogPostDTO getById(long id) {
        return mapper.toDTO(findPost(id));
    }

    // ---------- Escritura ----------

    public BlogPostDTO create(BlogPostUpsertDTO data) {
        if (isBlank(data.titleEn()) && isBlank(data.titleEs())) {
            throw new IllegalArgumentException("A title (EN or ES) is required");
        }
        BlogPost post = new BlogPost();
        post.setTitleEn(data.titleEn());
        post.setTitleEs(data.titleEs());
        post.setExcerptEn(data.excerptEn());
        post.setExcerptEs(data.excerptEs());
        post.setContentEn(sanitize(data.contentEn()));
        post.setContentEs(sanitize(data.contentEs()));
        post.setSlug(resolveSlug(data.slug(), firstNonBlank(data.titleEn(), data.titleEs())));
        if (Boolean.TRUE.equals(data.published())) {
            post.setPublished(true);
            post.setPublishedOn(LocalDate.now());
        }
        post.setUpdatedAt(LocalDateTime.now());
        return mapper.toDTO(postRepository.save(post));
    }

    /** Update parcial: cada campo se pisa solo si llega non-null (Jackson 3). */
    public BlogPostDTO update(long id, BlogPostUpsertDTO data) {
        BlogPost post = findPost(id);
        if (data.titleEn() != null) post.setTitleEn(data.titleEn());
        if (data.titleEs() != null) post.setTitleEs(data.titleEs());
        if (data.excerptEn() != null) post.setExcerptEn(data.excerptEn());
        if (data.excerptEs() != null) post.setExcerptEs(data.excerptEs());
        if (data.contentEn() != null) post.setContentEn(sanitize(data.contentEn()));
        if (data.contentEs() != null) post.setContentEs(sanitize(data.contentEs()));
        if (data.slug() != null && !data.slug().equals(post.getSlug())) {
            String slug = normalizeSlug(data.slug());
            if (!SLUG_PATTERN.matcher(slug).matches()) {
                throw new IllegalArgumentException("Invalid slug: " + data.slug());
            }
            if (postRepository.existsBySlug(slug)) {
                throw new IllegalArgumentException("Slug already in use: " + slug);
            }
            post.setSlug(slug);
        }
        if (data.published() != null) {
            post.setPublished(data.published());
            // Solo la primera publicación fija la fecha (no se re-fija después).
            if (data.published() && post.getPublishedOn() == null) {
                post.setPublishedOn(LocalDate.now());
            }
        }
        post.setUpdatedAt(LocalDateTime.now());
        return mapper.toDTO(postRepository.save(post));
    }

    public void delete(long id) {
        BlogPost post = findPost(id);
        imageRepository.deleteByPost(post);
        postRepository.delete(post);
    }

    // ---------- Portada ----------

    public Resource getCover(long id) {
        BlogPost post = findPost(id);
        if (post.getCover() == null) {
            throw new RuntimeException("Post has no cover");
        }
        try {
            return new InputStreamResource(post.getCover().getBinaryStream());
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving post cover", e);
        }
    }

    public void setCover(long id, MultipartFile image) throws IOException {
        BlogPost post = findPost(id);
        post.setCover(BlobProxy.generateProxy(image.getInputStream(), image.getSize()));
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    public void deleteCover(long id) {
        BlogPost post = findPost(id);
        post.setCover(null);
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    // ---------- Imágenes inline ----------

    /**
     * Sube una imagen inline y devuelve id + URL para insertarla en Quill.
     * postId es opcional: en un borrador aún sin guardar la imagen queda sin
     * post (huérfana tolerada en v1; solo las crea el admin).
     */
    public Map<String, Object> addImage(MultipartFile image, Long postId) throws IOException {
        BlogImage entity = new BlogImage();
        entity.setImage(BlobProxy.generateProxy(image.getInputStream(), image.getSize()));
        if (postId != null) {
            entity.setPost(findPost(postId));
        }
        BlogImage saved = imageRepository.save(entity);
        return Map.of("id", saved.getId(), "url", "/api/blog/images/" + saved.getId());
    }

    public Resource getImage(long id) {
        BlogImage image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blog image not found with id: " + id));
        try {
            return new InputStreamResource(image.getImage().getBinaryStream());
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving blog image", e);
        }
    }

    // ---------- Saneado ----------

    /**
     * Sanea el HTML del editor: safelist estricta + dos postfiltros — las
     * imágenes solo pueden apuntar a /api/blog/images/{id} (fuera data:,
     * externas…) y del atributo class solo sobreviven clases ql-*.
     *
     * Además normaliza dos defectos del exportador HTML de Quill 2 que rompen
     * la tipografía: convierte TODOS los espacios en &amp;nbsp; (el navegador
     * no puede partir líneas por palabras y las corta por cualquier sitio) y
     * exporta las líneas en blanco como &lt;p&gt;&lt;/p&gt; vacíos que se
     * colapsan (los saltos de línea "desaparecen") — aquí vuelven a ser
     * espacios normales y &lt;p&gt;&lt;br&gt;&lt;/p&gt; visibles.
     */
    private String sanitize(String html) {
        if (html == null) {
            return null;
        }
        Document.OutputSettings compact = new Document.OutputSettings().prettyPrint(false);
        String clean = Jsoup.clean(html, "", SAFELIST, compact);
        Document doc = Jsoup.parseBodyFragment(clean);
        doc.outputSettings(compact);
        for (Element img : doc.select("img")) {
            if (!INLINE_IMAGE_SRC.matcher(img.attr("src")).matches()) {
                img.remove();
            }
        }
        for (Element el : doc.select("[class]")) {
            List<String> kept = new ArrayList<>();
            for (String cls : el.attr("class").trim().split("\\s+")) {
                if (QUILL_CLASS.matcher(cls).matches()) {
                    kept.add(cls);
                }
            }
            if (kept.isEmpty()) {
                el.removeAttr("class");
            } else {
                el.attr("class", String.join(" ", kept));
            }
        }
        // Párrafo vacío (línea en blanco del editor) → <p><br></p> renderizable.
        for (Element p : doc.select("p")) {
            if (p.select("br, img").isEmpty() && p.text().strip().isEmpty()) {
                p.empty().appendElement("br");
            }
        }
        // jsoup serializa el carácter nbsp como la ENTIDAD &nbsp;, así que se
        // normaliza sobre la salida ya serializada. Un "&nbsp;" literal escrito
        // por el autor sale como &amp;nbsp; y no coincide: no se toca.
        return doc.body().html().replace("&nbsp;", " ");
    }

    // ---------- Slug (patrón Hall of Fame) ----------

    private String resolveSlug(String requested, String title) {
        if (requested != null && !requested.isBlank()) {
            String slug = normalizeSlug(requested);
            if (!SLUG_PATTERN.matcher(slug).matches()) {
                throw new IllegalArgumentException("Invalid slug: " + requested);
            }
            if (postRepository.existsBySlug(slug)) {
                throw new IllegalArgumentException("Slug already in use: " + slug);
            }
            return slug;
        }
        String base = normalizeSlug(title);
        if (base.isBlank()) {
            base = "post"; // p. ej. un título solo en chino se normaliza a vacío
        }
        String slug = base;
        int suffix = 2;
        while (postRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private String normalizeSlug(String value) {
        String noDiacritics = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noDiacritics.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    // ---------- Helpers ----------

    private BlogPost findPost(long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blog post not found with id: " + id));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String firstNonBlank(String a, String b) {
        return !isBlank(a) ? a : (b != null ? b : "");
    }
}
