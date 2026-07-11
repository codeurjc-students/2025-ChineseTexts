import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { LoginService } from '../../services/login.service';
import { LocaleNavService } from '../../i18n/locale-nav.service';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';
import {
  LevelQuestion, QUESTION_BANK, TOTAL_QUESTIONS, START_LEVEL,
  nextLevel, estimateLevel
} from '../../data/hsk-level-test';

/**
 * Free, public HSK level test (SEO magnet + onboarding). A 12-question adaptive
 * staircase over the static question bank: right answers climb a level, misses drop
 * one, and the final estimate is the median of the converged tail. Fully client-side.
 *
 * Funnel: anonymous visitors get a RANGE ("around HSK2–HSK3") plus a sign-up CTA to
 * see the exact level and recommended readings; logged-in users get the exact level
 * and a direct link to their HSK reading list. The result is kept in localStorage so
 * returning users land on the right level.
 */
@Component({
  selector: 'app-level-test',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule, LocalizeLinkPipe],
  templateUrl: './level-test.component.html',
  styleUrl: './level-test.component.scss'
})
export class LevelTestComponent implements OnInit {

  phase: 'intro' | 'question' | 'result' = 'intro';

  questionNumber = 0;           // 1-based while testing
  current: LevelQuestion | null = null;
  options: string[] = [];
  selected: string | null = null;   // shown briefly for answer feedback
  answered = false;
  lastCorrect = false;

  resultLevel = 1;

  private level = START_LEVEL;
  private visited: number[] = [];
  private used = new Set<LevelQuestion>();

  constructor(
    private loginService: LoginService,
    private localeNav: LocaleNavService,
    private transloco: TranslocoService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    // Refresh session state so the result screen shows the right CTA.
    if (isPlatformBrowser(this.platformId)) {
      this.loginService.reqIsLogged().subscribe();
    }
  }

  get isLogged(): boolean {
    return this.loginService.isLogged();
  }

  get progressPercent(): number {
    return Math.round((this.questionNumber / TOTAL_QUESTIONS) * 100);
  }

  get totalQuestions(): number {
    return TOTAL_QUESTIONS;
  }

  /** Anonymous users see a range around the estimate instead of the exact level. */
  get resultRange(): string {
    const low = Math.max(1, this.resultLevel - 1);
    const high = Math.min(6, this.resultLevel + 1);
    return low === high ? `HSK${low}` : `HSK${low}–HSK${high}`;
  }

  start(): void {
    this.phase = 'question';
    this.level = START_LEVEL;
    this.visited = [];
    this.used.clear();
    this.questionNumber = 0;
    this.nextQuestion();
  }

  answer(option: string): void {
    if (this.answered || !this.current) return;
    this.answered = true;
    this.selected = option;
    this.lastCorrect = option === this.correctOf(this.current);
    // Brief feedback, then advance — long enough to register, short enough to flow.
    setTimeout(() => this.advance(), 650);
  }

  correctOf(q: LevelQuestion): string {
    return this.transloco.getActiveLang() === 'es' ? q.correctEs : q.correctEn;
  }

  goToReadings(): void {
    this.localeNav.navigate(['/texts', `HSK${this.resultLevel}`]);
  }

  signUp(): void {
    this.localeNav.navigate(['/signup']);
  }

  private advance(): void {
    this.level = nextLevel(this.level, this.lastCorrect);
    if (this.questionNumber >= TOTAL_QUESTIONS) {
      this.finish();
    } else {
      this.nextQuestion();
    }
  }

  private nextQuestion(): void {
    this.questionNumber++;
    this.visited.push(this.level);
    this.current = this.pickQuestion(this.level);
    this.used.add(this.current);
    this.options = this.shuffledOptions(this.current);
    this.answered = false;
    this.selected = null;
  }

  /** Random unused question of the target level (falls back to any unused one). */
  private pickQuestion(level: number): LevelQuestion {
    const fresh = QUESTION_BANK.filter(q => q.level === level && !this.used.has(q));
    const pool = fresh.length ? fresh : QUESTION_BANK.filter(q => !this.used.has(q));
    return pool[Math.floor(Math.random() * pool.length)];
  }

  private shuffledOptions(q: LevelQuestion): string[] {
    const es = this.transloco.getActiveLang() === 'es';
    const opts = es ? [q.correctEs, ...q.wrongEs] : [q.correctEn, ...q.wrongEn];
    for (let i = opts.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [opts[i], opts[j]] = [opts[j], opts[i]];
    }
    return opts;
  }

  private finish(): void {
    this.resultLevel = estimateLevel(this.visited);
    this.phase = 'result';
  }
}
