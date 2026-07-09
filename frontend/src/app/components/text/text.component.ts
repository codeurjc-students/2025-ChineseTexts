import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TextsService, TextItem } from '../../services/texts.service';
import { LoginService } from '../../services/login.service';
import { WordsService, Word } from '../../services/words.service';
import { CollectionsService, CollectionDTO } from '../../services/collections.service';
import { SpeakButtonComponent } from '../speak-button/speak-button.component';
import { WordChatComponent } from '../word-chat/word-chat.component';
import { SeoService } from '../../services/seo.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { Lang } from '../../i18n/locale.util';
import { LocaleNavService } from '../../i18n/locale-nav.service';
import { AuthUiService } from '../../services/auth-ui.service';

@Component({
  selector: 'app-text',
  standalone: true,
  imports: [CommonModule, FormsModule, SpeakButtonComponent, WordChatComponent, TranslocoModule],
  templateUrl: './text.component.html',
  styleUrl: './text.component.scss'
})
export class TextComponent implements OnInit {

  text: TextItem = {
    id: 0, titleSpanish: '', titleEnglish: '', text: '',
    spanishTranslation: '', englishTranslation: '', level: '',
    englishDescription: '', spanishDescription: '', creationDate: '', liked: false
  };

  originalText: string[] = [];
  wordsArray: Word[] = [];
  translatedSpanishText: string[] = [];
  translatedEnglishText: string[] = [];
  originalTextSeparatedBySentences: string[] = [];
  translatedSpanishTextSeparatedBySentences: string[] = [];
  translatedEnglishTextSeparatedBySentences: string[] = [];

  liked = false;
  showTranslation = true;
  showSentences = false;
  showPinyin = true;

  activeWordIndex: number | null = null;
  activeSentenceIndex: number | null = null;

  // Save word
  showSavePanel = false;
  collections: CollectionDTO[] = [];
  selectedCollectionId: number | null = null;
  newCollectionTitle = '';
  showNewCollectionInput = false;
  saveStatus: 'idle' | 'success' | 'error' | 'duplicate' | 'not-logged' | 'no-collections' = 'idle';
  pendingWord: string | null = null;

  showDeleteTextModal = false;

