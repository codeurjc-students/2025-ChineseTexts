import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { TextsService, ValidationResult } from '../../services/texts.service';
import { WordsService, Word } from '../../services/words.service';
import { LoginService } from '../../services/login.service';
import { LocaleNavService } from '../../i18n/locale-nav.service';
import { splitChineseSentences, splitTranslatedSentences } from '../../utils/sentence.util';

interface MissingWordForm {
  chinese: string;
  pinyin: string;
  english: string;
  spanish: string;
  saved: boolean;
  saving: boolean;
  error: string;
}

@Component({
  selector: 'app-upload-text',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoModule],
  templateUrl: './upload-text.component.html',
  styleUrl: './upload-text.component.scss'
})
export class UploadTextComponent implements OnInit {

  /** True when hosted inside Admin Tools; shows a "back to menu" control. */
  @Input() embedded = false;

  /** Emitted when the admin wants to go back to the Admin Tools menu. */
  @Output() exit = new EventEmitter<void>();

  titleEnglish = '';
  titleSpanish = '';
  chineseText = '';
  englishTranslation = '';
  spanishTranslation = '';
  englishDescription = '';
  spanishDescription = '';
  level = 'HSK1';
  creationDate = new Date().toISOString().split('T')[0];
  imageFile: File | null = null;
  imagePreview: string | null = null;

  status: 'idle' | 'validating' | 'valid' | 'invalid' | 'uploading' | 'success' | 'error' = 'idle';
  errorMessage = '';
  validationResult: ValidationResult | null = null;
  missingWordForms: MissingWordForm[] = [];
  missingWordsOpen = true;

  levels = ['HSK1', 'HSK2', 'HSK3', 'HSK4', 'HSK5', 'HSK6'];

  constructor(
    private textsService: TextsService,
    private wordsService: WordsService,
    private loginService: LoginService,
    private transloco: TranslocoService,
    private localeNav: LocaleNavService
  ) {}

  ngOnInit(): void {
    this.loginService.reqIsLogged().subscribe({
      next: (user) => {
        if (!user || !user.roles.includes('ADMIN')) {
          this.localeNav.navigate(['/']);
        }
      },
      error: () => this.localeNav.navigate(['/'])
    });
  }

  onImageChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.imageFile = input.files[0];
    const reader = new FileReader();
    reader.onload = () => this.imagePreview = reader.result as string;
    reader.readAsDataURL(this.imageFile);
  }

  get formComplete(): boolean {
    return !!(
      this.titleEnglish.trim() &&
      this.titleSpanish.trim() &&
      this.chineseText.trim() &&
      this.englishTranslation.trim() &&
      this.spanishTranslation.trim() &&
      this.englishDescription.trim() &&
      this.spanishDescription.trim() &&
      this.level &&
      this.imageFile
    );
  }

  get missingSavedCount(): number {
    return this.missingWordForms.filter(f => f.saved).length;
  }

  get hasSomeSaved(): boolean {
    return this.missingWordForms.some(f => f.saved);
  }

  get sentenceCountMatch(): boolean {
    // Same sentence rules as the backend (which rejects mismatches with 400):
    // this pre-check just surfaces the problem before uploading.
    const original = splitChineseSentences(this.chineseText).length;
    const english = splitTranslatedSentences(this.englishTranslation).length;
    const spanish = splitTranslatedSentences(this.spanishTranslation).length;
    return original > 0 && original === english && original === spanish;
  }

  validate(): void {
    this.errorMessage = '';

    if (!this.formComplete) {
      this.errorMessage = this.imageFile
        ? this.transloco.translate('uploadText.errors.fillRequired')
        : this.transloco.translate('uploadText.errors.imageRequiredValidate');
      this.status = 'error';
      return;
    }

    if (!this.sentenceCountMatch) {
      this.errorMessage = this.transloco.translate('uploadText.errors.sentenceMismatch');
      this.status = 'error';
      return;
    }

    this.status = 'validating';

    this.textsService.validateText(this.chineseText.trim()).subscribe({
      next: (result) => {
        this.validationResult = result;
        if (result.valid) {
          this.status = 'valid';
          this.missingWordForms = [];
        } else {
          this.status = 'invalid';
          this.missingWordForms = result.missingWords.map(w => ({
            chinese: w, pinyin: '', english: '', spanish: '',
            saved: false, saving: false, error: ''
          }));
          this.missingWordsOpen = true;
        }
      },
      error: () => {
        this.status = 'error';
        this.errorMessage = this.transloco.translate('uploadText.errors.validateFailed');
      }
    });
  }

  saveWord(form: MissingWordForm): void {
    if (!form.pinyin.trim() || !form.english.trim() || !form.spanish.trim()) {
      form.error = this.transloco.translate('uploadText.errors.wordFieldsRequired');
      return;
    }
    form.saving = true;
    form.error = '';

    const word: Word = {
      chinese: form.chinese,
      pinyin: form.pinyin.trim(),
      english: form.english.trim(),
      spanish: form.spanish.trim()
    };

    this.wordsService.saveWord(word).subscribe({
      next: () => {
        form.saved = true;
        form.saving = false;
        if (this.allMissingWordsSaved) this.validate();
      },
      error: (err) => {
        form.saving = false;
        form.error = err.status === 409
          ? this.transloco.translate('uploadText.errors.wordExists')
          : this.transloco.translate('uploadText.errors.wordSaveFailed');
        if (err.status === 409) {
          form.saved = true;
          if (this.allMissingWordsSaved) this.validate();
        }
      }
    });
  }

  get allMissingWordsSaved(): boolean {
    return this.missingWordForms.length > 0 && this.missingWordForms.every(f => f.saved);
  }

  get isUploading(): boolean {
    return this.status === 'uploading';
  }

  upload(): void {
    if (this.status !== 'valid') return;
    this.status = 'uploading';

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
        this.status = 'error';
        this.errorMessage = err.status === 409
          ? this.transloco.translate('uploadText.errors.titleExists')
          : err.status === 400
            ? this.transloco.translate('uploadText.errors.sentenceMismatch')
            : this.transloco.translate('uploadText.errors.uploadFailed');
      }
    });
  }

  goToTexts(): void {
    this.localeNav.navigate(['/texts']);
  }

  back(): void {
    this.exit.emit();
  }

  reset(): void {
    this.titleEnglish = '';
    this.titleSpanish = '';
    this.chineseText = '';
    this.englishTranslation = '';
    this.spanishTranslation = '';
    this.englishDescription = '';
    this.spanishDescription = '';
    this.level = 'HSK1';
    this.creationDate = new Date().toISOString().split('T')[0];
    this.imageFile = null;
    this.imagePreview = null;
    this.status = 'idle';
    this.errorMessage = '';
    this.validationResult = null;
    this.missingWordForms = [];
  }
}