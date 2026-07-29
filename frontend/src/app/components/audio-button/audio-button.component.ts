import { Component, Inject, Input, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';

/**
 * Minimal player for STATIC audio assets (the /learn tutorial mp3s). Unlike
 * SpeakButtonComponent — which is auth-gated, quota-metered and calls the paid
 * /api/tts backend — this one just plays a bundled file with `new Audio()`:
 * free, anonymous, works on prerendered public pages. If the file is missing
 * (e.g. before the assets have been generated) it fails silently.
 */
@Component({
  selector: 'app-audio-button',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './audio-button.component.html',
  styleUrl: './audio-button.component.scss'
})
export class AudioButtonComponent {

  /** Full asset path, e.g. 'assets/audio/learn/ma1.mp3'. */
  @Input({ required: true }) src!: string;
  /** Accessible label ("Listen to mā"). */
  @Input() label = '';
  @Input() size: 'sm' | 'md' = 'md';

  playing = false;
  private audio: HTMLAudioElement | null = null;

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  play(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.audio?.pause();
    this.audio = new Audio(this.src);
    this.audio.onended = () => (this.playing = false);
    this.audio.onerror = () => (this.playing = false);
    this.playing = true;
    this.audio.play().catch(() => (this.playing = false));
  }
}
