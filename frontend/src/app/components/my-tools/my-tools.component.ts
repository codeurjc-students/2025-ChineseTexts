import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { LoginService } from '../../services/login.service';
import { MyTextsService, UserTextSummary } from '../../services/my-texts.service';
import { LocaleNavService } from '../../i18n/locale-nav.service';

/**
 * "My Tools" — the user-facing counterpart of Admin Tools. A registered user turns
 * Chinese text into their own PRIVATE graded reader. Flow: paste text OR extract it
 * from a photo (OCR) into an editable box, review/fix it, then create the reader.
 * Generating always happens from the (possibly edited) text box, so the AI only ever
 * works on text the user confirmed — better quality and less wasted spend.
 */
@Component({
  selector: 'app-my-tools',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoModule],
  templateUrl: './my-tools.component.html',
  styleUrl: './my-tools.component.scss'
})
export class MyToolsComponent implements OnInit {

  texts: UserTextSummary[] = [];
  loadingList = false;

  pasteText = '';
  selectedFile: File | null = null;
  extracting = false;
  creating = false;

  message = '';
  messageType: 'success' | 'error' | 'info' | '' = '';

  confirmingDeleteId: number | null = null;

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private loginService: LoginService,
    private myTexts: MyTextsService,
    private router: Router,
    private transloco: TranslocoService,
    private localeNav: LocaleNavService
  ) {}

  ngOnInit(): void {
    // SEO for this private (noindex) page is applied globally by AppComponent
    // via resolveSeo('/my-tools', lang), so no per-component override is needed.
    if (!isPlatformBrowser(this.platformId)) return;

    this.loginService.reqIsLogged().subscribe({
      next: (user) => {
        if (!user) { this.localeNav.navigate(['/']); return; }
        this.loadTexts();
      },
      error: () => this.localeNav.navigate(['/'])
    });
  }

  // ——— List ———

  loadTexts(): void {
    this.loadingList = true;
    this.myTexts.list().subscribe({
      next: (list) => { this.texts = list; this.loadingList = false; },
      error: () => { this.loadingList = false; this.showError(this.transloco.translate('myTools.messages.loadFailed')); }
    });
  }

  openText(id: number): void {
    this.localeNav.navigate(['/my-text', id]);
  }

  // ——— Extract from photo (OCR) ———

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files && input.files.length ? input.files[0] : null;
  }

  extract(): void {
    this.clearFeedback();
    if (!this.selectedFile) {
      this.showError(this.transloco.translate('myTools.messages.chooseImage'));
      return;
    }
    this.extracting = true;
    this.myTexts.extractFromImage(this.selectedFile).subscribe({
      next: (res) => {
        this.extracting = false;
        this.pasteText = res.text || '';
        this.selectedFile = null;
        this.showInfo(this.transloco.translate('myTools.messages.extracted'));
      },
      error: (err) => {
        this.extracting = false;
        this.handleError(err, this.transloco.translate('myTools.messages.readImageFailed'));
      }
    });
  }

  // ——— Create the reader ———

  create(): void {
    this.clearFeedback();
    if (!this.pasteText.trim()) {
      this.showError(this.transloco.translate('myTools.messages.pasteText'));
      return;
    }
    this.creating = true;
    this.myTexts.createFromText(this.pasteText.trim()).subscribe({
      next: (created) => {
        this.creating = false;
        this.pasteText = '';
        this.localeNav.navigate(['/my-text', created.id]);
      },
      error: (err) => {
        this.creating = false;
        this.handleError(err, this.transloco.translate('myTools.messages.processFailed'));
      }
    });
  }

  // ——— Delete ———

  askDelete(event: Event, id: number): void {
    event.stopPropagation();
    this.confirmingDeleteId = id;
  }

  cancelDelete(event: Event): void {
    event.stopPropagation();
    this.confirmingDeleteId = null;
  }

  confirmDelete(event: Event, id: number): void {
    event.stopPropagation();
    this.myTexts.delete(id).subscribe({
      next: () => {
        this.confirmingDeleteId = null;
        this.texts = this.texts.filter(t => t.id !== id);
      },
      error: () => {
        this.confirmingDeleteId = null;
        this.showError(this.transloco.translate('myTools.messages.deleteFailed'));
      }
    });
  }

  // ——— Helpers ———

  private handleError(err: any, fallback: string): void {
    if (err?.status === 429) {
      this.showError(err.error?.message || this.transloco.translate('myTools.messages.usageLimit'));
    } else if (err?.status === 400) {
      this.showError(err.error?.message || this.transloco.translate('myTools.messages.invalidInput'));
    } else {
      this.showError(fallback);
    }
  }

  private clearFeedback(): void { this.message = ''; this.messageType = ''; }
  private showError(msg: string): void { this.message = msg; this.messageType = 'error'; }
  private showInfo(msg: string): void { this.message = msg; this.messageType = 'info'; }
}
