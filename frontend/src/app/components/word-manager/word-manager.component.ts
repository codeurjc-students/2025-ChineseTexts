import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

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
  imports: [CommonModule, FormsModule],
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

  constructor(private wordsService: WordsService) {}

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
      this.showError('Please enter a Chinese word to search.');
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
          this.showError(`"${term}" is not in the dictionary yet. Fill in the fields to add it.`);
        } else if (err.status === 401 || err.status === 403) {
          this.showError('You are not authorized to manage the dictionary.');
        } else {
          this.showError('Error searching for the word. Please try again.');
        }
      }
    });
  }

  // ——— Create ———

  create(): void {
    this.clearFeedback();
    if (!this.fieldsComplete()) {
      this.showError('All fields are required to add a new word.');
      return;
    }

    this.loading = true;
    this.wordsService.saveWord(this.trimmedWord()).subscribe({
      next: (saved) => {
        this.loading = false;
        this.word = { ...saved };
        this.mode = 'edit';
        this.showSuccess('Word added to the dictionary.');
      },
      error: (err) => {
        this.loading = false;
        this.showError(err.status === 409
          ? 'This word already exists in the dictionary.'
          : 'Error adding the word. Please try again.');
      }
    });
  }

  // ——— Update ———

  update(): void {
    this.clearFeedback();
    if (this.word.id == null) {
      this.showError('Cannot update a word without an identifier.');
      return;
    }
    if (!this.fieldsComplete()) {
      this.showError('All fields are required.');
      return;
    }

    this.loading = true;
    this.wordsService.updateWord(this.word.id, this.trimmedWord()).subscribe({
      next: (updated) => {
        this.loading = false;
        this.word = { ...updated };
        this.showSuccess('Changes saved successfully.');
      },
      error: (err) => {
        this.loading = false;
        this.showError(err.status === 404
          ? 'The word no longer exists. It may have been deleted.'
          : 'Error saving changes. Please try again.');
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
      this.showError('Cannot delete a word without an identifier.');
      return;
    }
    this.loading = true;
    this.confirmingDelete = false;
    const removed = this.word.chinese;

    this.wordsService.deleteWord(this.word.id).subscribe({
      next: () => {
        this.loading = false;
        this.resetToSearch();
        this.showSuccess(`"${removed}" was deleted from the dictionary.`);
      },
      error: (err) => {
        this.loading = false;
        this.showError(err.status === 409
          ? 'This word is used in existing flashcards and cannot be deleted.'
          : err.status === 404
            ? 'The word no longer exists.'
            : 'Error deleting the word. Please try again.');
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
