import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { MyTextsService, UserTextReader, UserTextWord } from '../../services/my-texts.service';
import { LoginService } from '../../services/login.service';
import { SpeakButtonComponent } from '../speak-button/speak-button.component';
import { SeoService } from '../../services/seo.service';

/**
 * Read-only reader for one of the user's PRIVATE texts. Mirrors the public text
 * reader (word popover, sentence modal, audio, pinyin/translation toggles) but is
 * fed entirely from the self-contained user text — no global dictionary — and only
 * allows reading and deleting (per spec, users can't edit their texts).
 */
@Component({
  selector: 'app-my-text-reader',
  standalone: true,
  imports: [CommonModule, FormsModule, SpeakButtonComponent],
  templateUrl: './my-text-reader.component.html',
  styleUrl: './my-text-reader.component.scss'
})
export class MyTextReaderComponent implements OnInit {

  reader: UserTextReader | null = null;
  loading = true;

  originalText: string[] = [];
  wordsArray: UserTextWord[] = [];
  translatedEnglishText: string[] = [];
  translatedSpanishText: string[] = [];
  sentences: string[] = [];
  englishSentences: string[] = [];
  spanishSentences: string[] = [];

  showTranslation = true;
  showSentences = false;
  showPinyin = true;

  activeWordIndex: number | null = null;
  activeSentenceIndex: number | null = null;

  showDeleteModal = false;

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private myTexts: MyTextsService,
    private loginService: LoginService,
    private route: ActivatedRoute,
    private router: Router,
    private seo: SeoService
  ) {}

  ngOnInit(): void {
    this.seo.update({
      title: 'My Text | ChineseReads',
      description: 'Read your private Chinese text.',
      path: '/my-text',
      noindex: true
    });
    if (!isPlatformBrowser(this.platformId)) return;

    this.loginService.reqIsLogged().subscribe({
      next: (user) => {
        if (!user) { this.router.navigate(['/']); return; }
        this.load();
      },
      error: () => this.router.navigate(['/'])
    });
  }

  private load(): void {
    const id = Number(this.route.snapshot.params['id']);
    this.myTexts.get(id).subscribe({
      next: (reader) => {
        this.reader = reader;
        this.wordsArray = reader.words;
        this.originalText = reader.words.map(w => w.chinese);
        this.translatedEnglishText = reader.words.map(w => w.english);
        this.translatedSpanishText = reader.words.map(w => w.spanish);
        this.sentences = this.groupSentences(this.originalText);
        this.englishSentences = this.splitSentences(reader.englishTranslation);
        this.spanishSentences = this.splitSentences(reader.spanishTranslation);
        this.loading = false;
      },
      error: () => {
        // Not found or not the owner → back to the list.
        this.router.navigate(['/my-tools']);
      }
    });
  }

  // ——— Word popover ———

  onWordClick(event: MouseEvent, index: number): void {
    event.stopPropagation();
    this.activeSentenceIndex = null;
    this.activeWordIndex = this.activeWordIndex === index ? null : index;
  }

  closeWordPopover(): void {
    this.activeWordIndex = null;
  }

  // ——— Sentence modal ———

  onSentenceClick(index: number): void {
    this.activeWordIndex = null;
    this.activeSentenceIndex = this.activeSentenceIndex === index ? null : index;
  }

  closeSentenceModal(): void {
    this.activeSentenceIndex = null;
  }

  // ——— Delete ———

  openDeleteModal(): void { this.showDeleteModal = true; }
  cancelDelete(): void { this.showDeleteModal = false; }

  confirmDelete(): void {
    if (!this.reader) return;
    this.myTexts.delete(this.reader.id).subscribe({
      next: () => this.router.navigate(['/my-tools']),
      error: () => this.router.navigate(['/my-tools'])
    });
  }

  goBack(): void {
    this.router.navigate(['/my-tools']);
  }

  // ——— Helpers (same sentence logic as the public reader) ———

  private groupSentences(text: string[]): string[] {
    const sentences: string[] = [];
    let current: string[] = [];
    for (const word of text) {
      current.push(word);
      if (word.endsWith('.') || word.endsWith('。')) {
        sentences.push(current.join(''));
        current = [];
      }
    }
    if (current.length > 0) sentences.push(current.join(''));
    return sentences;
  }

  private splitSentences(text: string): string[] {
    return (text || '').split(/(?<=\.|。)(?=\s|$)/).filter(s => s.trim() !== '');
  }
}
