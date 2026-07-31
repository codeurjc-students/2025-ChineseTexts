import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { SeoService, SITE_URL } from '../../services/seo.service';
import { Lang, addLangPrefix } from '../../i18n/locale.util';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';
import { LocaleNavService } from '../../i18n/locale-nav.service';
import { HALL_OF_FAME_BADGES } from '../../data/hall-of-fame-badges';
import { HallOfFameService, HallOfFameEntry } from '../../services/hall-of-fame.service';

/**
 * Página detalle pública de un miembro del Salón de la Fama
 * (/hall-of-fame/:slug), servida por SSR real como /blog/:slug — al ser ruta
 * parametrizada, el prerender del build no puede enumerarla, así que es inmune
 * por construcción al bug de las fotos congeladas (PR #140): solo hay que
 * añadirla a @ssrPages en Caddy, nunca al script de limpieza.
 *
 * Solo lectura a propósito: el ADMIN edita desde el listado /hall-of-fame
 * (edición in-place ya existente); duplicar aquí el modo edición sería una
 * segunda superficie que mantener sincronizada.
 *
 * Un slug inexistente responde 404 → soft-404 noindex (patrón blog-post).
 * Otros errores NO tocan el SEO por defecto (HOF_MEMBER_DEFAULT ya es
 * indexable): un fallo transitorio jamás debe des-indexar un perfil real.
 */
@Component({
  selector: 'app-hall-of-fame-member',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule, LocalizeLinkPipe],
  templateUrl: './hall-of-fame-member.component.html',
  styleUrl: './hall-of-fame-member.component.scss'
})
export class HallOfFameMemberComponent implements OnInit {

  entry: HallOfFameEntry | null = null;
  loading = true;

  constructor(
    private hallOfFameService: HallOfFameService,
    private activatedRoute: ActivatedRoute,
    private localeNav: LocaleNavService,
    private transloco: TranslocoService,
    private seo: SeoService
  ) {}

  ngOnInit(): void {
    // paramMap (no snapshot): navegar de un perfil a otro reutiliza el
    // componente y debe recargar.
    this.activatedRoute.paramMap.subscribe(params => {
      const slug = params.get('slug');
      if (slug) this.load(slug);
    });
  }

  get es(): boolean {
    return this.transloco.getActiveLang() === 'es';
  }

  private load(slug: string): void {
    this.loading = true;
    this.hallOfFameService.getBySlug(slug).subscribe({
      next: (entry) => {
        this.entry = this.normalize(entry);
        this.loading = false;
        this.updateSeo();
      },
      error: (err) => {
        this.loading = false;
        if (err?.status === 404) {
          this.localeNav.navigate(['/not-found'], { skipLocationChange: true });
        } else {
          console.error('Error loading hall of fame member', err);
        }
      }
    });
  }

  /** Garantiza que las listas nunca sean null para simplificar la plantilla. */
  private normalize(e: HallOfFameEntry): HallOfFameEntry {
    e.badges = e.badges ?? [];
    e.socials = e.socials ?? [];
    return e;
  }

  // ---------- Helpers de renderizado (mismos que el listado) ----------

  get tagline(): string {
    const e = this.entry;
    if (!e) return '';
    return (this.es ? (e.taglineEs || e.taglineEn) : (e.taglineEn || e.taglineEs)) || '';
  }

  get bio(): string {
    const e = this.entry;
    if (!e) return '';
    return (this.es ? (e.bioEs || e.bioEn) : (e.bioEn || e.bioEs)) || '';
  }

  get initials(): string {
    const name = this.entry?.name?.trim();
    if (!name) return '?';
    return name.split(/\s+/).slice(0, 2).map(w => w[0]?.toUpperCase() ?? '').join('');
  }

  badgeIcon(key: string): string {
    return HALL_OF_FAME_BADGES.find(b => b.key === key)?.icon ?? 'bi-award';
  }

  photoUrl(): string {
    const id = this.entry?.id;
    return id ? this.hallOfFameService.photoUrl(id) : '';
  }

  // ---------- SEO ----------

  /** Patrón blog-post: update() y DESPUÉS el JSON-LD, en el callback HTTP. */
  private updateSeo(): void {
    const e = this.entry;
    if (!e) return;
    const lang = this.transloco.getActiveLang() as Lang;

    const title = lang === 'es'
      ? `${e.name} — Salón de la Fama | ChineseReads`
      : `${e.name} — Hall of Fame | ChineseReads`;
    const description = this.tagline || this.truncate(this.bio, 160) ||
      (lang === 'es'
        ? `Perfil de ${e.name} en el Salón de la Fama de ChineseReads.`
        : `${e.name}'s profile in the ChineseReads Hall of Fame.`);
    const image = e.hasPhoto ? `${SITE_URL}/api/hall-of-fame/${e.id}/photo` : undefined;

    this.seo.update({ title, description, path: `/hall-of-fame/${e.slug}`, image }, lang);
    this.seo.setPageJsonLd(this.buildJsonLd(lang, description, image));
  }

  private truncate(value: string, max: number): string {
    if (!value) return '';
    return value.length <= max ? value : value.slice(0, max - 1).trimEnd() + '…';
  }

  /** schema.org ProfilePage con Person (homogéneo con el ItemList del listado). */
  private buildJsonLd(lang: Lang, description: string, image?: string): object {
    const e = this.entry!;
    const url = SITE_URL + addLangPrefix(`/hall-of-fame/${e.slug}`, lang);
    const person: Record<string, unknown> = {
      '@type': 'Person',
      name: e.name,
      description,
      url
    };
    if (image) person['image'] = image;
    const sameAs = e.socials.map(s => s.url).filter(u => !!u);
    if (sameAs.length) person['sameAs'] = sameAs;

    return {
      '@context': 'https://schema.org',
      '@type': 'ProfilePage',
      url,
      mainEntityOfPage: url,
      inLanguage: lang,
      mainEntity: person
    };
  }
}
