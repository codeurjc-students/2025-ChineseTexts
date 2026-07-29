import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { AudioButtonComponent } from '../audio-button/audio-button.component';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';
import { Lang, addLangPrefix } from '../../i18n/locale.util';
import { SITE_URL } from '../../services/seo.service';
import { wirePageJsonLd } from '../../utils/page-jsonld.util';
import {
  TONES, TONE_QUIZ, ToneQuizItem, QUIZ_TONE_OPTIONS, quizResultKey
} from '../../data/learn-tones';

/**
 * Lesson 2 (/learn/tones): the four Mandarin tones plus the neutral tone,
 * demonstrated with the classic "ma" set, and a 10-question ear-training
 * mini-quiz (hear a syllable → pick the tone). Fully client-side over static
 * audio assets — free, anonymous and prerender-safe (no browser APIs are
 * touched until the visitor clicks).
 */
@Component({
  selector: 'app-learn-tones',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule, LocalizeLinkPipe, AudioButtonComponent],
  templateUrl: './learn-tones.component.html',
  styleUrl: './learn-tones.component.scss'
})
export class LearnTonesComponent {

  readonly tones = TONES;
  readonly toneOptions = QUIZ_TONE_OPTIONS;
  readonly audioBase = 'assets/audio/learn/';
  /** Visual pitch contour per tone, shown next to the tone number. */
  readonly arrows: Record<number, string> = { 1: '→', 2: '↗', 3: '↘↗', 4: '↘', 5: '·' };

  quizPhase: 'idle' | 'question' | 'result' = 'idle';
  quizIndex = 0;                    // 1-based while playing
  score = 0;
  answered = false;
  selectedTone: number | null = null;
  current: ToneQuizItem | null = null;
  private items: ToneQuizItem[] = [];

  @ViewChild('quizAudio') private quizAudio?: AudioButtonComponent;

  constructor(private transloco: TranslocoService) {
    wirePageJsonLd(lang => this.buildJsonLd(lang));
  }

  /** Bilingual data fields (meaningEn/Es …) follow the active UI language. */
  get es(): boolean {
    return this.transloco.getActiveLang() === 'es';
  }

  get totalQuestions(): number {
    return TONE_QUIZ.length;
  }

  get progressPercent(): number {
    return Math.round((this.quizIndex / this.totalQuestions) * 100);
  }

  get resultKey(): 'perfect' | 'good' | 'keep' {
    return quizResultKey(this.score, this.totalQuestions);
  }

  startQuiz(): void {
    this.items = [...TONE_QUIZ];
    for (let i = this.items.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [this.items[i], this.items[j]] = [this.items[j], this.items[i]];
    }
    this.quizPhase = 'question';
    this.quizIndex = 0;
    this.score = 0;
    this.nextQuestion();
  }

  answerTone(tone: number): void {
    if (this.answered || !this.current) return;
    this.answered = true;
    this.selectedTone = tone;
    if (tone === this.current.tone) this.score++;
    // Brief feedback (the revealed syllable + coloring), then advance.
    setTimeout(() => this.advance(), 900);
  }

  private advance(): void {
    if (this.quizIndex >= this.totalQuestions) {
      this.quizPhase = 'result';
    } else {
      this.nextQuestion();
    }
  }

  private nextQuestion(): void {
    this.quizIndex++;
    this.current = this.items[this.quizIndex - 1];
    this.answered = false;
    this.selectedTone = null;
    // Auto-play once the view has rendered the new question's audio button.
    // If the browser blocks it (autoplay policy), the button is right there.
    setTimeout(() => this.quizAudio?.play());
  }

  private buildJsonLd(lang: Lang): object {
    const es = lang === 'es';
    return {
      '@context': 'https://schema.org',
      '@type': 'LearningResource',
      name: es ? 'Los tonos del chino — Los 4 tonos del mandarín con audio y quiz' : 'Chinese Tones Explained — The 4 Mandarin Tones with Audio & Quiz',
      description: es
        ? 'Los 4 tonos del chino y el tono neutro con el ejemplo clásico de «ma», audio de cada tono y un quiz de 10 preguntas para entrenar el oído.'
        : 'The 4 Chinese tones plus the neutral tone with the classic “ma” example, audio for every tone and a 10-question ear-training quiz.',
      url: SITE_URL + addLangPrefix('/learn/tones', lang),
      learningResourceType: 'Lesson',
      educationalLevel: 'Beginner',
      isAccessibleForFree: true,
      inLanguage: lang,
      teaches: es ? 'Los cuatro tonos del mandarín y el tono neutro' : 'The four Mandarin tones and the neutral tone',
      isPartOf: {
        '@type': 'Course',
        name: es ? 'Aprende a leer chino desde cero' : 'Learn to Read Chinese from Zero',
        url: SITE_URL + addLangPrefix('/learn', lang)
      },
      provider: { '@type': 'Organization', name: 'ChineseReads', url: SITE_URL }
    };
  }
}
