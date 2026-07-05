import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { LoginService } from '../../services/login.service';
import { MyTextsService, UserTextSummary } from '../../services/my-texts.service';
import { SeoService } from '../../services/seo.service';

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
  imports: [CommonModule, FormsModule],
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
    private seo: SeoService
  ) {}

  ngOnInit(): void {
    this.seo.update({
      title: 'My Tools | ChineseReads',
      description: 'Turn any Chinese text or photo into your own private graded reader.',
      path: '/my-tools',
      noindex: true
    });
    if (!isPlatformBrowser(this.platformId)) return;

    this.loginService.reqIsLogged().subscribe({
      next: (user) => {
        if (!user) { this.router.navigate(['/']); return; }
        this.loadTexts();
      },
      error: () => this.router.navigate(['/'])
    });
  }

  // ——— List ———

  loadTexts(): void {
    this.loadingList = true;
    this.myTexts.list().subscribe({
      next: (list) => { this.texts = list; this.loadingList = false; },
      error: () => { this.loadingList = false; this.showError('Could not load your texts.'); }
    });
  }

  openText(id: number): void {
    this.router.navigate(['/my-text', id]);
  }

  // ——— Extract from photo (OCR) ———

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files && input.files.length ? input.files[0] : null;
  }

  extract(): void {
    this.clearFeedback();
    if (!this.selectedFile) {
      this.showError('Please choose an image first.');
      return;
    }
    this.extracting = true;
    this.myTexts.extractFromImage(this.selectedFile).subscribe({
      next: (res) => {
        this.extracting = false;
        this.pasteText = res.text || '';
        this.selectedFile = null;
        this.showInfo('Text extracted. Review and fix it below, then create your reader.');
      },
      error: (err) => {
        this.extracting = false;
        this.handleError(err, 'Could not read the image. Please try another photo.');
      }
    });
  }

  // ——— Create the reader ———

  create(): void {
    this.clearFeedback();
    if (!this.pasteText.trim()) {
      this.showError('Paste some Chinese text, or extract it from a photo first.');
      return;
    }
    this.creating = true;
    this.myTexts.createFromText(this.pasteText.trim()).subscribe({
      next: (created) => {
        this.creating = false;
        this.pasteText = '';
        this.router.navigate(['/my-text', created.id]);
      },
      error: (err) => {
        this.creating = false;
        this.handleError(err, 'Could not process your text. Please try again.');
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
        this.showError('Could not delete the text. Please try again.');
      }
    });
  }

  // ——— Helpers ———

  private handleError(err: any, fallback: string): void {
    if (err?.status === 429) {
      this.showError(err.error?.message || 'You have reached your usage limit. Please try again later.');
    } else if (err?.status === 400) {
      this.showError(err.error?.message || 'Please provide a valid Chinese text or image.');
    } else {
      this.showError(fallback);
    }
  }

  private clearFeedback(): void { this.message = ''; this.messageType = ''; }
  private showError(msg: string): void { this.message = msg; this.messageType = 'error'; }
  private showInfo(msg: string): void { this.message = msg; this.messageType = 'info'; }
}
