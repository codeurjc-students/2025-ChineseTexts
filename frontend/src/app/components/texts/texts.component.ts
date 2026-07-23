import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { combineLatest } from 'rxjs';
import { TextsService, TextItem, TextMetadataUpdate, TEXT_TOPICS, MAX_TOPICS_PER_TEXT } from '../../services/texts.service';
import { LoginService } from '../../services/login.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';

@Component({
  selector: 'app-texts',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslocoModule, LocalizeLinkPipe],
  templateUrl: './texts.component.html',
  styleUrl: './texts.component.scss'
})
export class TextsComponent implements OnInit {

  texts: TextItem[] = [];
  page = 0;
  size = 2;
  hasMore: boolean = true;
  currentLevel: string | null = null;

  // Filtro por tema: se lee de ?topic=... para que un filtro sea compartible
  // por URL. El SEO no cambia: los canónicos ignoran los query params.
  readonly topics = TEXT_TOPICS;
  currentTopic: string | null = null;

  showDeleteModal = false;
  textToDelete: TextItem | null = null;

  // Edición admin de metadatos (títulos, descripciones, nivel, temas). El
  // contenido chino y las traducciones NO se editan aquí: cambiarlos exige
  // revalidar las frases alineadas, así que siguen el camino borrar + resubir.
  showEditModal = false;
  textToEdit: TextItem | null = null;
  editTitleEnglish = '';
  editTitleSpanish = '';
  editEnglishDescription = '';
  editSpanishDescription = '';
  editLevel = 'HSK1';
  editTopics: string[] = [];
  editImageFile: File | null = null;
  editImagePreview: string | null = null;
  editSaving = false;
  editError = '';
  // Rompe la caché del navegador para la portada de un texto cuya imagen se
  // acaba de cambiar (el src es estable por id, así que sin esto seguiría
  // mostrándose la imagen antigua hasta un refresco duro).
  private imageBust: Record<number, number> = {};
  readonly levels = ['HSK1', 'HSK2', 'HSK3', 'HSK4', 'HSK5', 'HSK6'];
  readonly maxTopics = MAX_TOPICS_PER_TEXT;

  constructor(
    private textsService: TextsService,
    private route: ActivatedRoute,
    private router: Router,
    private loginService: LoginService,
    private transloco: TranslocoService
  ) {}

  get isAdmin(): boolean {
    return this.loginService.isRoleAdmin();
  }

  /** Card title in the active UI language (falls back to the other language). */
  displayTitle(t: TextItem): string {
    const es = this.transloco.getActiveLang() === 'es';
    return (es ? t.titleSpanish : t.titleEnglish) || t.titleEnglish || t.titleSpanish || '';
  }

  /** Card description in the active UI language (falls back to the other language). */
  displayDescription(t: TextItem): string {
    const es = this.transloco.getActiveLang() === 'es';
    return (es ? t.spanishDescription : t.englishDescription) || t.englishDescription || t.spanishDescription || '';
  }

  /** Topics of a card in the canonical TEXT_TOPICS display order. */
  displayTopics(t: TextItem): string[] {
    if (!t.topics?.length) return [];
    return this.topics.filter(key => t.topics!.includes(key));
  }

  /** Cover image URL, with a cache-buster if the image was edited this session. */
  imageSrc(t: TextItem): string {
    const bust = this.imageBust[t.id];
    return `/api/texts/${t.id}/image${bust ? '?v=' + bust : ''}`;
  }

  ngOnInit(): void {
    // Detecta cambios en la URL: /texts/HSK3 (nivel) y ?topic=food (tema)
    combineLatest([this.route.paramMap, this.route.queryParamMap]).subscribe(([params, query]) => {
      this.currentLevel = params.get('level');
      const topic = query.get('topic');
      this.currentTopic = topic && (this.topics as readonly string[]).includes(topic) ? topic : null;

      // Reiniciar estado
      this.page = 0;
      this.texts = [];
      this.hasMore = true;

      if (this.currentLevel) {
        this.loadTextsByLevel(this.currentLevel);
      } else {
        this.loadTexts();
      }
    });
  }

