import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { LoginService } from '../../services/login.service';
import { SeoService, SITE_URL } from '../../services/seo.service';
import { Lang, addLangPrefix } from '../../i18n/locale.util';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';
import { LocaleNavService } from '../../i18n/locale-nav.service';
import { BlogService, BlogPost } from '../../services/blog.service';

/**
 * Detalle de un post del blog (/blog/:slug), servido por SSR real como
 * /text/:id. El cuerpo llega YA saneado del backend (safelist jsoup) y se
 * renderiza con [innerHTML], que además pasa por el sanitizador por defecto
 * de Angular — defensa doble; nunca bypassSecurityTrustHtml. El wrapper
 * .ql-editor da estilo a las clases ql-* (alineaciones) con el CSS de Quill.
 *
 * Un slug inexistente (o borrador) responde 404 → se navega al NotFound con
 * skipLocationChange (soft-404 noindex, patrón text.component). Otros errores
 * NO tocan el SEO por defecto (BLOG_POST_DEFAULT ya es indexable): un fallo
 * transitorio jamás debe des-indexar un post real.
 */
@Component({
  selector: 'app-blog-post',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule, LocalizeLinkPipe],
  templateUrl: './blog-post.component.html',
  styleUrl: './blog-post.component.scss'
})
export class BlogPostComponent implements OnInit {

  post: BlogPost | null = null;
  loading = true;

  constructor(
    private blogService: BlogService,
    private activatedRoute: ActivatedRoute,
    private localeNav: LocaleNavService,
    public login: LoginService,
    private transloco: TranslocoService,
    private seo: SeoService
  ) {}

  ngOnInit(): void {
    // paramMap (no snapshot): un enlace de /blog/:a a /blog/:b reutiliza el
    // componente y debe recargar.
    this.activatedRoute.paramMap.subscribe(params => {
      const slug = params.get('slug');
      if (slug) this.load(slug);
    });
  }

  get isAdmin(): boolean {
    return this.login.isRoleAdmin();
  }

  get es(): boolean {
    return this.transloco.getActiveLang() === 'es';
  }

  private load(slug: string): void {
    this.loading = true;
    this.blogService.getBySlug(slug).subscribe({
      next: (post) => {
        this.post = post;
        this.loading = false;
        this.updateSeo();
      },
      error: (err) => {
        this.loading = false;
        if (err?.status === 404) {
          this.localeNav.navigate(['/not-found'], { skipLocationChange: true });
        } else {
          console.error('Error loading blog post', err);
        }
      }
    });
  }

  // ---------- Helpers de renderizado ----------

  get title(): string {
    const p = this.post;
    if (!p) return '';
    return (this.es ? (p.titleEs || p.titleEn) : (p.titleEn || p.titleEs)) || '';
  }

  get excerpt(): string {
    const p = this.post;
    if (!p) return '';
    return (this.es ? (p.excerptEs || p.excerptEn) : (p.excerptEn || p.excerptEs)) || '';
  }

  /**
   * Cuerpo en el idioma activo con fallback (HTML saneado en el backend).
   * El replace de &nbsp; cubre los posts guardados ANTES de que el saneado
   * normalizara los espacios del exportador de Quill 2 (que convertía todos
   * los espacios en nbsp e impedía partir las líneas por palabras).
   */
  get contentHtml(): string {
    const p = this.post;
    if (!p) return '';
    const html = (this.es ? (p.contentEs || p.contentEn) : (p.contentEn || p.contentEs)) || '';
    return html.replace(/&nbsp;/g, ' ');
  }

  get coverUrl(): string {
    return this.post ? this.blogService.coverUrl(this.post.id) : '';
  }

  // ---------- SEO ----------

  /** Patrón text.component: update() y DESPUÉS el JSON-LD, en el callback HTTP. */
  private updateSeo(): void {
    const p = this.post;
    if (!p) return;
    const lang = this.transloco.getActiveLang() as Lang;

    const title = `${this.title} | ChineseReads`;
    const description = this.excerpt ||
      (lang === 'es'
        ? 'Un artículo sobre aprender chino en el blog de ChineseReads.'
        : 'An article about learning Chinese on the ChineseReads blog.');
    const image = p.hasCover ? `${SITE_URL}/api/blog/${p.id}/cover` : undefined;

    this.seo.update({
      title,
      description,
      path: `/blog/${p.slug}`,
      image,
      type: 'article',
      publishedTime: p.publishedOn ?? undefined,
      modifiedTime: p.updatedAt ?? undefined
    }, lang);

    this.seo.setPageJsonLd(this.buildJsonLd(lang, title, description, image));
  }

  /** schema.org BlogPosting (homogéneo con el Article de /text/:id). */
  private buildJsonLd(lang: Lang, title: string, description: string, image?: string): object {
    const p = this.post!;
    const url = SITE_URL + addLangPrefix(`/blog/${p.slug}`, lang);
    const data: Record<string, unknown> = {
      '@context': 'https://schema.org',
      '@type': 'BlogPosting',
      headline: this.title,
      name: title,
      description,
      url,
      mainEntityOfPage: url,
      inLanguage: lang,
      author: { '@type': 'Person', name: 'José Víctor García Llorente' },
      publisher: {
        '@type': 'Organization',
        name: 'ChineseReads',
        url: SITE_URL,
        logo: { '@type': 'ImageObject', url: `${SITE_URL}/icon-512.png` }
      }
    };
    if (image) data['image'] = image;
    if (p.publishedOn) data['datePublished'] = p.publishedOn;
    if (p.updatedAt) data['dateModified'] = p.updatedAt;
    return data;
  }
}
