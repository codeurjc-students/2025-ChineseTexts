import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { AiService, AiTextResult } from '../../services/ai.service';
import { LoginService } from '../../services/login.service';
import { TextsService } from '../../services/texts.service';
import { WordsService } from '../../services/words.service';
import { LocaleNavService } from '../../i18n/locale-nav.service';

interface MissingWordForm {
  chinese: string;
  pinyin: string;
  english: string;
  spanish: string;
  saved: boolean;
  saving: boolean;
  error: string;
}

type AiMode = 'choose' | 'generate' | 'ocr';
type Status = 'idle' | 'loading' | 'ready' | 'uploading' | 'success' | 'error';

@Component({
  selector: 'app-ai-tools',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoModule],
  templateUrl: './ai-tools.component.html',
  styleUrl: './ai-tools.component.scss'
})
export class AiToolsComponent implements OnInit {

  /**
   * When set (e.g. embedded inside Admin Tools), the component skips its own mode chooser
   * and starts directly in that mode. Going "back" then returns to the host via {@link exit}.
   */
  @Input() forcedMode: 'generate' | 'ocr' | null = null;

  /** Emitted when the admin wants to leave this tool (only meaningful when embedded). */
  @Output() exit = new EventEmitter<void>();

  mode: AiMode = 'choose';
  status: Status = 'idle';
  errorMessage = '';

  // Campos del formulario
  chineseText = '';
  titleEnglish = '';
  titleSpanish = '';
  englishTranslation = '';
  spanishTranslation = '';
  englishDescription = '';
  spanishDescription = '';
  level = 'HSK1';
  topic = '';
  creationDate = new Date().toISOString().split('T')[0];
  imageFile: File | null = null;
  imagePreview: string | null = null;

  // Palabras faltantes
  missingWordForms: MissingWordForm[] = [];
  missingWordsOpen = true;

  // OCR
  ocrImageFile: File | null = null;
  ocrImagePreview: string | null = null;

  levels = ['HSK1', 'HSK2', 'HSK3', 'HSK4', 'HSK5', 'HSK6'];

  // Validación
  validationError = '';

  constructor(
    private aiService: AiService,
    private textsService: TextsService,
    private wordsService: WordsService,
    private loginService: LoginService,
    private transloco: TranslocoService,
    private localeNav: LocaleNavService
  ) {}

  ngOnInit(): void {
    if (this.forcedMode) {
      this.mode = this.forcedMode;
    }
    this.loginService.reqIsLogged().subscribe({
      next: (user) => {
        if (!user || !user.roles.includes('ADMIN')) {
          this.localeNav.navigate(['/']);
        }
      },
      error: () => this.localeNav.navigate(['/'])
    });
  }

  selectMode(mode: AiMode): void {
    this.mode = mode;
    this.reset();
  }

  /**
   * Back navigation: when embedded with a forced mode, leave the tool entirely;
   * otherwise return to this component's own mode chooser.
   */
  goBack(): void {
    if (this.forcedMode) {
      this.exit.emit();
    } else {
      this.selectMode('choose');
    }
  }

  // ——— Validación del formulario ———

  get sentenceCountMatch(): boolean {
    const count = (text: string) => (text.match(/[.。]/g) || []).length;
    const original = count(this.chineseText);
    const english = count(this.englishTranslation);
    const spanish = count(this.spanishTranslation);
    return original > 0 && original === english && original === spanish;
  }

  get formComplete(): boolean {
    return !!(
      this.chineseText.trim() &&
      this.titleEnglish.trim() &&
      this.titleSpanish.trim() &&
      this.englishTranslation.trim() &&
      this.spanishTranslation.trim() &&
      this.englishDescription.trim() &&
      this.spanishDescription.trim()
    );
  }

  validateBeforeUpload(): boolean {
    this.validationError = '';

    if (!this.formComplete) {
      this.validationError = this.transloco.translate('aiTools.errors.fillAll');
      return false;
    }

    if (!this.imageFile) {
      this.validationError = this.transloco.translate('aiTools.errors.imageRequired');
      return false;
    }

    if (!this.sentenceCountMatch) {
      this.validationError = this.transloco.translate('aiTools.errors.sentenceMismatch');
      return false;
    }

    if (!this.allMissingWordsSaved) {
      this.validationError = this.transloco.translate('aiTools.errors.saveWords');
      return false;
    }

    return true;
  }

  // ——— Generar texto con IA ———

  generate(): void {
    this.status = 'loading';
    this.errorMessage = '';

    this.aiService.generateText(this.level, this.topic).subscribe({
      next: (result) => this.applyAiResult(result),
      error: () => {
        this.status = 'error';
        this.errorMessage = this.transloco.translate('aiTools.errors.aiConnection');
      }
    });
  }

