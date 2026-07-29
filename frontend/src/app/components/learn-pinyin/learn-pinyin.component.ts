import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { AudioButtonComponent } from '../audio-button/audio-button.component';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';
import { Lang, addLangPrefix } from '../../i18n/locale.util';
import { SITE_URL } from '../../services/seo.service';
import { wirePageJsonLd } from '../../utils/page-jsonld.util';
import { SAMPLE_INITIALS, SAMPLE_FINALS } from '../../data/learn-pinyin';

/**
 * Lesson 1 (/learn/pinyin): what pinyin is and how a syllable is built
 * (initial + final + tone), with sample sound tables and the sounds that trip
 * beginners up. Static content + static audio assets — free and prerendered.
 */
@Component({
  selector: 'app-learn-pinyin',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule, LocalizeLinkPipe, AudioButtonComponent],
  templateUrl: './learn-pinyin.component.html',
  styleUrl: './learn-pinyin.component.scss'
})
export class LearnPinyinComponent {

  readonly initials = SAMPLE_INITIALS;
  readonly finals = SAMPLE_FINALS;
  readonly audioBase = 'assets/audio/learn/';
  /** The four initials beginners struggle with, pulled from the sample table. */
  readonly pitfalls = SAMPLE_INITIALS.filter(s => ['x', 'q', 'zh', 'c'].includes(s.symbol));

  constructor(private transloco: TranslocoService) {
    wirePageJsonLd(lang => this.buildJsonLd(lang));
  }

  /** Bilingual data fields (soundsLikeEn/Es …) follow the active UI language. */
  get es(): boolean {
    return this.transloco.getActiveLang() === 'es';
  }

  private buildJsonLd(lang: Lang): object {
    const es = lang === 'es';
    return {
      '@context': 'https://schema.org',
      '@type': 'LearningResource',
      name: es ? '¿Qué es el pinyin? Aprende pinyin desde cero con audio' : 'What Is Pinyin? Learn Pinyin Basics with Audio',
      description: es
        ? 'Cómo cada sílaba china se forma con una inicial, una final y un tono, con ejemplos de audio y los sonidos que más confunden a los principiantes.'
        : 'How every Chinese syllable is built from an initial, a final and a tone, with audio examples and the sounds that trip up beginners.',
      url: SITE_URL + addLangPrefix('/learn/pinyin', lang),
      learningResourceType: 'Lesson',
      educationalLevel: 'Beginner',
      isAccessibleForFree: true,
      inLanguage: lang,
      teaches: es ? 'Fundamentos del pinyin: iniciales, finales y tonos' : 'Pinyin basics: initials, finals and tones',
      isPartOf: {
        '@type': 'Course',
        name: es ? 'Aprende a leer chino desde cero' : 'Learn to Read Chinese from Zero',
        url: SITE_URL + addLangPrefix('/learn', lang)
      },
      provider: { '@type': 'Organization', name: 'ChineseReads', url: SITE_URL }
    };
  }
}
