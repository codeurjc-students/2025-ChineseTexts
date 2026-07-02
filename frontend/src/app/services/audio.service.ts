import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';

export type SpeakState = 'idle' | 'loading' | 'playing' | 'error';

/**
 * Plays Chinese text as speech using the backend TTS endpoint (/api/tts).
 *
 * A single audio element is reused, so starting a new playback stops the
 * previous one. Every request gets an incrementing id; components subscribe to
 * `state$` and filter by the id returned from `speak()` to follow just their
 * own playback. Fully SSR-safe: on the server it does nothing.
 */
@Injectable({ providedIn: 'root' })
export class AudioService {

  private readonly isBrowser: boolean;
  private audio: HTMLAudioElement | null = null;
  private objectUrl: string | null = null;

  private seq = 0;
  private activeId = 0;

  private readonly stateSubject = new Subject<{ id: number; state: SpeakState }>();
  readonly state$: Observable<{ id: number; state: SpeakState }> = this.stateSubject.asObservable();

  constructor(@Inject(PLATFORM_ID) platformId: Object, private http: HttpClient) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  /**
   * Fetches and plays speech for `text`, stopping any current playback.
   * Returns the request id; emits 'loading' → 'playing' → 'idle' (or 'error')
   * on `state$` for that id.
   */
  speak(text: string): number {
    const id = ++this.seq;
    const clean = (text || '').trim();

    if (!this.isBrowser) return id;           // no audio on the server
    if (!clean) { this.emit(id, 'error'); return id; }

    this.stop();                              // cancel previous + reset its button
    this.activeId = id;
    this.emit(id, 'loading');

    this.http.post('/api/tts', { text: clean }, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        if (this.activeId !== id) return;     // superseded while loading
        try {
          this.objectUrl = URL.createObjectURL(blob);
          const audio = new Audio(this.objectUrl);
          this.audio = audio;

          audio.onended = () => { if (this.activeId === id) { this.emit(id, 'idle'); this.cleanup(); } };
          audio.onerror = () => { if (this.activeId === id) { this.emit(id, 'error'); this.cleanup(); } };

          audio.play()
            .then(() => { if (this.activeId === id) this.emit(id, 'playing'); })
            .catch(() => { if (this.activeId === id) { this.emit(id, 'error'); this.cleanup(); } });
        } catch {
          this.emit(id, 'error');
          this.cleanup();
        }
      },
      error: () => { if (this.activeId === id) this.emit(id, 'error'); }
    });

    return id;
  }

  /** Stops current playback (if any) and resets its button to idle. */
  stop(): void {
    if (!this.isBrowser) return;
    if (this.audio) {
      this.audio.pause();
      if (this.activeId) this.emit(this.activeId, 'idle');
    }
    this.cleanup();
    this.activeId = 0;
  }

  private cleanup(): void {
    if (this.audio) {
      this.audio.onended = null;
      this.audio.onerror = null;
      this.audio = null;
    }
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }

  private emit(id: number, state: SpeakState): void {
    this.stateSubject.next({ id, state });
  }
}