  // ——— OCR ———

  onOcrImageChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.ocrImageFile = input.files[0];
    const reader = new FileReader();
    reader.onload = () => this.ocrImagePreview = reader.result as string;
    reader.readAsDataURL(this.ocrImageFile);
  }

  processOcr(): void {
    if (!this.ocrImageFile) return;
    this.status = 'loading';
    this.errorMessage = '';

    this.aiService.processOcr(this.ocrImageFile).subscribe({
      next: (result) => this.applyAiResult(result),
      error: () => {
        this.status = 'error';
        this.errorMessage = this.transloco.translate('aiTools.errors.ocrFailed');
      }
    });
  }

  private applyAiResult(result: AiTextResult): void {
    this.chineseText = result.chineseText;
    this.titleEnglish = result.titleEnglish;
    this.titleSpanish = result.titleSpanish;
    this.englishTranslation = result.englishTranslation;
    this.spanishTranslation = result.spanishTranslation;
    this.englishDescription = result.englishDescription;
    this.spanishDescription = result.spanishDescription;

    this.missingWordForms = result.missingWordSuggestions.map(s => ({
      chinese: s.chinese,
      pinyin: s.pinyin,
      english: s.english,
      spanish: s.spanish,
      saved: false,
      saving: false,
      error: ''
    }));

    this.status = 'ready';
    this.validationError = '';
  }

  onImageChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.imageFile = input.files[0];
    const reader = new FileReader();
    reader.onload = () => this.imagePreview = reader.result as string;
    reader.readAsDataURL(this.imageFile);
    this.validationError = '';
  }

  // ——— Guardar palabras faltantes ———

  saveWord(form: MissingWordForm): void {
    if (!form.pinyin.trim() || !form.english.trim() || !form.spanish.trim()) {
      form.error = this.transloco.translate('aiTools.errors.wordFieldsRequired');
      return;
    }
    form.saving = true;
    form.error = '';

    this.wordsService.saveWord({
      chinese: form.chinese,
      pinyin: form.pinyin.trim(),
      english: form.english.trim(),
      spanish: form.spanish.trim()
    }).subscribe({
      next: () => { form.saved = true; form.saving = false; },
      error: (err) => {
        form.saving = false;
        form.error = err.status === 409
          ? this.transloco.translate('aiTools.errors.wordExists')
          : this.transloco.translate('aiTools.errors.wordSaveFailed');
        if (err.status === 409) form.saved = true;
      }
    });
  }

  get allMissingWordsSaved(): boolean {
    return this.missingWordForms.length === 0 ||
           this.missingWordForms.every(f => f.saved);
  }

  get hasSomeSaved(): boolean {
    return this.missingWordForms.some(f => f.saved);
  }

  get missingSavedCount(): number {
    return this.missingWordForms.filter(f => f.saved).length;
  }

  get canUpload(): boolean {
    return this.status === 'ready';
  }

  // ——— Subir texto ———

  upload(): void {
    if (!this.validateBeforeUpload()) return;

    this.status = 'uploading';
    this.validationError = '';

    const data = {
      id: null,
      titleEnglish: this.titleEnglish.trim(),
      titleSpanish: this.titleSpanish.trim(),
      text: this.chineseText.trim(),
      englishTranslation: this.englishTranslation.trim(),
      spanishTranslation: this.spanishTranslation.trim(),
      englishDescription: this.englishDescription.trim(),
      spanishDescription: this.spanishDescription.trim(),
      level: this.level,
      creationDate: this.creationDate
    };

    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (this.imageFile) formData.append('image', this.imageFile);

    this.textsService.uploadText(formData).subscribe({
      next: () => { this.status = 'success'; },
      error: (err) => {
        this.status = 'ready';
        this.validationError = err.status === 409
          ? this.transloco.translate('aiTools.errors.titleExists')
          : this.transloco.translate('aiTools.errors.uploadFailed');
      }
    });
  }

  goToTexts(): void {
    this.localeNav.navigate(['/texts']);
  }

  reset(): void {
    this.status = 'idle';
    this.errorMessage = '';
    this.validationError = '';
    this.chineseText = '';
    this.titleEnglish = '';
    this.titleSpanish = '';
    this.englishTranslation = '';
    this.spanishTranslation = '';
    this.englishDescription = '';
    this.spanishDescription = '';
    this.topic = '';
    this.imageFile = null;
    this.imagePreview = null;
    this.ocrImageFile = null;
    this.ocrImagePreview = null;
    this.missingWordForms = [];
  }
}