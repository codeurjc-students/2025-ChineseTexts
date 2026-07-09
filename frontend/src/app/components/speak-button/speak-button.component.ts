import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { Subscription } from 'rxjs';
import { AudioService, AudioType, SpeakState } from '../../services/audio.service';
import { LoginService } from '../../services/login.service';
import { LocaleNavService } from '../../i18n/locale-nav.service';

/**
 * Reusable speaker button. Plays the given Chinese `text` through AudioService
 * and reflects loading / playing / error state. Drop it anywhere a word,
 * sentence or full text should be listenable:
 *
 *   <app-speak-button [text]="word.chinese" audioType="word" ariaLabel="Listen to this word">
 *
 * Audio is registered-only (it calls the paid TTS API): anonymous visitors are sent
 * to sign up, and once a free user spends their monthly audio quota the button points
 * them to the premium page.
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

  /** 'word' (cheap, high monthly quota) or 'phrase' for sentences / full text (metered). */
  @Input() audioType: AudioType = 'phrase';

  state: SpeakState = 'idle';

  private myId = -1;
  private sub?: Subscription;

  constructor(
    private audio: AudioService,
    private loginService: LoginService,
    private localeNav: LocaleNavService,
    private transloco: TranslocoService
  ) {}

  ngOnInit(): void {
    this.sub = this.audio.state$.subscribe(e => {
      if (e.id === this.myId) this.state = e.state;
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    if (this.state === 'playing' || this.state === 'loading') this.audio.stop();
  }

  get isLogged(): boolean {
    return this.loginService.isLogged();
  }

  toggle(event: Event): void {
    event.stopPropagation();           // never trigger the parent (word/sentence click)

    // Registered-only: send anonymous visitors to sign up instead of calling the
    // backend (which would 401 anyway). This is the registration funnel.
    if (!this.isLogged) {
      this.localeNav.navigate(['/signup']);
      return;
    }
    // Monthly audio quota spent: guide the user to the premium page.
    if (this.state === 'limitReached') {
      this.localeNav.navigate(['/premium']);
      return;
    }
    if (this.state === 'loading' || this.state === 'playing') {
      this.audio.stop();
      this.state = 'idle';
      return;
    }
    this.state = 'loading';            // instant feedback; async states arrive via state$
    this.myId = this.audio.speak(this.text, this.audioType);
  }

  get icon(): string {
    if (!this.isLogged) return 'bi-lock';       // audio locked until registration
    switch (this.state) {
      case 'playing':      return 'bi-volume-up-fill';
      case 'error':        return 'bi-volume-mute';
      case 'authRequired': return 'bi-lock';
      case 'limitReached': return 'bi-star';    // hints at the premium upgrade
      default:             return 'bi-volume-up';
    }
  }

  /** Localized tooltip / aria-label reflecting the gate, the quota or the state. */
  get tooltip(): string {
    if (!this.isLogged) return this.transloco.translate('speakButton.registerToListen');
    switch (this.state) {
      case 'authRequired': return this.transloco.translate('speakButton.registerToListen');
      case 'limitReached': return this.transloco.translate('speakButton.limitReached');
      case 'error':        return this.transloco.translate('speakButton.audioUnavailable');
      default:             return this.ariaLabel || this.transloco.translate('speakButton.listen');
    }
  }
}
