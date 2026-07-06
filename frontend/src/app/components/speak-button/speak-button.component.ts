import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoModule } from '@jsverse/transloco';
import { Subscription } from 'rxjs';
import { AudioService, SpeakState } from '../../services/audio.service';

/**
 * Reusable speaker button. Plays the given Chinese `text` through AudioService
 * and reflects loading / playing / error state. Drop it anywhere a word,
 * sentence or full text should be listenable:
 *
 *   <app-speak-button [text]="word.chinese" ariaLabel="Listen to this word">
 */
@Component({
  selector: 'app-speak-button',
  standalone: true,
  imports: [CommonModule, TranslocoModule],
  templateUrl: './speak-button.component.html',
  styleUrl: './speak-button.component.scss'
})
export class SpeakButtonComponent implements OnInit, OnDestroy {

  /** The Chinese text to synthesize and play. */
  @Input() text = '';

  /** Accessible label / tooltip. Falls back to the localized "Listen" when empty. */
  @Input() ariaLabel = '';

  /** Visual size: 'sm' inline (words/sentences), 'md' default, 'lg' flashcards. */
  @Input() size: 'sm' | 'md' | 'lg' = 'md';

  state: SpeakState = 'idle';

  private myId = -1;
  private sub?: Subscription;

  constructor(private audio: AudioService) {}

  ngOnInit(): void {
    this.sub = this.audio.state$.subscribe(e => {
      if (e.id === this.myId) this.state = e.state;
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    if (this.state === 'playing' || this.state === 'loading') this.audio.stop();
  }

  toggle(event: Event): void {
    event.stopPropagation();           // never trigger the parent (word/sentence click)
    if (this.state === 'loading' || this.state === 'playing') {
      this.audio.stop();
      this.state = 'idle';
      return;
    }
    this.state = 'loading';            // instant feedback; async states arrive via state$
    this.myId = this.audio.speak(this.text);
  }

  get icon(): string {
    switch (this.state) {
      case 'playing': return 'bi-volume-up-fill';
      case 'error':   return 'bi-volume-mute';
      default:        return 'bi-volume-up';
    }
  }
}
