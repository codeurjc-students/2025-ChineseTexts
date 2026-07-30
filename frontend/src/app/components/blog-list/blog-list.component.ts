import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { LoginService } from '../../services/login.service';
import { SeoService, SITE_URL } from '../../services/seo.service';
import { Lang, addLangPrefix } from '../../i18n/locale.util';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';
import { BlogService, BlogPostSummary } from '../../services/blog.service';

/**
 * Listado del blog (/blog): tarjetas de los posts publicados. Servido por SSR
 * real (frontend-ssr vía Caddy, como /text/* y /hall-of-fame): la carga NO se
 * limita al navegador y el HTML servido lleva contenido y SEO refinado.
 *
 * El admin ve además el botón "Nuevo post", el de editar en cada tarjeta y un
 * toggle para listar también los borradores (petición con credenciales que
 * solo se dispara con su clic, nunca en SSR).
 */
@Component({
  selector: 'app-blog-list',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule, LocalizeLinkPipe],
  templateUrl: './blog-list.component.html',
  styleUrl: './blog-list.component.scss'
})
export class BlogListComponent implements OnInit {

  posts: BlogPostSummary[] = [];
  loading = true;
  /** true cuando el admin está viendo también los borradores. */
  showingAll = false;

  constructor(
    private blogService: BlogService,
    public login: LoginService,
    private transloco: TranslocoService,
    private seo: SeoService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  get isAdmin(): boolean {
    return this.login.isRoleAdmin();
  }

  get es(): boolean {
    return this.transloco.getActiveLang() === 'es';
  }

  private load(): void {
    this.loading = true;
    this.blogService.getPublished().subscribe({
      next: (posts) => {
        this.posts = posts;
        this.loading = false;
        this.updateSeo();
      },
      error: () => {
        // También en error (p. ej. prerender sin backend): estado vacío y SEO
        // base, sin excepción — patrón hall-of-fame.
        this.loading = false;
        this.updateSeo();
      }
    });
  }

  /** Alterna entre "solo publicados" y "todos" (admin; nunca corre en SSR). */
  toggleAll(): void {
    if (this.showingAll) {
      this.showingAll = false;
      this.load();
      return;
    }
    this.blogService.getAll().subscribe({
      next: (posts) => { this.posts = posts; this.showingAll = true; },
      error: () => { /* sin permisos o backend caído: se queda como está */ }
    });
  }

  // ---------- Helpers de renderizado ----------

  /** Título en el idioma activo, con fallback al otro si está vacío. */
  titleOf(p: BlogPostSummary): string {
    return (this.es ? (p.titleEs || p.titleEn) : (p.titleEn || p.titleEs)) || '';
  }

  excerptOf(p: BlogPostSummary): string {
    return (this.es ? (p.excerptEs || p.excerptEn) : (p.excerptEn || p.excerptEs)) || '';
  }

  coverUrl(p: BlogPostSummary): string {
    return this.blogService.coverUrl(p.id);
  }

  // ---------- SEO ----------

  /**
   * SEO refinado con datos reales, patrón text.component: corre en el callback
   * HTTP de load() (también con lista vacía) y el JSON-LD solo se fija si hay
   * posts — siempre DESPUÉS de seo.update(), que lo limpia.
   */
  private updateSeo(): void {
    const lang = this.transloco.getActiveLang() as Lang;
    const es = lang === 'es';
    const titles = this.posts.map(p => this.titleOf(p)).filter(t => t.length > 0);
    const featured = titles.slice(0, 3).join(' · ');

    const title = es
      ? 'Blog para aprender chino — Consejos, guías e historias | ChineseReads'
      : 'Chinese Learning Blog — Tips, Guides & Stories | ChineseReads';
    const description = es
      ? (featured
          ? `Artículos sobre aprender chino en ChineseReads. Últimos posts: ${featured}.`
          : 'Artículos sobre aprender chino: consejos de estudio, guías HSK, estrategias de '
            + 'lectura e historias de la comunidad de ChineseReads.')
      : (featured
          ? `Articles about learning Chinese on ChineseReads. Latest posts: ${featured}.`
          : 'Articles about learning Chinese: study tips, HSK guides, reading strategies and '
            + 'stories from the ChineseReads community.');

    this.seo.update({ title, description, path: '/blog' }, lang);

    if (this.posts.length > 0) {
      this.seo.setPageJsonLd(this.buildJsonLd(lang, title, description));
    }
  }

  /** schema.org Blog con un BlogPosting resumido por post publicado. */
  private buildJsonLd(lang: Lang, title: string, description: string): object {
    const url = SITE_URL + addLangPrefix('/blog', lang);
    return {
      '@context': 'https://schema.org',
      '@type': 'Blog',
      name: title,
      description,
      url,
      inLanguage: lang,
      publisher: {
        '@type': 'Organization',
        name: 'ChineseReads',
        url: SITE_URL,
        logo: { '@type': 'ImageObject', url: `${SITE_URL}/icon-512.png` }
      },
      blogPost: this.posts
        .filter(p => p.published)
        .map(p => {
          const post: Record<string, unknown> = {
            '@type': 'BlogPosting',
            headline: this.titleOf(p),
            url: SITE_URL + addLangPrefix(`/blog/${p.slug}`, lang)
          };
          const excerpt = this.excerptOf(p);
          if (excerpt) post['description'] = excerpt;
          if (p.publishedOn) post['datePublished'] = p.publishedOn;
          if (p.hasCover) post['image'] = `${SITE_URL}/api/blog/${p.id}/cover`;
          return post;
        })
    };
  }
}
