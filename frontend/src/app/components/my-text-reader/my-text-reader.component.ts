import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { MyTextsService, UserTextReader, UserTextWord } from '../../services/my-texts.service';
import { LoginService } from '../../services/login.service';
import { SpeakButtonComponent } from '../speak-button/speak-button.component';
import { WordChatComponent } from '../word-chat/word-chat.component';
import { LocaleNavService } from '../../i18n/locale-nav.service';
import { AuthUiService } from '../../services/auth-ui.service';
import { ActivityService } from '../../services/activity.service';

/**
 * Read-only reader for one of the user's PRIVATE texts. Mirrors the public text
 * reader (word popover, sentence modal, audio, pinyin/translation toggles) but is
 * fed entirely from the self-contained user text — no global dictionary — and only
 * allows reading and deleting (per spec, users can't edit their texts).
 */
@Component({
  selector: 'app-my-text-reader',
  standalone: true,
  imports: [CommonModule, FormsModule, SpeakButtonComponent, WordChatComponent, TranslocoModule],
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

  // AI word chat
  showChat = false;
  chatWord = '';
  chatSentence = '';
  chatText = '';
  chatTranslation = '';

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private myTexts: MyTextsService,
    private loginService: LoginService,
    private route: ActivatedRoute,
    private router: Router,
    private localeNav: LocaleNavService,
    private transloco: TranslocoService,
    private authUi: AuthUiService,
    private activity: ActivityService
  ) {}

  /** Full translation in the active UI language (falls back to the other language). */
  get displayTranslation(): string {
    if (!this.reader) return '';
    const es = this.transloco.getActiveLang() === 'es';
    const sents = this.reader.sentences;
    if (sents && sents.length) {
      // Rebuild the block from the 1:1 aligned sentence translations: one sentence per
      // line (readable), each ending with the terminal punctuation of its Chinese
      // sentence — the AI sometimes drops the final '.', '?' or '!'. Purely
      // presentational: no data is changed and old behaviour is preserved via the fallback.
      const lines = sents
        .map(s => this.punctuate(((es ? s.spanish : s.english) || (es ? s.english : s.spanish) || '').trim(), s.chinese))
        .filter(t => t);
      if (lines.length) return lines.join('\n');
    }
    // Old texts without aligned sentences: use the stored translation as-is.
    return (es ? this.reader.spanishTranslation : this.reader.englishTranslation)
      || this.reader.englishTranslation || this.reader.spanishTranslation || '';
  }

  /**
   * Ensures a sentence translation ends with sentence-final punctuation, borrowing the
   * terminator (?, !, .) from its source Chinese sentence when the translation lacks one.
   */
  private punctuate(translation: string, chinese: string): string {
    if (!translation) return '';
    if (/[.!?…。！？;；:]$/.test(translation)) return translation;
    const last = (chinese || '').trim().slice(-1);
    const map: Record<string, string> = {
      '？': '?', '?': '?', '！': '!', '!': '!',
      '。': '.', '．': '.', '.': '.', '…': '…', '；': ';', ';': ';'
    };
    return translation + (map[last] || '.');
  }

  ngOnInit(): void {
    // SEO for this private (noindex) page is applied globally by AppComponent
    // via resolveSeo('/my-text', lang), so no per-component override is needed.
    if (!isPlatformBrowser(this.platformId)) return;

    this.loginService.reqIsLogged().subscribe({
      next: (user) => {
        if (!user) { this.localeNav.navigate(['/']); return; }
        this.load();
      },
      error: () => this.localeNav.navigate(['/'])
    });
  }

  private load(): void {
    const id = Number(this.route.snapshot.params['id']);
    this.myTexts.get(id).subscribe({
      next: (reader) => {
        this.reader = reader;
        // Habit layer: private readers count toward the streak too (idempotent/day).
        this.activity.recordReading(`own:${id}`);
        this.wordsArray = reader.words;
        this.originalText = reader.words.map(w => w.chinese);
        this.translatedEnglishText = reader.words.map(w => w.english);
        this.translatedSpanishText = reader.words.map(w => w.spanish);

        if (reader.sentences && reader.sentences.length) {
          // New texts carry sentences with translations aligned 1:1 (built at creation).
          this.sentences = reader.sentences.map(s => s.chinese);
          this.englishSentences = reader.sentences.map(s => s.english);
          this.spanishSentences = reader.sentences.map(s => s.spanish);
        } else {
          // Fallback for texts created before aligned sentences existed.
          this.sentences = this.groupSentences(this.originalText);
          this.englishSentences = this.splitSentences(reader.englishTranslation);
          this.spanishSentences = this.splitSentences(reader.spanishTranslation);
        }
        this.loading = false;
      },
      error: () => {
        // Not found or not the owner → back to the list.
        this.localeNav.navigate(['/my-tools']);
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

  /** Opens the AI chat about the active word. Anonymous users get a sign-up notice. */
  openWordChat(): void {
    if (this.activeWordIndex === null || !this.reader) return;
    if (!this.loginService.isLogged()) {
      this.authUi.promptAuth('notice.loginToChat');
      return;
    }
    this.chatWord = this.originalText[this.activeWordIndex] || '';
    this.chatSentence = this.sentenceForWordIndex(this.activeWordIndex);
    this.chatText = this.reader.text || '';
    this.chatTranslation = this.displayTranslation;
    this.showChat = true;
  }

  /** The sentence (as a string) that the word at `index` belongs to. */
  private sentenceForWordIndex(index: number): string {
    let current: string[] = [];
    let start = 0;
    for (let i = 0; i < this.originalText.length; i++) {
      current.push(this.originalText[i]);
      const w = this.originalText[i];
      if (w.endsWith('.') || w.endsWith('。')) {
        if (index >= start && index <= i) return current.join('');
        current = [];
        start = i + 1;
      }
    }
    if (index >= start && current.length > 0) return current.join('');
    return this.originalText[index] || '';
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
      next: () => this.localeNav.navigate(['/my-tools']),
      error: () => this.localeNav.navigate(['/my-tools'])
    });
  }

  goBack(): void {
    this.localeNav.navigate(['/my-tools']);
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