  /** Cambia el filtro de tema actualizando la URL (null = todos). */
  selectTopic(topic: string | null): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { topic },
      queryParamsHandling: 'merge'
    });
  }

  // Carga general sin filtro de nivel (el tema aplica si está activo)
  loadTexts(): void {
    this.textsService.getTexts(this.page, this.size, this.currentTopic ?? undefined).subscribe({
      next: (data) => {
        this.texts = [...this.texts, ...data];

        if (data.length < this.size) {
          this.hasMore = false;
        }
      },
      error: (err) => console.error('Error loading texts:', err)
    });
  }

  // Carga filtrada por nivel (y tema si está activo)
  loadTextsByLevel(level: string): void {
    this.textsService.getTextsByLevel(level, this.page, this.size, this.currentTopic ?? undefined).subscribe({
      next: (data) => {
        this.texts = [...this.texts, ...data];

        if (data.length < this.size) {
          this.hasMore = false;
        }
      },
      error: (err) => console.error('Error loading texts by level:', err)
    });
  }

  // Load more respeta si hay nivel o no
  loadMore(): void {
    this.page++;

    if (this.currentLevel) {
      this.loadTextsByLevel(this.currentLevel);
    } else {
      this.loadTexts();
    }
  }

  toggleLike(text: TextItem, event: Event): void {
    event.stopPropagation();
    text.liked = !text.liked;
  }

  openDeleteModal(text: TextItem, event: Event): void {
    event.stopPropagation();
    this.textToDelete = text;
    this.showDeleteModal = true;
  }

  confirmDeleteText(): void {
    if (!this.textToDelete) return;
    this.textsService.deleteText(this.textToDelete.id).subscribe({
      next: () => {
        this.texts = this.texts.filter(t => t.id !== this.textToDelete!.id);
        this.cancelDelete();
      },
      error: (err) => console.error('Error deleting text', err)
    });
  }

  cancelDelete(): void {
    this.showDeleteModal = false;
    this.textToDelete = null;
  }

  openEditModal(text: TextItem, event: Event): void {
    event.stopPropagation();
    this.textToEdit = text;
    this.editTitleEnglish = text.titleEnglish || '';
    this.editTitleSpanish = text.titleSpanish || '';
    this.editEnglishDescription = text.englishDescription || '';
    this.editSpanishDescription = text.spanishDescription || '';
    this.editLevel = text.level || 'HSK1';
    this.editTopics = [...(text.topics || [])];
    this.editImageFile = null;
    this.editImagePreview = null;
    this.editError = '';
    this.editSaving = false;
    this.showEditModal = true;
  }

  onEditImageChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.editImageFile = input.files[0];
    const reader = new FileReader();
    reader.onload = () => this.editImagePreview = reader.result as string;
    reader.readAsDataURL(this.editImageFile);
  }

  toggleEditTopic(key: string): void {
    if (this.editTopics.includes(key)) {
      this.editTopics = this.editTopics.filter(t => t !== key);
    } else if (this.editTopics.length < this.maxTopics) {
      this.editTopics = [...this.editTopics, key];
    }
  }

  saveEdit(): void {
    if (!this.textToEdit || this.editSaving) return;
    this.editSaving = true;
    this.editError = '';

    const patch: TextMetadataUpdate = {
      titleEnglish: this.editTitleEnglish.trim(),
      titleSpanish: this.editTitleSpanish.trim(),
      englishDescription: this.editEnglishDescription,
      spanishDescription: this.editSpanishDescription,
      level: this.editLevel,
      topics: this.editTopics
    };

    this.textsService.updateTextMetadata(this.textToEdit.id, patch).subscribe({
      next: (updated) => {
        this.texts = this.texts.map(t => t.id === updated.id ? { ...t, ...updated } : t);
        // Los metadatos ya están guardados; si además hay imagen nueva, se sube
        // ahora. Si fallara, el modal queda abierto mostrando solo ese error.
        if (this.editImageFile) {
          this.textsService.updateTextImage(updated.id, this.editImageFile).subscribe({
            next: () => {
              this.imageBust[updated.id] = Date.now();
              this.cancelEdit();
            },
            error: () => {
              this.editSaving = false;
              this.editError = this.transloco.translate('texts.edit.errors.imageFailed');
            }
          });
        } else {
          this.cancelEdit();
        }
      },
      error: (err) => {
        this.editSaving = false;
        this.editError = this.transloco.translate(
          err.status === 409 ? 'texts.edit.errors.titleExists' : 'texts.edit.errors.saveFailed');
      }
    });
  }

  cancelEdit(): void {
    this.showEditModal = false;
    this.textToEdit = null;
    this.editImageFile = null;
    this.editImagePreview = null;
    this.editSaving = false;
    this.editError = '';
  }
}
