import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoService } from '@jsverse/transloco';

/** Una frase china que el corazón "regala" al pulsarlo. */
export interface LikePhrase {
  chinese: string;
  pinyin: string;
  english: string;
  spanish: string;
}

// Frases de ánimo y refranes sobre aprender. Son datos (chino/pinyin fijos,
// traducción bilingüe embebida), no textos de UI: por eso viven aquí y no en
// los diccionarios de Transloco, igual que los títulos EN/ES de la BBDD.
export const LIKE_PHRASES: LikePhrase[] = [
  { chinese: '加油！', pinyin: 'jiā yóu', english: 'Keep going!', spanish: '¡Ánimo!' },
  { chinese: '你真棒！', pinyin: 'nǐ zhēn bàng', english: "You're awesome!", spanish: '¡Eres genial!' },
  { chinese: '太好了！', pinyin: 'tài hǎo le', english: 'Great!', spanish: '¡Genial!' },
  { chinese: '我爱读书', pinyin: 'wǒ ài dú shū', english: 'I love reading', spanish: 'Me encanta leer' },
  { chinese: '熟能生巧', pinyin: 'shú néng shēng qiǎo', english: 'Practice makes perfect', spanish: 'La práctica hace al maestro' },
  { chinese: '学无止境', pinyin: 'xué wú zhǐ jìng', english: 'Learning never ends', spanish: 'Nunca se acaba de aprender' },
  { chinese: '心想事成', pinyin: 'xīn xiǎng shì chéng', english: 'May your wishes come true', spanish: 'Que tus deseos se cumplan' },
  { chinese: '天道酬勤', pinyin: 'tiān dào chóu qín', english: 'Hard work pays off', spanish: 'El esfuerzo tiene recompensa' },
  { chinese: '坚持就是胜利', pinyin: 'jiān chí jiù shì shèng lì', english: 'Persistence is victory', spanish: 'La constancia es la victoria' },
  { chinese: '万事开头难', pinyin: 'wàn shì kāi tóu nán', english: 'The first step is always the hardest', spanish: 'Lo más difícil es empezar' },
  { chinese: '每天进步一点点', pinyin: 'měi tiān jìn bù yì diǎn diǎn', english: 'A little progress every day', spanish: 'Un poquito de progreso cada día' },
  { chinese: '有志者事竟成', pinyin: 'yǒu zhì zhě shì jìng chéng', english: "Where there's a will, there's a way", spanish: 'Querer es poder' },
  { chinese: '温故而知新', pinyin: 'wēn gù ér zhī xīn', english: 'Review the old to learn the new', spanish: 'Repasa lo viejo y descubre lo nuevo' },
  { chinese: '好好学习，天天向上', pinyin: 'hǎo hǎo xué xí, tiān tiān xiàng shàng', english: 'Study hard, improve every day', spanish: 'Estudia mucho, mejora cada día' },
  { chinese: '千里之行，始于足下', pinyin: 'qiān lǐ zhī xíng, shǐ yú zú xià', english: 'A thousand-mile journey begins with one step', spanish: 'Un viaje de mil li empieza con un paso' },
  { chinese: '笑一笑，十年少', pinyin: 'xiào yi xiào, shí nián shào', english: 'A smile makes you ten years younger', spanish: 'Una sonrisa te quita diez años' },
  { chinese: '读万卷书，行万里路', pinyin: 'dú wàn juàn shū, xíng wàn lǐ lù', english: 'Read ten thousand books, travel ten thousand miles', spanish: 'Lee diez mil libros, recorre diez mil li' },
  { chinese: '世上无难事，只怕有心人', pinyin: 'shì shàng wú nán shì, zhǐ pà yǒu xīn rén', english: 'Nothing is impossible to a willing heart', spanish: 'Nada es imposible para quien pone el corazón' }
];

interface HeartParticle {
  id: number;
  x: number;
  y: number;
  dx: number;
  size: number;
  duration: number;
  delay: number;
  color: string;
}

/**
 * La sorpresa del corazón: al dar "me gusta" a un texto, lanza una lluvia de
 * corazones desde el punto pulsado y muestra una frase china aleatoria con
 * pinyin y traducción. Overlay fijo sin eventos de puntero; solo se monta en
 * el DOM mientras hay animación viva, así el prerender/SEO no ve nada.
 */
@Component({
  selector: 'app-heart-burst',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './heart-burst.component.html',
  styleUrl: './heart-burst.component.scss'
})
export class HeartBurstComponent implements OnDestroy {

  particles: HeartParticle[] = [];
  // Array de 0-1 elementos: un objeto nuevo por frase recrea el nodo y con él
  // la animación CSS (reasignar el texto no la reiniciaría).
  phraseBox: { id: number; phrase: LikePhrase }[] = [];

  private nextId = 0;
  private lastPhraseIndex = -1;
  private timeouts: ReturnType<typeof setTimeout>[] = [];

  private static readonly COLORS = ['#e74c3c', '#ff6b81', '#e84393', '#ff8fa3', '#d63031'];
  private static readonly HEARTS_PER_BURST = 12;
  private static readonly PARTICLES_MS = 2400;
  private static readonly PHRASE_MS = 3200; // = duración de la animación phrase-pop

  constructor(private transloco: TranslocoService) { }

  get activeLang(): string {
    return this.transloco.getActiveLang();
  }

  /** Lanza la sorpresa desde el punto del clic (o el centro del botón si fue teclado). */
  burstFromEvent(event: MouseEvent): void {
    let x = event.clientX;
    let y = event.clientY;
    if (!x && !y && event.target instanceof Element) {
      const rect = event.target.getBoundingClientRect();
      x = rect.left + rect.width / 2;
      y = rect.top + rect.height / 2;
    }
    this.burst(x, y);
  }

  burst(x: number, y: number): void {
    const born: HeartParticle[] = [];
    for (let i = 0; i < HeartBurstComponent.HEARTS_PER_BURST; i++) {
      born.push({
        id: this.nextId++,
        x, y,
        dx: (Math.random() - 0.5) * 240,
        size: 0.8 + Math.random() * 1.2,
        duration: 1.1 + Math.random() * 0.9,
        delay: Math.random() * 0.25,
        color: HeartBurstComponent.COLORS[Math.floor(Math.random() * HeartBurstComponent.COLORS.length)]
      });
    }
    this.particles = [...this.particles, ...born];
    this.schedule(() => {
      const ids = new Set(born.map(p => p.id));
      this.particles = this.particles.filter(p => !ids.has(p.id));
    }, HeartBurstComponent.PARTICLES_MS);

    let index = Math.floor(Math.random() * LIKE_PHRASES.length);
    if (index === this.lastPhraseIndex) {
      index = (index + 1) % LIKE_PHRASES.length;
    }
    this.lastPhraseIndex = index;
    const box = { id: this.nextId++, phrase: LIKE_PHRASES[index] };
    this.phraseBox = [box];
    this.schedule(() => {
      if (this.phraseBox.length && this.phraseBox[0].id === box.id) {
        this.phraseBox = [];
      }
    }, HeartBurstComponent.PHRASE_MS);
  }

  trackById(_: number, item: { id: number }): number {
    return item.id;
  }

  private schedule(fn: () => void, ms: number): void {
    this.timeouts.push(setTimeout(fn, ms));
  }

  ngOnDestroy(): void {
    this.timeouts.forEach(clearTimeout);
  }
}
