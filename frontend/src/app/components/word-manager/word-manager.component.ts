import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { WordsService, Word } from '../../services/words.service';

/**
 * Admin tool to manage dictionary words: search a Chinese word, then edit, delete,
 * or (if it does not exist yet) create it.
 *
 * States:
 *  - 'search': waiting for the admin to type a Chinese word and search.
 *  - 'edit':   the word exists; its fields are shown and can be updated or deleted.
 *  - 'create': the word does not exist; an empty form is shown to add it.
 */
type Mode = 'search' | 'edit' | 'create';

@Component({
  selector: 'app-word-manager',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoModule],
  templateUrl: './word-manager.component.html',
  styleUrl: './word-manager.component.scss'
})
export class WordManagerComponent {

  /** Emitted when the admin wants to go back to the Admin Tools menu. */
  @Output() exit = new EventEmitter<void>();

  mode: Mode = 'search';

  searchTerm = '';
  /** The word currently being edited/created. Never mutates the search input directly. */
  word: Word = this.emptyWord();

  loading = false;
  confirmingDelete = false;

  message = '';
  messageType: 'success' | 'error' | '' = '';

  constructor(
    private wordsService: WordsService,
    private transloco: TranslocoService
  ) {}

  private emptyWord(chinese = ''): Word {
    return { chinese, pinyin: '', english: '', spanish: '' };
  }

  private clearFeedback(): void {
    this.message = '';
    this.messageType = '';
    this.confirmingDelete = false;
  }

  private showError(msg: string): void {
    this.message = msg;
    this.messageType = 'error';
  }

  private showSuccess(msg: string): void {
    this.message = msg;
    this.messageType = 'success';
  }

  // ——— Search ———

  search(): void {
    const term = this.searchTerm.trim();
    this.clearFeedback();

    if (!term) {
      this.showError(this.transloco.translate('wordManager.errors.enterWord'));
      return;
    }

    this.loading = true;
    this.wordsService.getDictionaryWord(term).subscribe({
      next: (found) => {
        this.loading = false;
        this.word = { ...found };
        this.mode = 'edit';
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 404) {
          // Word does not exist yet: offer to create it with the searched characters.
          this.word = this.emptyWord(term);
          this.mode = 'create';
          this.showError(this.transloco.translate('wordManager.errors.notFound', { term }));
        } else if (err.status === 401 || err.status === 403) {
          this.showError(this.transloco.translate('wordManager.errors.unauthorized'));
        } else {
          this.showError(this.transloco.translate('wordManager.errors.searchFailed'));
        }
      }
    });
  }

  // ——— Create ———

  create(): void {
    this.clearFeedback();
    if (!this.fieldsComplete()) {
      this.showError(this.transloco.translate('wordManager.errors.fieldsRequiredCreate'));
      return;
    }

    this.loading = true;
    this.wordsService.saveWord(this.trimmedWord()).subscribe({
      next: (saved) => {
        this.loading = false;
        this.word = { ...saved };
        this.mode = 'edit';
        this.showSuccess(this.transloco.translate('wordManager.messages.added'));
      },
      error: (err) => {
        this.loading = false;
        this.showError(err.status === 409
          ? this.transloco.translate('wordManager.errors.exists')
          : this.transloco.translate('wordManager.errors.addFailed'));
      }
    });
  }

  // ——— Update ———

  update(): void {
    this.clearFeedback();
    if (this.word.id == null) {
      this.showError(this.transloco.translate('wordManager.errors.noIdUpdate'));
      return;
    }
    if (!this.fieldsComplete()) {
      this.showError(this.transloco.translate('wordManager.errors.fieldsRequired'));
      return;
    }

    this.loading = true;
    this.wordsService.updateWord(this.word.id, this.trimmedWord()).subscribe({
      next: (updated) => {
        this.loading = false;
        this.word = { ...updated };
        this.showSuccess(this.transloco.translate('wordManager.messages.updated'));
      },
      error: (err) => {
        this.loading = false;
        this.showError(err.status === 404
          ? this.transloco.translate('wordManager.errors.goneUpdate')
          : this.transloco.translate('wordManager.errors.saveFailed'));
      }
    });
  }

  // ——— Delete ———

  askDelete(): void {
    this.message = '';
    this.messageType = '';
    this.confirmingDelete = true;
  }

  cancelDelete(): void {
    this.confirmingDelete = false;
  }

  confirmDelete(): void {
    if (this.word.id == null) {
      this.showError(this.transloco.translate('wordManager.errors.noIdDelete'));
      return;
    }
    this.loading = true;
    this.confirmingDelete = false;
    const removed = this.word.chinese;

    this.wordsService.deleteWord(this.word.id).subscribe({
      next: () => {
        this.loading = false;
        this.resetToSearch();
        this.showSuccess(this.transloco.translate('wordManager.messages.deleted', { word: removed }));
      },
      error: (err) => {
        this.loading = false;
        this.showError(err.status === 409
          ? this.transloco.translate('wordManager.errors.inUse')
          : err.status === 404
            ? this.transloco.translate('wordManager.errors.goneDelete')
            : this.transloco.translate('wordManager.errors.deleteFailed'));
      }
    });
  }

  // ——— Helpers ———

  fieldsComplete(): boolean {
    return !!(this.word.chinese.trim() && this.word.pinyin.trim() &&
              this.word.english.trim() && this.word.spanish.trim());
  }

  private trimmedWord(): Word {
    return {
      id: this.word.id,
      chinese: this.word.chinese.trim(),
      pinyin: this.word.pinyin.trim(),
      english: this.word.english.trim(),
      spanish: this.word.spanish.trim()
    };
  }

  resetToSearch(): void {
    this.mode = 'search';
    this.searchTerm = '';
    this.word = this.emptyWord();
    this.confirmingDelete = false;
  }

  newSearch(): void {
    this.resetToSearch();
    this.clearFeedback();
  }

  goBack(): void {
    this.exit.emit();
  }
}
