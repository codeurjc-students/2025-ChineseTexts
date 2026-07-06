import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { LoginService } from '../../services/login.service';
import {
  FounderService, FounderProfile, FounderSection, FounderItem, FounderSocial
} from '../../services/founder.service';

/**
 * Página personal del creador (/founder), estilo LinkedIn.
 *
 * - Cualquier visitante ve el perfil (sólo lectura), cargado desde la API.
 * - Sólo el ADMIN ve el botón "Edit" y puede añadir/editar/borrar secciones,
 *   ítems y enlaces, subir su foto y logos, y reordenar el contenido — todo
 *   desde la web, sin tocar código ni desplegar.
 *
 * El contenido vive en la base de datos (entidades FounderProfile/Section/Item),
 * por lo que actualizar el CV no requiere ningún cambio de código.
 */
@Component({
  selector: 'app-founder',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslocoModule],
  templateUrl: './founder.component.html',
  styleUrl: './founder.component.scss'
})
export class FounderComponent implements OnInit {

  profile: FounderProfile | null = null;
  loading = true;
  editing = false;
  feedback = '';
  feedbackError = false;

  /** Tipos de sección disponibles al crear una nueva. */
  readonly sectionTypes = ['EXPERIENCE', 'EDUCATION', 'PROJECTS', 'SKILLS', 'CUSTOM'];

  // Versiones para forzar recarga de imágenes tras subir una nueva (cache-busting).
  photoVersion = 0;
  private logoVersions: { [id: number]: number } = {};

  constructor(
    private founderService: FounderService,
    public login: LoginService,
    private transloco: TranslocoService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    // El contenido es dinámico: sólo cargamos en el navegador (evita depender
    // del backend durante el prerender; el SEO ya se sirve por meta estáticas).
    if (isPlatformBrowser(this.platformId)) {
      this.load();
    } else {
      this.loading = false;
    }
  }

  get isAdmin(): boolean {
    return this.login.isRoleAdmin();
  }

  /** Iniciales para el avatar por defecto cuando no hay foto. */
  get initials(): string {
    const name = this.profile?.name?.trim();
    if (!name) return '?';
    return name.split(/\s+/).slice(0, 2).map(w => w[0]?.toUpperCase() ?? '').join('');
  }

  private load(): void {
    this.loading = true;
    this.founderService.getProfile().subscribe({
      next: (p) => { this.profile = this.normalize(p); this.loading = false; },
      error: () => { this.loading = false; this.showError(this.transloco.translate('founder.feedback.loadError')); }
    });
  }

  /** Garantiza que las listas nunca sean null para simplificar la plantilla. */
  private normalize(p: FounderProfile): FounderProfile {
    p.socials = p.socials ?? [];
    p.sections = (p.sections ?? []).map(s => ({ ...s, items: s.items ?? [] }));
    return p;
  }

  toggleEdit(): void {
    this.editing = !this.editing;
    this.feedback = '';
  }

  // ---------- Imágenes ----------
  photoUrl(): string {
    return this.founderService.photoUrl(this.photoVersion);
  }

  itemLogoUrl(item: FounderItem): string {
    return this.founderService.itemLogoUrl(item.id!, this.logoVersions[item.id!] ?? 0);
  }

  onPhotoSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file || !this.profile) return;
    this.founderService.uploadPhoto(file).subscribe({
      next: () => { this.profile!.hasPhoto = true; this.photoVersion++; this.showOk(this.transloco.translate('founder.feedback.photoUpdated')); },
      error: () => this.showError(this.transloco.translate('founder.feedback.photoUploadError'))
    });
  }

  removePhoto(): void {
    if (!this.profile) return;
    this.founderService.deletePhoto().subscribe({
      next: () => { this.profile!.hasPhoto = false; this.showOk(this.transloco.translate('founder.feedback.photoRemoved')); },
      error: () => this.showError(this.transloco.translate('founder.feedback.photoRemoveError'))
    });
  }

  onItemLogoSelected(item: FounderItem, event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file || !item.id) return;
    this.founderService.uploadItemLogo(item.id, file).subscribe({
      next: () => { item.hasLogo = true; this.logoVersions[item.id!] = (this.logoVersions[item.id!] ?? 0) + 1; this.showOk(this.transloco.translate('founder.feedback.logoUpdated')); },
      error: () => this.showError(this.transloco.translate('founder.feedback.logoUploadError'))
    });
  }

  removeItemLogo(item: FounderItem): void {
    if (!item.id) return;
    this.founderService.deleteItemLogo(item.id).subscribe({
      next: () => { item.hasLogo = false; this.showOk(this.transloco.translate('founder.feedback.logoRemoved')); },
      error: () => this.showError(this.transloco.translate('founder.feedback.logoRemoveError'))
    });
  }

  // ---------- Perfil ----------
  saveProfile(): void {
    if (!this.profile) return;
    this.founderService.updateProfile(this.profile).subscribe({
      next: () => this.showOk(this.transloco.translate('founder.feedback.profileSaved')),
      error: () => this.showError(this.transloco.translate('founder.feedback.profileSaveError'))
    });
  }

  // ---------- Enlaces / redes ----------
  addSocial(): void {
    if (!this.profile) return;
    const draft: FounderSocial = { label: this.transloco.translate('founder.defaults.newLink'), icon: 'bi-link-45deg', url: '', displayOrder: this.profile.socials.length };
    this.founderService.addSocial(draft).subscribe({
      next: (created) => { this.profile!.socials.push(created); this.showOk(this.transloco.translate('founder.feedback.linkAdded')); },
      error: () => this.showError(this.transloco.translate('founder.feedback.linkAddError'))
    });
  }

  saveSocial(social: FounderSocial): void {
    this.founderService.updateSocial(social.id!, social).subscribe({
      next: () => this.showOk(this.transloco.translate('founder.feedback.linkSaved')),
      error: () => this.showError(this.transloco.translate('founder.feedback.linkSaveError'))
    });
  }

  deleteSocial(social: FounderSocial): void {
    this.founderService.deleteSocial(social.id!).subscribe({
      next: () => { this.profile!.socials = this.profile!.socials.filter(s => s !== social); this.showOk(this.transloco.translate('founder.feedback.linkRemoved')); },
      error: () => this.showError(this.transloco.translate('founder.feedback.linkRemoveError'))
    });
  }

  // ---------- Secciones ----------
  addSection(): void {
    if (!this.profile) return;
    const draft: FounderSection = { title: this.transloco.translate('founder.defaults.newSection'), type: 'CUSTOM', displayOrder: this.profile.sections.length, items: [] };
    this.founderService.addSection(draft).subscribe({
      next: (created) => { created.items = created.items ?? []; this.profile!.sections.push(created); this.showOk(this.transloco.translate('founder.feedback.sectionAdded')); },
      error: () => this.showError(this.transloco.translate('founder.feedback.sectionAddError'))
    });
  }

  saveSection(section: FounderSection): void {
    this.founderService.updateSection(section.id!, section).subscribe({
      next: () => this.showOk(this.transloco.translate('founder.feedback.sectionSaved')),
      error: () => this.showError(this.transloco.translate('founder.feedback.sectionSaveError'))
    });
  }

  deleteSection(section: FounderSection): void {
    this.founderService.deleteSection(section.id!).subscribe({
      next: () => { this.profile!.sections = this.profile!.sections.filter(s => s !== section); this.showOk(this.transloco.translate('founder.feedback.sectionRemoved')); },
      error: () => this.showError(this.transloco.translate('founder.feedback.sectionRemoveError'))
    });
  }

  // ---------- Ítems ----------
  addItem(section: FounderSection): void {
    const draft: FounderItem = {
      heading: this.transloco.translate('founder.defaults.newEntry'), subheading: '', period: '', location: '',
      description: '', linkUrl: '', linkLabel: '', displayOrder: section.items.length, hasLogo: false
    };
    this.founderService.addItem(section.id!, draft).subscribe({
      next: (created) => { section.items.push(created); this.showOk(this.transloco.translate('founder.feedback.entryAdded')); },
      error: () => this.showError(this.transloco.translate('founder.feedback.entryAddError'))
    });
  }

  saveItem(item: FounderItem): void {
    this.founderService.updateItem(item.id!, item).subscribe({
      next: () => this.showOk(this.transloco.translate('founder.feedback.entrySaved')),
      error: () => this.showError(this.transloco.translate('founder.feedback.entrySaveError'))
    });
  }

  deleteItem(section: FounderSection, item: FounderItem): void {
    this.founderService.deleteItem(item.id!).subscribe({
      next: () => { section.items = section.items.filter(i => i !== item); this.showOk(this.transloco.translate('founder.feedback.entryRemoved')); },
      error: () => this.showError(this.transloco.translate('founder.feedback.entryRemoveError'))
    });
  }

  // ---------- Reordenar (intercambia displayOrder y persiste ambos) ----------
  moveSection(index: number, dir: -1 | 1): void {
    if (!this.profile) return;
    this.swap(this.profile.sections, index, dir, (a, b) => {
      this.founderService.updateSection(a.id!, a).subscribe();
      this.founderService.updateSection(b.id!, b).subscribe();
    });
  }

  moveItem(section: FounderSection, index: number, dir: -1 | 1): void {
    this.swap(section.items, index, dir, (a, b) => {
      this.founderService.updateItem(a.id!, a).subscribe();
      this.founderService.updateItem(b.id!, b).subscribe();
    });
  }

  /** Intercambia dos elementos vecinos de una lista y sus displayOrder. */
  private swap<T extends { displayOrder: number }>(list: T[], index: number, dir: -1 | 1, persist: (a: T, b: T) => void): void {
    const target = index + dir;
    if (target < 0 || target >= list.length) return;
    const a = list[index];
    const b = list[target];
    const tmp = a.displayOrder; a.displayOrder = b.displayOrder; b.displayOrder = tmp;
    list[index] = b; list[target] = a;
    persist(a, b);
  }

  // ---------- Helpers de renderizado ----------
  isSkills(section: FounderSection): boolean {
    return section.type === 'SKILLS';
  }

  hasAnyContent(): boolean {
    if (!this.profile) return false;
    return !!(this.profile.name || this.profile.summary || this.profile.sections.length || this.profile.socials.length);
  }

  // ---------- Feedback ----------
  private showOk(msg: string): void { this.feedback = msg; this.feedbackError = false; this.autoClear(); }
  private showError(msg: string): void { this.feedback = msg; this.feedbackError = true; this.autoClear(); }
  private autoClear(): void { /* el mensaje permanece hasta la siguiente acción; simple y sin timers en SSR */ }
}