  // AI word chat
  showChat = false;
  chatWord = '';
  chatSentence = '';
  chatText = '';
  chatTranslation = '';
  chatLevel = '';

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private textService: TextsService,
    private loginService: LoginService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private wordService: WordsService,
    private collectionsService: CollectionsService,
    private seo: SeoService,
    private transloco: TranslocoService,
    private localeNav: LocaleNavService,
    private authUi: AuthUiService
  ) {}

  get isAdmin(): boolean {
    return this.loginService.isRoleAdmin();
  }

  /** Text title in the active UI language (falls back to the other language). */
  get displayTitle(): string {
    const es = this.transloco.getActiveLang() === 'es';
    return (es ? this.text.titleSpanish : this.text.titleEnglish)
      || this.text.titleEnglish || this.text.titleSpanish || '';
  }

  /** Full translation of the text in the active UI language (falls back). */
  get displayTranslation(): string {
    const es = this.transloco.getActiveLang() === 'es';
    return (es ? this.text.spanishTranslation : this.text.englishTranslation)
      || this.text.englishTranslation || this.text.spanishTranslation || '';
  }

  ngOnInit(): void {
    this.loginService.reqIsLogged().subscribe(); // solo para actualizar el estado del usuario
    this.init(); // carga el texto independientemente
  }

  // ——— Palabra popover ———

  onWordClick(event: MouseEvent, index: number): void {
    event.stopPropagation();
    this.activeSentenceIndex = null;
    this.activeWordIndex = this.activeWordIndex === index ? null : index;
    this.resetSavePanel();
  }

  /** Opens the AI chat about the active word. Anonymous users get a sign-up notice. */
  openWordChat(): void {
    if (this.activeWordIndex === null) return;
    if (!this.loginService.isLogged()) {
      this.authUi.promptAuth('notice.loginToChat');
      return;
    }
    this.chatWord = this.originalText[this.activeWordIndex] || '';
    this.chatSentence = this.sentenceForWordIndex(this.activeWordIndex);
    this.chatText = this.text.text || '';
    this.chatTranslation = this.displayTranslation;
    this.chatLevel = this.text.level || '';
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

  closeWordPopover(): void {
    this.activeWordIndex = null;
    this.resetSavePanel();
  }

  // ——— Save word ———

  addWord(word: string): void {
    this.pendingWord = word;
    this.saveStatus = 'idle';

    if (!this.loginService.isLogged()) {
      this.saveStatus = 'not-logged';
      this.showSavePanel = true;
      return;
    }

    this.collectionsService.getUserCollections().subscribe({
      next: (cols) => {
        this.collections = cols;
        if (cols.length === 0) {
          this.saveStatus = 'no-collections';
        }
        this.showSavePanel = true;
      },
      error: () => {
        this.saveStatus = 'error';
        this.showSavePanel = true;
      }
    });
  }

  confirmSave(): void {
    if (!this.selectedCollectionId || !this.pendingWord) return;

    this.collectionsService.addFlashcard(this.selectedCollectionId, this.pendingWord, this.text.id).subscribe({
      next: () => {
        this.saveStatus = 'success';
        setTimeout(() => this.resetSavePanel(), 2000);
      },
      error: (err) => {
        this.saveStatus = err.status === 409 ? 'duplicate' : 'error';
      }
    });
  }

  createCollectionAndSave(): void {
    if (!this.newCollectionTitle.trim()) return;

    this.collectionsService.createCollection(this.newCollectionTitle.trim()).subscribe({
      next: (col) => {
        this.collections.push(col);
        this.selectedCollectionId = col.id;
        this.newCollectionTitle = '';
        this.showNewCollectionInput = false;
        this.saveStatus = 'idle';
      },
      error: () => this.saveStatus = 'error'
    });
  }

  resetSavePanel(): void {
    this.showSavePanel = false;
    this.selectedCollectionId = null;
    this.newCollectionTitle = '';
    this.showNewCollectionInput = false;
    this.saveStatus = 'idle';
    this.pendingWord = null;
  }

  // ——— Frase modal ———

  onSentenceClick(index: number): void {
    this.activeWordIndex = null;
    this.activeSentenceIndex = this.activeSentenceIndex === index ? null : index;
  }

  closeSentenceModal(): void {
    this.activeSentenceIndex = null;
  }

  toggleLike(): void {
    this.liked = !this.liked;
  }

  goBack(): void {
    this.localeNav.navigate(['/texts']);
  }

  // ——— Carga de datos ———

  private init(): void {
    const id = this.activatedRoute.snapshot.params['id'];
    this.getText(id);
    this.getSpanishText(id);
  }

  private getText(id: number): void {
    this.textService.getText(id).subscribe({
      next: (text) => {
        this.text = text;
        this.updateSeo();
      },
      error: (err) => console.error('Error loading text', err)
    });
  }

  /** Gives each text page a unique, content-rich title & description for SEO. */
  private updateSeo(): void {
    const lang = this.transloco.getActiveLang() as Lang;
    const level = this.text.level || 'HSK';

    if (lang === 'es') {
      const title = (this.text.titleSpanish || '').trim();
      const summary = (this.text.spanishDescription || '').trim();
      this.seo.update({
        title: title
          ? `${title} — Texto de lectura en chino ${level} | ChineseReads`
          : `Lee un texto en chino ${level} | ChineseReads`,
        description: summary
          || `Lee este texto en chino graduado de nivel ${level} con pinyin palabra por palabra, traducciones `
             + `instantáneas, desglose de frases y pronunciación con audio natural en ChineseReads.`,
        path: `/text/${this.text.id}`
      }, 'es');
      return;
    }

    const title = (this.text.titleEnglish || '').trim();
    const summary = (this.text.englishDescription || '').trim();
    this.seo.update({
      title: title
        ? `${title} — ${level} Chinese Reading Text | ChineseReads`
        : `Read a ${level} Chinese Text | ChineseReads`,
      description: summary
        || `Read this ${level} graded Chinese text with word-by-word pinyin, instant translations, `
           + `sentence breakdown and natural audio pronunciation on ChineseReads.`,
      path: `/text/${this.text.id}`
    }, 'en');
  }

  private getSpanishText(id: number): void {
    this.textService.getSpanishText(id).subscribe({
      next: (data) => {
        this.originalText = data[0];
        this.translatedSpanishText = data[1];
        this.originalTextSeparatedBySentences = this.getSentences(data[0]);
        this.translatedSpanishTextSeparatedBySentences = this.getSentencesString(this.text.spanishTranslation);
        this.getEnglishText(id);
      },
      error: (err) => console.error('Error loading Spanish text', err)
    });
  }

  private getEnglishText(id: number): void {
    this.textService.getEnglishText(id).subscribe({
      next: (data) => {
        this.translatedEnglishText = data[1];
        this.translatedEnglishTextSeparatedBySentences = this.getSentencesString(this.text.englishTranslation);
        this.getWords(data[0]);
      },
      error: (err) => console.error('Error loading English text', err)
    });
  }

  private getWords(chineseText: string[]): void {
    this.wordService.getTextWords(chineseText).subscribe({
      next: (wordsArray) => this.wordsArray = wordsArray,
      error: (err) => console.error('Error loading words', err)
    });
  }

  private getSentences(text: string[]): string[] {
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

  private getSentencesString(text: string): string[] {
    return text.split(/(?<=\.|。)(?=\s|$)/).filter(s => s.trim() !== '');
  }

  openDeleteTextModal(): void {
    this.showDeleteTextModal = true;
  }

  cancelDeleteText(): void {
    this.showDeleteTextModal = false;
  }

  confirmDeleteText(): void {
    this.textService.deleteText(this.text.id).subscribe({
      next: () => this.localeNav.navigate(['/texts']),
      error: (err) => console.error('Error deleting text', err)
    });
  }
}