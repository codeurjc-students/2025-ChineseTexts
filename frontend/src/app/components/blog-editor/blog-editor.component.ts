import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { QuillModule } from 'ngx-quill';
import type Quill from 'quill';

import { LoginService } from '../../services/login.service';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';
import { LocaleNavService } from '../../i18n/locale-nav.service';
import { BlogService, BlogPostUpsert } from '../../services/blog.service';

/**
 * Editor de posts del blog (/blog-editor y /blog-editor/:id), solo admin.
 *
 * Es la ÚNICA página que importa Quill y se carga con loadComponent (lazy):
 * el editor no pesa ni un byte en el bundle inicial ni en las páginas
 * públicas. La ruta NO está en @ssrPages de Caddy (se sirve como shell SPA) y,
 * como cinturón extra, <quill-editor> solo se instancia en navegador
 * (isBrowser) porque Quill toca document.
 *
 * Protección real en el backend (SecurityConfig: /api/blog/** ADMIN → 403);
 * aquí no hay route-guard, como en admin-tools.
 */
@Component({
  selector: 'app-blog-editor',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslocoModule, LocalizeLinkPipe, QuillModule],
  templateUrl: './blog-editor.component.html',
  styleUrl: './blog-editor.component.scss'
})
export class BlogEditorComponent implements OnInit {

  readonly isBrowser: boolean;

  // Toolbar alineada con la safelist del backend (BlogService.sanitize):
  // h1 reservado al título del post; sin color/fondo (estilos inline no
  // sobreviven al saneado); imagen con handler propio que sube al backend.
  readonly quillModules = {
    toolbar: {
      container: [
        [{ header: [2, 3, false] }],
        ['bold', 'italic', 'underline', 'strike'],
        ['blockquote', 'code-block'],
        [{ list: 'ordered' }, { list: 'bullet' }],
        [{ align: [] }],
        ['link', 'image'],
        ['clean']
      ]
    }
  };

  id: number | null = null;
  titleEn = '';
  titleEs = '';
  excerptEn = '';
  excerptEs = '';
  contentEn = '';
  contentEs = '';
  slug = '';
  published = false;
  hasCover = false;
  coverVersion = 0;

  activeTab: 'en' | 'es' = 'en';
  loading = false;
  saving = false;
  confirmingDelete = false;
  feedback = '';
  feedbackError = false;

  constructor(
    private blogService: BlogService,
    private activatedRoute: ActivatedRoute,
    private localeNav: LocaleNavService,
    public login: LoginService,
    private transloco: TranslocoService,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    const id = this.activatedRoute.snapshot.params['id'];
    if (id) this.load(Number(id));
  }

  get isAdmin(): boolean {
    return this.login.isRoleAdmin();
  }

  private load(id: number): void {
    this.loading = true;
    this.blogService.getById(id).subscribe({
      next: (post) => {
        this.id = post.id;
        this.titleEn = post.titleEn ?? '';
        this.titleEs = post.titleEs ?? '';
        this.excerptEn = post.excerptEn ?? '';
        this.excerptEs = post.excerptEs ?? '';
        this.contentEn = post.contentEn ?? '';
        this.contentEs = post.contentEs ?? '';
        this.slug = post.slug;
        this.published = post.published;
        this.hasCover = post.hasCover;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.showError(this.transloco.translate('blog.editor.feedback.loadError'));
      }
    });
  }

  // ---------- Guardar / borrar ----------

  save(): void {
    const payload: BlogPostUpsert = {
      titleEn: this.titleEn,
      titleEs: this.titleEs,
      excerptEn: this.excerptEn,
      excerptEs: this.excerptEs,
      contentEn: this.contentEn,
      contentEs: this.contentEs,
      published: this.published
    };
    // El slug en blanco al crear se deriva del título en el backend; al editar,
    // omitirlo (undefined) significa "sin cambios".
    if (this.slug.trim()) payload.slug = this.slug.trim();

    this.saving = true;
    const request = this.id != null
      ? this.blogService.update(this.id, payload)
      : this.blogService.create(payload);

    request.subscribe({
      next: (saved) => {
        this.saving = false;
        this.id = saved.id;
        this.slug = saved.slug; // refleja el slug normalizado/derivado
        this.published = saved.published;
        this.showOk(this.transloco.translate('blog.editor.feedback.saved'));
      },
      error: (err) => {
        this.saving = false;
        const apiError = err?.error?.error;
        this.showError(apiError || this.transloco.translate('blog.editor.feedback.saveError'));
      }
    });
  }

  askDelete(): void { this.confirmingDelete = true; }

  cancelDelete(): void { this.confirmingDelete = false; }

  confirmDelete(): void {
    if (this.id == null) return;
    this.blogService.delete(this.id).subscribe({
      next: () => this.localeNav.navigate(['/blog']),
      error: () => this.showError(this.transloco.translate('blog.editor.feedback.deleteError'))
    });
  }

  // ---------- Portada ----------

  onCoverSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file || this.id == null) return;
    this.blogService.uploadCover(this.id, file).subscribe({
      next: () => {
        this.hasCover = true;
        this.coverVersion++;
        this.showOk(this.transloco.translate('blog.editor.feedback.coverUpdated'));
      },
      error: () => this.showError(this.transloco.translate('blog.editor.feedback.coverError'))
    });
  }

  removeCover(): void {
    if (this.id == null) return;
    this.blogService.deleteCover(this.id).subscribe({
      next: () => { this.hasCover = false; this.showOk(this.transloco.translate('blog.editor.feedback.coverRemoved')); },
      error: () => this.showError(this.transloco.translate('blog.editor.feedback.coverError'))
    });
  }

  get coverUrl(): string {
    return this.id != null ? this.blogService.coverUrl(this.id, this.coverVersion) : '';
  }

  // ---------- Imágenes inline (handler de la toolbar de Quill) ----------

  /** Sustituye el handler de imagen: sube al backend e inserta la URL propia. */
  onEditorCreated(editor: Quill): void {
    const toolbar = editor.getModule('toolbar') as { addHandler: (name: string, h: () => void) => void };
    toolbar.addHandler('image', () => this.pickImage(editor));
  }

  private pickImage(editor: Quill): void {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = () => {
      const file = input.files?.[0];
      if (!file) return;
      this.blogService.uploadImage(file, this.id ?? undefined).subscribe({
        next: ({ url }) => {
          const range = editor.getSelection(true);
          editor.insertEmbed(range ? range.index : 0, 'image', url, 'user');
        },
        error: () => this.showError(this.transloco.translate('blog.editor.feedback.imageError'))
      });
    };
    input.click();
  }

  // ---------- Feedback ----------
  private showOk(msg: string): void { this.feedback = msg; this.feedbackError = false; }
  private showError(msg: string): void { this.feedback = msg; this.feedbackError = true; }
}
