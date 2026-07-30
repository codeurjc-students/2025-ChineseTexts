import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { LoginService } from '../../services/login.service';
import { SeoService, SITE_URL } from '../../services/seo.service';
import { Lang, addLangPrefix } from '../../i18n/locale.util';
import { HALL_OF_FAME_BADGES } from '../../data/hall-of-fame-badges';
import {
  SOCIAL_LINK_PRESETS, SocialLinkPreset, detectSocialPreset
} from '../../data/social-link-presets';
import {
  HallOfFameService, HallOfFameEntry, HallOfFameSocial
} from '../../services/hall-of-fame.service';

/** Icono por defecto de un link recién creado sin red elegida. */
const GENERIC_LINK_ICON = 'bi-link-45deg';

/**
 * Salón de la Fama (/hall-of-fame): tarjetas públicas de los influencers y
 * colaboradores (foto, bio bilingüe, redes, código de descuento y badges).
 *
 * - Cualquier visitante ve las tarjetas (sólo lectura), cargadas desde la API.
 * - Sólo el ADMIN ve el botón "Edit" y puede crear/editar/borrar miembros,
 *   subir fotos, conceder badges y reordenar — patrón de la página /founder.
 *
 * A diferencia de /founder, esta página se sirve por SSR real (frontend-ssr
 * vía Caddy, como /text/*): la carga NO se limita al navegador, de modo que
 * el HTML servido ya lleva el contenido y el SEO refinado con datos reales.
 * El JSON-LD se fija tras la respuesta HTTP (posterior al update() central de
 * AppComponent en NavigationEnd, que lo limpiaría — mismo mecanismo que
 * text.component).
 */
@Component({
  selector: 'app-hall-of-fame',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslocoModule],
  templateUrl: './hall-of-fame.component.html',
  styleUrl: './hall-of-fame.component.scss'
})
export class HallOfFameComponent implements OnInit {

  entries: HallOfFameEntry[] = [];
  loading = true;
  editing = false;
  feedback = '';
  feedbackError = false;

  /** Catálogo cerrado de badges (chips + picker del admin). */
  readonly badgeCatalog = HALL_OF_FAME_BADGES;

  /** Redes conocidas para el picker de links (icono+label prefijados). */
  readonly socialPresets = SOCIAL_LINK_PRESETS;

  /** Entrada con el picker de redes abierto (null = ninguno). */
  socialPickerFor: number | null = null;

  /** Entrada con la confirmación de borrado abierta (null = ninguna). */
  confirmingId: number | null = null;

  // Versiones para forzar recarga de fotos tras subir una nueva (cache-busting).
  private photoVersions: { [id: number]: number } = {};

  constructor(
    private hallOfFameService: HallOfFameService,
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
    this.hallOfFameService.getAll().subscribe({
      next: (entries) => {
        this.entries = entries.map(e => this.normalize(e));
        this.loading = false;
        this.updateSeo();
      },
      error: () => {
        this.loading = false;
        this.updateSeo();
        this.showError(this.transloco.translate('hallOfFame.feedback.loadError'));
      }
    });
  }

  /** Garantiza que las listas nunca sean null para simplificar la plantilla. */
  private normalize(e: HallOfFameEntry): HallOfFameEntry {
    e.badges = e.badges ?? [];
    e.socials = e.socials ?? [];
    return e;
  }

  toggleEdit(): void {
    this.editing = !this.editing;
    this.feedback = '';
    this.confirmingId = null;
    this.socialPickerFor = null;
  }

  // ---------- Helpers de renderizado ----------

  /** Bio en el idioma activo, con fallback a la otra si está vacía. */
  bioOf(e: HallOfFameEntry): string {
    return (this.es ? (e.bioEs || e.bioEn) : (e.bioEn || e.bioEs)) || '';
  }

  /** Iniciales para el avatar por defecto cuando no hay foto. */
  initialsOf(e: HallOfFameEntry): string {
    const name = e.name?.trim();
    if (!name) return '?';
    return name.split(/\s+/).slice(0, 2).map(w => w[0]?.toUpperCase() ?? '').join('');
  }

  badgeIcon(key: string): string {
    return this.badgeCatalog.find(b => b.key === key)?.icon ?? 'bi-award';
  }

  photoUrl(e: HallOfFameEntry): string {
    return this.hallOfFameService.photoUrl(e.id!, this.photoVersions[e.id!] ?? 0);
  }

