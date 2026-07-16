import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TextsService, TextItem } from '../../services/texts.service';
import { LoginService } from '../../services/login.service';
import { WordsService, Word } from '../../services/words.service';
import { CollectionsService, CollectionDTO } from '../../services/collections.service';
import { SpeakButtonComponent } from '../speak-button/speak-button.component';
import { WordChatComponent } from '../word-chat/word-chat.component';
import { SeoService, SITE_URL } from '../../services/seo.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { Lang } from '../../i18n/locale.util';
import { LocaleNavService } from '../../i18n/locale-nav.service';
import { AuthUiService } from '../../services/auth-ui.service';
import { ActivityService } from '../../services/activity.service';
import { endsSentence, splitTranslatedSentences, sentenceBreaksFromTokens, groupTranslationLines } from '../../utils/sentence.util';

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
  /** The text's most useful words (unique, multi-character), for the study/SEO section. */
  keyVocabulary: Word[] = [];
  translatedSpanishText: string[] = [];
  translatedEnglishText: string[] = [];
  originalTextSeparatedBySentences: string[] = [];
  translatedSpanishTextSeparatedBySentences: string[] = [];
  translatedEnglishTextSeparatedBySentences: string[] = [];

  liked = false;
  showTranslation = true;
  showSentences = false;
  showPinyin = true;
  // Default TRUE on purpose: the vocabulary section must be in the DOM when
  // crawlers render the page (it is this text's unique indexable content).
  // Unchecking only ever happens on user interaction, which crawlers never do.
  showVocab = true;

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
    private authUi: AuthUiService,
    private activity: ActivityService
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

  /**
   * Full translation in the active UI language, laid out like the original —
   * same presentation as the private reader (my-text-reader). Rebuilt from the
   * per-sentence translations, each re-punctuated from its Chinese sentence:
   * when the text carries a real layout ('\n' between sentences: dialogues),
   * the lines mirror the original's lines; otherwise one sentence per line.
   * Purely presentational; if the sentence arrays are not usable the stored
   * translation is shown as-is (old behaviour).
   */
  get displayTranslation(): string {
    const es = this.transloco.getActiveLang() === 'es';
    const primary = es ? this.translatedSpanishTextSeparatedBySentences
                       : this.translatedEnglishTextSeparatedBySentences;
    const secondary = es ? this.translatedEnglishTextSeparatedBySentences
                         : this.translatedSpanishTextSeparatedBySentences;
    if (primary && primary.length) {
      const sents = primary
        .map((t, i) => this.punctuate((t || secondary[i] || '').trim(),
                                      this.originalTextSeparatedBySentences[i] || ''));
      const breaks = sentenceBreaksFromTokens(this.originalText);
      const lines = breaks.some(b => b)
        ? groupTranslationLines(sents, breaks)
        : sents.filter(t => t);
      if (lines.length) return lines.join('\n');
    }
    return (es ? this.text.spanishTranslation : this.text.englishTranslation)
      || this.text.englishTranslation || this.text.spanishTranslation || '';
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
      const w = this.originalText[i];
      if (!this.isLineBreak(w)) current.push(w);
      if (endsSentence(w)) {
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
        // Habit layer: log the reading (browser-only, logged-in only; idempotent
        // per day on the backend, so refreshes don't inflate the stats).
        if (isPlatformBrowser(this.platformId) && this.loginService.isLogged()) {
          this.activity.recordReading(`public:${text.id}`);
        }
      },
      error: (err) => console.error('Error loading text', err)
    });
  }

  /**
   * Gives each text page a unique, content-rich title & description for SEO,
   * plus per-text keywords, its cover as social image, and Article/LearningResource
   * structured data. Runs twice: once when the text arrives, and again when the
   * vocabulary is ready so keywords and JSON-LD `teaches` can include it.
   */
  private updateSeo(): void {
    const lang = this.transloco.getActiveLang() as Lang;
    const level = this.text.level || 'HSK';
    const image = this.text.id ? `${SITE_URL}/api/texts/${this.text.id}/image` : undefined;
    const keywords = this.buildKeywords(lang, level);

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
        keywords,
        image,
        path: `/text/${this.text.id}`
      }, 'es');
    } else {
      const title = (this.text.titleEnglish || '').trim();
      const summary = (this.text.englishDescription || '').trim();
      this.seo.update({
        title: title
          ? `${title} — ${level} Chinese Reading Text | ChineseReads`
          : `Read a ${level} Chinese Text | ChineseReads`,
        description: summary
          || `Read this ${level} graded Chinese text with word-by-word pinyin, instant translations, `
           + `sentence breakdown and natural audio pronunciation on ChineseReads.`,
        keywords,
        image,
        path: `/text/${this.text.id}`
      }, 'en');
    }

    this.updatePageJsonLd(lang, level);
  }

  /**
   * Per-text meta keywords: the title, the level phrases and the key vocabulary
   * (hanzi + pinyin), so each page targets its own long-tail searches instead of
   * the generic site-wide keyword list.
   */
  private buildKeywords(lang: Lang, level: string): string | undefined {
    if (!this.text.id) return undefined;
    const title = ((lang === 'es' ? this.text.titleSpanish : this.text.titleEnglish) || '').trim();
    const base = lang === 'es'
      ? [`texto en chino ${level}`, `lectura graduada ${level}`, 'aprender chino leyendo', 'vocabulario chino con pinyin']
      : [`${level} chinese text`, `${level} graded reader`, 'learn chinese by reading', 'chinese vocabulary with pinyin'];
    const vocab = this.keyVocabulary.slice(0, 10).map(w => `${w.chinese} ${w.pinyin}`.trim());
    return [...(title ? [title] : []), ...base, ...vocab].join(', ');
  }

  /**
   * Structured data (schema.org Article + LearningResource) so search engines
   * understand each reading as an educational resource: HSK level, publication
   * date, cover image and the key vocabulary it teaches.
   */
  private updatePageJsonLd(lang: Lang, level: string): void {
    if (!this.text.id) return;
    const es = lang === 'es';
    const title = ((es ? this.text.titleSpanish : this.text.titleEnglish)
      || this.text.titleEnglish || this.text.titleSpanish || '').trim();
    const description = ((es ? this.text.spanishDescription : this.text.englishDescription)
      || this.text.englishDescription || this.text.spanishDescription || '').trim();
    const url = `${SITE_URL}${es ? '/es' : ''}/text/${this.text.id}`;

    const data: Record<string, unknown> = {
      '@context': 'https://schema.org',
      '@type': ['Article', 'LearningResource'],
      headline: title,
      description,
      url,
      mainEntityOfPage: url,
      image: `${SITE_URL}/api/texts/${this.text.id}/image`,
      datePublished: this.text.creationDate || undefined,
      educationalLevel: level,
      learningResourceType: 'Reading passage',
      isAccessibleForFree: true,
      // The passage itself is Simplified Chinese; the UI language is already
      // declared on <html lang> and og:locale.
      inLanguage: 'zh-Hans',
      publisher: {
        '@type': 'Organization',
        name: 'ChineseReads',
        url: SITE_URL,
        logo: { '@type': 'ImageObject', url: `${SITE_URL}/icon-512.png` }
      }
    };
    if (this.keyVocabulary.length > 0) {
      data['teaches'] = this.keyVocabulary.map(w => `${w.chinese} (${w.pinyin})`).join(', ');
    }
    this.seo.setPageJsonLd(data);
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
      next: (wordsArray) => {
        this.wordsArray = wordsArray;
        this.computeKeyVocabulary();
        // Re-run so keywords + JSON-LD pick up the vocabulary just computed.
        // Guarded: if the words beat the text itself (parallel requests), the
        // text's own updateSeo() will run later with the vocabulary already set.
        if (this.text.id) this.updateSeo();
      },
      error: (err) => console.error('Error loading words', err)
    });
  }

  /**
   * Distils the segmented text into its key vocabulary: unique dictionary words
   * of 2+ characters (single characters are mostly particles, and punctuation
   * never reaches 2), in order of first appearance, capped at 12.
   */
  private computeKeyVocabulary(): void {
    const seen = new Set<string>();
    const vocab: Word[] = [];
    for (const w of this.wordsArray) {
      if (!w || !w.chinese || w.chinese.length < 2 || !w.pinyin) continue;
      if (seen.has(w.chinese)) continue;
      seen.add(w.chinese);
      vocab.push(w);
      if (vocab.length >= 12) break;
    }
    this.keyVocabulary = vocab;
  }

  /** Meaning of a vocabulary word in the active UI language (falls back). */
  vocabMeaning(w: Word): string {
    const es = this.transloco.getActiveLang() === 'es';
    return (es ? w.spanish : w.english) || w.english || w.spanish || '';
  }

  /**
   * The segmented text may carry standalone '\n' tokens (texts stored with line
   * breaks: dialogues, conversations). They are layout only: the words view
   * renders them as visual breaks and every other consumer skips them.
   */
  isLineBreak(word: string): boolean {
    return word === '\n';
  }

  // Sentence boundaries use the shared terminator set (。！？…；.!?; — the same
  // one the backend uses), so 你好！ or 是吗？ close a sentence just like 。 does.
  private getSentences(text: string[]): string[] {
    const sentences: string[] = [];
    let current: string[] = [];
    for (const word of text) {
      if (this.isLineBreak(word)) continue;
      current.push(word);
      if (endsSentence(word)) {
        sentences.push(current.join(''));
        current = [];
      }
    }
    if (current.length > 0) sentences.push(current.join(''));
    return sentences;
  }

  private getSentencesString(text: string): string[] {
    return splitTranslatedSentences(text);
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