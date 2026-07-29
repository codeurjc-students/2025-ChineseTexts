import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';

import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';
import { Lang, addLangPrefix } from '../../i18n/locale.util';
import { SITE_URL } from '../../services/seo.service';
import { wirePageJsonLd } from '../../utils/page-jsonld.util';

/**
 * Landing page of the beginner tutorial (/learn): what the visitor will learn
 * and the roadmap through the three lessons, ending in the level-test funnel
 * (lesson 1 → 2 → 3 → level test → graded texts). Fully static and
 * prerendered in both languages — a pure SEO/onboarding page.
 */
@Component({
  selector: 'app-learn-hub',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule, LocalizeLinkPipe],
  templateUrl: './learn-hub.component.html',
  styleUrl: './learn-hub.component.scss'
})
export class LearnHubComponent {

  constructor() {
    wirePageJsonLd(lang => this.buildJsonLd(lang));
  }

  /** schema.org Course: the tutorial as a free 3-lesson beginner course. */
  private buildJsonLd(lang: Lang): object {
    const es = lang === 'es';
    const url = (path: string) => SITE_URL + addLangPrefix(path, lang);
    return {
      '@context': 'https://schema.org',
      '@type': 'Course',
      name: es ? 'Aprende a leer chino desde cero' : 'Learn to Read Chinese from Zero',
      description: es
        ? 'Tutorial gratuito de 3 lecciones para principiantes absolutos: pinyin, los 4 tonos del mandarín con audio y tus primeros 12 caracteres chinos.'
        : 'A free 3-lesson tutorial for absolute beginners: pinyin, the 4 Mandarin tones with audio, and your first 12 Chinese characters.',
      url: url('/learn'),
      provider: {
        '@type': 'Organization',
        name: 'ChineseReads',
        url: SITE_URL,
        logo: `${SITE_URL}/icon-512.png`
      },
      isAccessibleForFree: true,
      inLanguage: lang,
      teaches: es
        ? 'Fundamentos de pinyin, los cuatro tonos del mandarín, los primeros 12 caracteres chinos (HSK1)'
        : 'Pinyin basics, the four Mandarin tones, the first 12 Chinese characters (HSK1)',
      hasPart: [
        {
          '@type': 'LearningResource',
          name: es ? '¿Qué es el pinyin?' : 'What is pinyin?',
          url: url('/learn/pinyin')
        },
        {
          '@type': 'LearningResource',
          name: es ? 'Los 4 tonos del chino' : 'The 4 Chinese tones',
          url: url('/learn/tones')
        },
        {
          '@type': 'LearningResource',
          name: es ? 'Tus primeros 12 caracteres chinos' : 'Your first 12 Chinese characters',
          url: url('/learn/characters')
        }
      ]
    };
  }
}