  // ---------- Entradas ----------

  addEntry(): void {
    this.hallOfFameService.create({ name: this.transloco.translate('hallOfFame.defaults.newMember') }).subscribe({
      next: (created) => { this.entries.push(this.normalize(created)); this.showOk(this.transloco.translate('hallOfFame.feedback.entryAdded')); },
      error: () => this.showError(this.transloco.translate('hallOfFame.feedback.entryAddError'))
    });
  }

  saveEntry(e: HallOfFameEntry): void {
    this.hallOfFameService.update(e.id!, {
      name: e.name, slug: e.slug, tagline: e.tagline, bioEn: e.bioEn,
      bioEs: e.bioEs, discountCode: e.discountCode, badges: e.badges
    }).subscribe({
      next: (saved) => {
        // El backend puede normalizar slug y badges: reflejamos su versión.
        e.slug = saved.slug;
        e.badges = saved.badges ?? [];
        this.showOk(this.transloco.translate('hallOfFame.feedback.entrySaved'));
      },
      error: () => this.showError(this.transloco.translate('hallOfFame.feedback.entrySaveError'))
    });
  }

  askDelete(e: HallOfFameEntry): void { this.confirmingId = e.id ?? null; }

  cancelDelete(): void { this.confirmingId = null; }

  confirmDelete(e: HallOfFameEntry): void {
    this.hallOfFameService.delete(e.id!).subscribe({
      next: () => {
        this.entries = this.entries.filter(x => x !== e);
        this.confirmingId = null;
        this.showOk(this.transloco.translate('hallOfFame.feedback.entryRemoved'));
      },
      error: () => this.showError(this.transloco.translate('hallOfFame.feedback.entryRemoveError'))
    });
  }

  /** Intercambia la entrada con su vecina y persiste ambos displayOrder. */
  move(index: number, dir: -1 | 1): void {
    const target = index + dir;
    if (target < 0 || target >= this.entries.length) return;
    const a = this.entries[index];
    const b = this.entries[target];
    const tmp = a.displayOrder; a.displayOrder = b.displayOrder; b.displayOrder = tmp;
    this.entries[index] = b; this.entries[target] = a;
    this.hallOfFameService.update(a.id!, { displayOrder: a.displayOrder }).subscribe();
    this.hallOfFameService.update(b.id!, { displayOrder: b.displayOrder }).subscribe();
  }

  // ---------- Badges ----------

  toggleBadge(e: HallOfFameEntry, key: string): void {
    e.badges = e.badges.includes(key)
      ? e.badges.filter(b => b !== key)
      : [...e.badges, key];
  }

  // ---------- Foto ----------

