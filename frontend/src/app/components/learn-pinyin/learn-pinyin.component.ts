import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { AudioButtonComponent } from '../audio-button/audio-button.component';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';
import { Lang, addLangPrefix } from '../../i18n/locale.util';
import { SITE_URL } from '../../services/seo.service';
import { wirePageJsonLd } from '../../utils/page-jsonld.util';
import { SIMPLE_VOWELS, CONSONANT_GROUPS, DIPHTHONGS, NASAL_FINALS } from '../../data/learn-pinyin';

/**
 * Lesson 1 (/learn/pinyin): what pinyin is, how a syllable is built
 * (initial + final + tone) and the full sound chart in the order of a first
 * class at a Chinese school — simple vowels, consonants, diphthongs and nasal
 * finals — each with an example word and audio. Static content + static audio
 * assets — free and prerendered.
 */
@Component({
  selector: 'app-learn-pinyin',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule, LocalizeLinkPipe, AudioButtonComponent],
  templateUrl: './learn-pinyin.component.html',
  styleUrl: './learn-pinyin.component.scss'
})
export class LearnPinyinComponent {

  readonly vowels = SIMPLE_VOWELS;
  readonly consonantGroups = CONSONANT_GROUPS;
  readonly diphthongs = DIPHTHONGS;
  readonly nasals = NASAL_FINALS;
  readonly audioBase = 'assets/audio/learn/';

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
        ? 'Cómo cada sílaba china se forma con una inicial, una final y un tono, y la tabla completa de sonidos del pinyin — vocales, consonantes, diptongos y finales nasales — con audio de ejemplo.'
        : 'How every Chinese syllable is built from an initial, a final and a tone, plus the complete pinyin sound chart — vowels, consonants, diphthongs and nasal finals — with example audio.',
      url: SITE_URL + addLangPrefix('/learn/pinyin', lang),
      learningResourceType: 'Lesson',
      educationalLevel: 'Beginner',
      isAccessibleForFree: true,
      inLanguage: lang,
      teaches: es
        ? 'Fundamentos del pinyin: vocales simples, consonantes, diptongos y finales nasales'
        : 'Pinyin basics: simple vowels, consonants, diphthongs and nasal finals',
      isPartOf: {
        '@type': 'Course',
        name: es ? 'Aprende a leer chino desde cero' : 'Learn to Read Chinese from Zero',
        url: SITE_URL + addLangPrefix('/learn', lang)
      },
      provider: { '@type': 'Organization', name: 'ChineseReads', url: SITE_URL }
    };
  }
}
