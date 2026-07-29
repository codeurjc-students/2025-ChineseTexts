import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { AudioButtonComponent } from '../audio-button/audio-button.component';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';
import { Lang, addLangPrefix } from '../../i18n/locale.util';
import { SITE_URL } from '../../services/seo.service';
import { wirePageJsonLd } from '../../utils/page-jsonld.util';
import { LEARN_CHARACTERS } from '../../data/learn-characters';

/**
 * Lesson 3 (/learn/characters): the first 12 HSK1 characters, each with
 * pinyin, meaning, audio and a real example word. Ends with the funnel CTA:
 * level test → graded texts. Static content + static audio — prerendered.
 */
@Component({
  selector: 'app-learn-characters',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule, LocalizeLinkPipe, AudioButtonComponent],
  templateUrl: './learn-characters.component.html',
  styleUrl: './learn-characters.component.scss'
})
export class LearnCharactersComponent {

  readonly characters = LEARN_CHARACTERS;
  readonly audioBase = 'assets/audio/learn/';

  constructor(private transloco: TranslocoService) {
    wirePageJsonLd(lang => this.buildJsonLd(lang));
  }

  /** Bilingual data fields (meaningEn/Es …) follow the active UI language. */
  get es(): boolean {
    return this.transloco.getActiveLang() === 'es';
  }

  private buildJsonLd(lang: Lang): object {
    const es = lang === 'es';
    return {
      '@context': 'https://schema.org',
      '@type': 'LearningResource',
      name: es ? 'Tus primeros 12 caracteres chinos con pinyin y audio' : 'Your First 12 Chinese Characters with Pinyin & Audio',
      description: es
        ? 'Lee tus primeros 12 caracteres chinos de HSK1 — 我, 你, 好, 水 y más — con pinyin, significado, palabra de ejemplo y audio.'
        : 'Read your first 12 HSK1 Chinese characters — 我, 你, 好, 水 and more — each with pinyin, meaning, an example word and audio.',
      url: SITE_URL + addLangPrefix('/learn/characters', lang),
      learningResourceType: 'Lesson',
      educationalLevel: 'Beginner',
      isAccessibleForFree: true,
      inLanguage: lang,
      teaches: es ? 'Los primeros 12 caracteres chinos del nivel HSK1' : 'The first 12 HSK1 Chinese characters',
      isPartOf: {
        '@type': 'Course',
        name: es ? 'Aprende a leer chino desde cero' : 'Learn to Read Chinese from Zero',
        url: SITE_URL + addLangPrefix('/learn', lang)
      },
      provider: { '@type': 'Organization', name: 'ChineseReads', url: SITE_URL }
    };
  }
}