  onPhotoSelected(e: HallOfFameEntry, event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file || !e.id) return;
    this.hallOfFameService.uploadPhoto(e.id, file).subscribe({
      next: () => {
        e.hasPhoto = true;
        this.photoVersions[e.id!] = (this.photoVersions[e.id!] ?? 0) + 1;
        this.showOk(this.transloco.translate('hallOfFame.feedback.photoUpdated'));
      },
      error: () => this.showError(this.transloco.translate('hallOfFame.feedback.photoUploadError'))
    });
  }

  removePhoto(e: HallOfFameEntry): void {
    if (!e.id) return;
    this.hallOfFameService.deletePhoto(e.id).subscribe({
      next: () => { e.hasPhoto = false; this.showOk(this.transloco.translate('hallOfFame.feedback.photoRemoved')); },
      error: () => this.showError(this.transloco.translate('hallOfFame.feedback.photoRemoveError'))
    });
  }

  // ---------- Redes sociales ----------

  toggleSocialPicker(e: HallOfFameEntry): void {
    this.socialPickerFor = this.socialPickerFor === e.id ? null : (e.id ?? null);
  }

  /** Crea un link: con preset (red conocida, icono+label prefijados) o genérico. */
  addSocial(e: HallOfFameEntry, preset?: SocialLinkPreset): void {
    const draft: HallOfFameSocial = {
      label: preset?.label ?? this.transloco.translate('hallOfFame.defaults.newLink'),
      icon: preset?.icon ?? GENERIC_LINK_ICON,
      url: '', displayOrder: e.socials.length
    };
    this.hallOfFameService.addSocial(e.id!, draft).subscribe({
      next: (created) => {
        e.socials.push(created);
        this.socialPickerFor = null;
        this.showOk(this.transloco.translate('hallOfFame.feedback.linkAdded'));
      },
      error: () => this.showError(this.transloco.translate('hallOfFame.feedback.linkAddError'))
    });
  }

  /**
   * Autodetecta la red al teclear/pegar la URL, sólo mientras el icono siga
   * siendo el genérico — una elección explícita nunca se pisa. No persiste
   * nada: como el resto de la fila, se guarda con su botón de guardar.
   */
  onSocialUrlChange(s: HallOfFameSocial): void {
    if (s.icon !== GENERIC_LINK_ICON) return;
    const preset = detectSocialPreset(s.url);
    if (preset) {
      s.icon = preset.icon;
      s.label = preset.label;
    }
  }

  saveSocial(social: HallOfFameSocial): void {
    this.hallOfFameService.updateSocial(social.id!, social).subscribe({
      next: () => this.showOk(this.transloco.translate('hallOfFame.feedback.linkSaved')),
      error: () => this.showError(this.transloco.translate('hallOfFame.feedback.linkSaveError'))
    });
  }

  deleteSocial(e: HallOfFameEntry, social: HallOfFameSocial): void {
    this.hallOfFameService.deleteSocial(social.id!).subscribe({
      next: () => { e.socials = e.socials.filter(s => s !== social); this.showOk(this.transloco.translate('hallOfFame.feedback.linkRemoved')); },
      error: () => this.showError(this.transloco.translate('hallOfFame.feedback.linkRemoveError'))
    });
  }

  // ---------- SEO ----------

  /**
   * SEO refinado con datos reales, patrón text.component: corre en el callback
   * HTTP de load() (también con lista vacía, para no depender del backend), y
   * el JSON-LD sólo se fija si hay entradas (honesto y mínimo).
   */
  private updateSeo(): void {
    const lang = this.transloco.getActiveLang() as Lang;
    const es = lang === 'es';
    const names = this.entries.map(e => (e.name || '').trim()).filter(n => n.length > 0);
    const featured = names.slice(0, 4).join(', ');

    const title = es
      ? 'Salón de la Fama — Creadores y colaboradores de ChineseReads'
      : 'Hall of Fame — Creators & Partners of ChineseReads';
    const description = es
      ? (featured
          ? `Conoce a ${featured} y al resto de creadores, profesores y colaboradores que ayudan a `
            + 'aprender chino con ChineseReads, con sus códigos de descuento exclusivos.'
          : 'Conoce a los creadores, profesores y colaboradores que ayudan a aprender chino con '
            + 'ChineseReads: quiénes son, qué hacen y sus códigos de descuento exclusivos.')
      : (featured
          ? `Meet ${featured} and the other creators, teachers and partners helping people learn `
            + 'Chinese with ChineseReads, with their exclusive discount codes.'
          : 'Meet the creators, teachers and partners helping people learn Chinese with '
            + 'ChineseReads: who they are, what they do, and their exclusive discount codes.');

    this.seo.update({ title, description, path: '/hall-of-fame' }, lang);

    if (this.entries.length > 0) {
      this.seo.setPageJsonLd(this.buildJsonLd(lang, title, description));
    }
  }

  /** schema.org CollectionPage + ItemList de Person (una por miembro). */
  private buildJsonLd(lang: Lang, title: string, description: string): object {
    const url = SITE_URL + addLangPrefix('/hall-of-fame', lang);
    return {
      '@context': 'https://schema.org',
      '@type': 'CollectionPage',
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
      mainEntity: {
        '@type': 'ItemList',
        numberOfItems: this.entries.length,
        itemListElement: this.entries.map((e, i) => {
          const person: Record<string, unknown> = {
            '@type': 'Person',
            name: e.name
          };
          if (e.tagline) person['description'] = e.tagline;
          if (e.hasPhoto && e.id) person['image'] = `${SITE_URL}/api/hall-of-fame/${e.id}/photo`;
          const sameAs = e.socials.map(s => s.url).filter(u => !!u);
          if (sameAs.length) person['sameAs'] = sameAs;
          return { '@type': 'ListItem', position: i + 1, item: person };
        })
      }
    };
  }

  // ---------- Feedback ----------
  private showOk(msg: string): void { this.feedback = msg; this.feedbackError = false; }
  private showError(msg: string): void { this.feedback = msg; this.feedbackError = true; }
}
