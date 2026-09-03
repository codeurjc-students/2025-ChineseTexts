/**
 * Lesson 1 of the beginner tutorial (/learn/pinyin): the complete pinyin sound
 * inventory as it is taught in a first class at a Chinese school (声母 / 韵母
 * tables), in the school order:
 *
 *   1. simple vowels        a o e i u ü                     (单韵母, 6)
 *   2. consonants           b p m f · d t n l · g k h · j q x · zh ch sh r · z c s · y w   (声母, 23)
 *   3. diphthongs           ai ei ui ao ou iu ie üe         (复韵母, 8)
 *   4. nasal finals         an en in un ün · ang eng ing ong (鼻韵母, 9)
 *
 * Every sound has one HSK-level example word with a static mp3 under
 * assets/audio/learn/ (see frontend/scripts/generate-learn-audio.mjs and
 * frontend/scripts/check-learn-audio.mjs).
 */

export interface PinyinSound {
  symbol: string;               // 'b', 'sh', 'ao', 'ü' …
  soundsLikeEn: string;         // approximation for an English speaker
  soundsLikeEs: string;         // approximation for a Spanish speaker
  exampleSyllable: string;      // 'bā'
  exampleHanzi: string;         // 八
  exampleMeaningEn: string;
  exampleMeaningEs: string;
  audio: string;                // filename under assets/audio/learn/
}

/** 单韵母 — the six simple vowels. */
export const SIMPLE_VOWELS: PinyinSound[] = [
  {
    symbol: 'a',
    soundsLikeEn: 'like “a” in “father”, mouth wide open',
    soundsLikeEs: 'como la “a” de «casa», con la boca bien abierta',
    exampleSyllable: 'mā', exampleHanzi: '妈',
    exampleMeaningEn: 'mother', exampleMeaningEs: 'madre',
    audio: 'ma1.mp3'
  },
  {
    symbol: 'o',
    soundsLikeEn: 'like “o” in “more”, lips rounded',
    soundsLikeEs: 'como la “o” de «flor», con los labios redondeados',
    exampleSyllable: 'wǒ', exampleHanzi: '我',
    exampleMeaningEn: 'I, me', exampleMeaningEs: 'yo',
    audio: 'wo3.mp3'
  },
  {
    symbol: 'e',
    soundsLikeEn: 'an unrounded “uh”, like “her” without the r',
    soundsLikeEs: 'un sonido entre “e” y “o”, con la boca relajada y sin redondear los labios',
    exampleSyllable: 'hē', exampleHanzi: '喝',
    exampleMeaningEn: 'to drink', exampleMeaningEs: 'beber',
    audio: 'he1.mp3'
  },
  {
    symbol: 'i',
    soundsLikeEn: 'like “ee” in “see”',
    soundsLikeEs: 'como la “i” de «sí»',
    exampleSyllable: 'yī', exampleHanzi: '一',
    exampleMeaningEn: 'one', exampleMeaningEs: 'uno',
    audio: 'yi1.mp3'
  },
  {
    symbol: 'u',
    soundsLikeEn: 'like “oo” in “food”, lips tightly rounded',
    soundsLikeEs: 'como la “u” de «luna», con los labios muy redondeados',
    exampleSyllable: 'wǔ', exampleHanzi: '五',
    exampleMeaningEn: 'five', exampleMeaningEs: 'cinco',
    audio: 'wu3.mp3'
  },
  {
    symbol: 'ü',
    soundsLikeEn: 'say “ee” and round your lips as for “oo” — like French “u” or German “ü”',
    soundsLikeEs: 'di una “i” y, sin moverla, redondea los labios como para una “u” — como la “u” francesa',
    exampleSyllable: 'yú', exampleHanzi: '鱼',
    exampleMeaningEn: 'fish', exampleMeaningEs: 'pez',
    audio: 'yu2.mp3'
  },
];

/**
 * 声母 — the 23 consonants, in the seven groups of the school chart. Each group
 * shares a tongue position; inside a group, b/p, d/t, g/k, j/q, zh/ch and z/c
 * are the same sound without and with a puff of air.
 */
export const CONSONANT_GROUPS: PinyinSound[][] = [
  [ // b p m f — lips
    {
      symbol: 'b',
      soundsLikeEn: 'like “p” in “spin” — soft, no puff of air',
      soundsLikeEs: 'como una “p” suave, sin soplo de aire (entre la b y la p del español)',
      exampleSyllable: 'bā', exampleHanzi: '八',
      exampleMeaningEn: 'eight', exampleMeaningEs: 'ocho',
      audio: 'ba1.mp3'
    },
    {
      symbol: 'p',
      soundsLikeEn: 'like “p” in “pot”, with a strong puff of air',
      soundsLikeEs: 'como la “p” de «papá», pero con un soplo de aire fuerte',
      exampleSyllable: 'pǎo', exampleHanzi: '跑',
      exampleMeaningEn: 'to run', exampleMeaningEs: 'correr',
      audio: 'pao3.mp3'
    },
    {
      symbol: 'm',
      soundsLikeEn: 'like “m” in “mother”',
      soundsLikeEs: 'como la “m” de «madre»',
      exampleSyllable: 'mā', exampleHanzi: '妈',
      exampleMeaningEn: 'mother', exampleMeaningEs: 'madre',
      audio: 'ma1.mp3'
    },
    {
      symbol: 'f',
      soundsLikeEn: 'like “f” in “food”',
      soundsLikeEs: 'como la “f” de «foto»',
      exampleSyllable: 'fàn', exampleHanzi: '饭',
      exampleMeaningEn: 'meal, cooked rice', exampleMeaningEs: 'comida, arroz',
      audio: 'fan4.mp3'
    },
  ],
  [ // d t n l — tip of the tongue
    {
      symbol: 'd',
      soundsLikeEn: 'like “t” in “stop” — soft, no puff of air',
      soundsLikeEs: 'como una “t” suave, sin soplo de aire (entre la d y la t del español)',
      exampleSyllable: 'dà', exampleHanzi: '大',
      exampleMeaningEn: 'big', exampleMeaningEs: 'grande',
      audio: 'da4.mp3'
    },
    {
      symbol: 't',
      soundsLikeEn: 'like “t” in “top”, with a strong puff of air',
      soundsLikeEs: 'como la “t” de «té», pero con un soplo de aire fuerte',
      exampleSyllable: 'tiān', exampleHanzi: '天',
      exampleMeaningEn: 'sky, day', exampleMeaningEs: 'cielo, día',
      audio: 'tian1.mp3'
    },
    {
      symbol: 'n',
      soundsLikeEn: 'like “n” in “no”',
      soundsLikeEs: 'como la “n” de «no»',
      exampleSyllable: 'nǐ', exampleHanzi: '你',
      exampleMeaningEn: 'you', exampleMeaningEs: 'tú',
      audio: 'ni3.mp3'
    },
    {
      symbol: 'l',
      soundsLikeEn: 'like “l” in “love”',
      soundsLikeEs: 'como la “l” de «luna»',
      exampleSyllable: 'lái', exampleHanzi: '来',
      exampleMeaningEn: 'to come', exampleMeaningEs: 'venir',
      audio: 'lai2.mp3'
    },
  ],
  [ // g k h — back of the tongue
    {
      symbol: 'g',
      soundsLikeEn: 'like “k” in “skin” — soft, no puff of air',
      soundsLikeEs: 'como la “g” de «gato», sin soplo de aire (suena entre g y k)',
      exampleSyllable: 'gǒu', exampleHanzi: '狗',
      exampleMeaningEn: 'dog', exampleMeaningEs: 'perro',
      audio: 'gou3.mp3'
    },
    {
      symbol: 'k',
      soundsLikeEn: 'like “k” in “kite”, with a strong puff of air',
      soundsLikeEs: 'como la “c” de «casa», pero con un soplo de aire fuerte',
      exampleSyllable: 'kàn', exampleHanzi: '看',
      exampleMeaningEn: 'to look, to watch', exampleMeaningEs: 'mirar, ver',
      audio: 'kan4.mp3'
    },
    {
      symbol: 'h',
      soundsLikeEn: 'like “h” in “hot”, slightly rougher',
      soundsLikeEs: 'como una “j” suave, como la de «jamón»',
      exampleSyllable: 'hē', exampleHanzi: '喝',
      exampleMeaningEn: 'to drink', exampleMeaningEs: 'beber',
      audio: 'he1.mp3'
    },
  ],
  [ // j q x — smiling, tip of the tongue down
    {
      symbol: 'j',
      soundsLikeEn: 'like “j” in “jeep”, lips spread as if smiling — no puff of air',
      soundsLikeEs: 'como una “ch” muy suave dicha sonriendo, sin soplo de aire',
      exampleSyllable: 'jiā', exampleHanzi: '家',
      exampleMeaningEn: 'home, family', exampleMeaningEs: 'casa, familia',
      audio: 'jia1.mp3'
    },
    {
      symbol: 'q',
      soundsLikeEn: 'like “ch” in “cheese”, lips spread as if smiling — with a puff of air',
      soundsLikeEs: 'como una “ch” dicha sonriendo, con un soplo de aire fuerte',
      exampleSyllable: 'qī', exampleHanzi: '七',
      exampleMeaningEn: 'seven', exampleMeaningEs: 'siete',
      audio: 'qi1.mp3'
    },
    {
      symbol: 'x',
      soundsLikeEn: 'like a light “sh” in “sheep”, lips spread as if smiling',
      soundsLikeEs: 'como una “sh” muy suave dicha sonriendo, con los labios estirados',
      exampleSyllable: 'xiǎo', exampleHanzi: '小',
      exampleMeaningEn: 'small', exampleMeaningEs: 'pequeño',
      audio: 'xiao3.mp3'
    },
  ],
  [ // zh ch sh r — tongue curled back
    {
      symbol: 'zh',
      soundsLikeEn: 'like “j” in “judge”, tongue curled back — no puff of air',
      soundsLikeEs: 'como una “ch” sin soplo de aire, con la lengua curvada hacia atrás',
      exampleSyllable: 'zhōng', exampleHanzi: '中',
      exampleMeaningEn: 'middle, centre', exampleMeaningEs: 'medio, centro',
      audio: 'zhong1.mp3'
    },
    {
      symbol: 'ch',
      soundsLikeEn: 'like “ch” in “church”, tongue curled back — with a puff of air',
      soundsLikeEs: 'como la “ch” de «chico», con la lengua curvada hacia atrás y un soplo de aire',
      exampleSyllable: 'chī', exampleHanzi: '吃',
      exampleMeaningEn: 'to eat', exampleMeaningEs: 'comer',
      audio: 'chi1.mp3'
    },
    {
      symbol: 'sh',
      soundsLikeEn: 'like “sh” in “ship”, tongue curled back',
      soundsLikeEs: 'como la “sh” inglesa de «show», con la lengua curvada hacia atrás',
      exampleSyllable: 'shū', exampleHanzi: '书',
      exampleMeaningEn: 'book', exampleMeaningEs: 'libro',
      audio: 'shu1.mp3'
    },
    {
      symbol: 'r',
      soundsLikeEn: 'between “r” in “red” and “s” in “measure”, tongue curled back — never rolled',
      soundsLikeEs: 'como la “r” inglesa de «red», con la lengua curvada hacia atrás — nunca vibra como la r española',
      exampleSyllable: 'rén', exampleHanzi: '人',
      exampleMeaningEn: 'person', exampleMeaningEs: 'persona',
      audio: 'ren2.mp3'
    },
  ],
  [ // z c s — flat tongue, behind the teeth
    {
      symbol: 'z',
      soundsLikeEn: 'like “ds” in “kids” — no puff of air',
      soundsLikeEs: 'como “ds”: una d y una s seguidas, sin soplo de aire',
      exampleSyllable: 'zì', exampleHanzi: '字',
      exampleMeaningEn: 'character, word', exampleMeaningEs: 'carácter, letra',
      audio: 'zi4.mp3'
    },
    {
      symbol: 'c',
      soundsLikeEn: 'like “ts” in “cats” — with a puff of air',
      soundsLikeEs: 'como “ts”: una t y una s seguidas, con soplo de aire',
      exampleSyllable: 'cài', exampleHanzi: '菜',
      exampleMeaningEn: 'vegetable, dish', exampleMeaningEs: 'verdura, plato',
      audio: 'cai4.mp3'
    },
    {
      symbol: 's',
      soundsLikeEn: 'like “s” in “see”',
      soundsLikeEs: 'como la “s” de «sol»',
      exampleSyllable: 'sān', exampleHanzi: '三',
      exampleMeaningEn: 'three', exampleMeaningEs: 'tres',
      audio: 'san1.mp3'
    },
  ],
  [ // y w — how i / u are written at the start of a syllable
    {
      symbol: 'y',
      soundsLikeEn: 'like “y” in “yes” — it is how “i” is written at the start of a syllable',
      soundsLikeEs: 'como la “y” de «yo» — es como se escribe la “i” a principio de sílaba',
      exampleSyllable: 'yī', exampleHanzi: '一',
      exampleMeaningEn: 'one', exampleMeaningEs: 'uno',
      audio: 'yi1.mp3'
    },
    {
      symbol: 'w',
      soundsLikeEn: 'like “w” in “we” — it is how “u” is written at the start of a syllable',
      soundsLikeEs: 'como la “u” de «hueso» — es como se escribe la “u” a principio de sílaba',
      exampleSyllable: 'wǒ', exampleHanzi: '我',
      exampleMeaningEn: 'I, me', exampleMeaningEs: 'yo',
      audio: 'wo3.mp3'
    },
  ],
];

/** The 23 consonants as a flat list, in school order. */
export const CONSONANTS: PinyinSound[] = CONSONANT_GROUPS.flat();

/** 复韵母 — the eight diphthongs (compound finals). */
export const DIPHTHONGS: PinyinSound[] = [
  {
    symbol: 'ai',
    soundsLikeEn: 'like “i” in “high”',
    soundsLikeEs: 'como “ai” en «aire»',
    exampleSyllable: 'ài', exampleHanzi: '爱',
    exampleMeaningEn: 'to love', exampleMeaningEs: 'amar',
    audio: 'ai4.mp3'
  },
  {
    symbol: 'ei',
    soundsLikeEn: 'like “ay” in “day”',
    soundsLikeEs: 'como “ei” en «reina»',
    exampleSyllable: 'běi', exampleHanzi: '北',
    exampleMeaningEn: 'north', exampleMeaningEs: 'norte',
    audio: 'bei3.mp3'
  },
  {
    symbol: 'ui',
    soundsLikeEn: 'like “way” — it is really “u-ei”',
    soundsLikeEs: 'suena “uei”, como en «buey»',
    exampleSyllable: 'shuǐ', exampleHanzi: '水',
    exampleMeaningEn: 'water', exampleMeaningEs: 'agua',
    audio: 'shui3.mp3'
  },
  {
    symbol: 'ao',
    soundsLikeEn: 'like “ow” in “cow”',
    soundsLikeEs: 'como “au” en «auto»',
    exampleSyllable: 'hǎo', exampleHanzi: '好',
    exampleMeaningEn: 'good', exampleMeaningEs: 'bueno',
    audio: 'hao3.mp3'
  },
  {
    symbol: 'ou',
    soundsLikeEn: 'like “o” in “go”',
    soundsLikeEs: 'una “o” que termina en “u”, como el inglés «go»',
    exampleSyllable: 'gǒu', exampleHanzi: '狗',
    exampleMeaningEn: 'dog', exampleMeaningEs: 'perro',
    audio: 'gou3.mp3'
  },
  {
    symbol: 'iu',
    soundsLikeEn: 'like “yo” in “yoga” said quickly — it is really “i-ou”',
    soundsLikeEs: 'suena “iou”: una i que pasa a “ou”',
    exampleSyllable: 'liù', exampleHanzi: '六',
    exampleMeaningEn: 'six', exampleMeaningEs: 'seis',
    audio: 'liu4.mp3'
  },
  {
    symbol: 'ie',
    soundsLikeEn: 'like “ye” in “yes”',
    soundsLikeEs: 'como “ie” en «tierra»',
    exampleSyllable: 'xiè', exampleHanzi: '谢',
    exampleMeaningEn: 'to thank (谢谢)', exampleMeaningEs: 'agradecer (谢谢)',
    audio: 'xie4.mp3'
  },
  {
    symbol: 'üe',
    soundsLikeEn: '“ü” gliding into “e”: “ü-eh”',
    soundsLikeEs: 'una “ü” que pasa a “e”: “üe”',
    exampleSyllable: 'yuè', exampleHanzi: '月',
    exampleMeaningEn: 'moon, month', exampleMeaningEs: 'luna, mes',
    audio: 'yue4.mp3'
  },
];

/** 鼻韵母 — the nine nasal finals: five ending in -n, four ending in -ng. */
export const NASAL_FINALS: PinyinSound[] = [
  {
    symbol: 'an',
    soundsLikeEn: 'like “an” in “can”',
    soundsLikeEs: 'como “an” en «pan»',
    exampleSyllable: 'sān', exampleHanzi: '三',
    exampleMeaningEn: 'three', exampleMeaningEs: 'tres',
    audio: 'san1.mp3'
  },
  {
    symbol: 'en',
    soundsLikeEn: 'like “un” in “fun”',
    soundsLikeEs: 'como “en” en «ten», con la e relajada',
    exampleSyllable: 'rén', exampleHanzi: '人',
    exampleMeaningEn: 'person', exampleMeaningEs: 'persona',
    audio: 'ren2.mp3'
  },
  {
    symbol: 'in',
    soundsLikeEn: 'like “een” in “seen”',
    soundsLikeEs: 'como “in” en «fin»',
    exampleSyllable: 'xīn', exampleHanzi: '新',
    exampleMeaningEn: 'new', exampleMeaningEs: 'nuevo',
    audio: 'xin1.mp3'
  },
  {
    symbol: 'un',
    soundsLikeEn: 'like “won” — it is really “u-en”',
    soundsLikeEs: 'suena “uen”: u + en',
    exampleSyllable: 'chūn', exampleHanzi: '春',
    exampleMeaningEn: 'spring (season)', exampleMeaningEs: 'primavera',
    audio: 'chun1.mp3'
  },
  {
    symbol: 'ün',
    soundsLikeEn: '“ü” + n, lips rounded',
    soundsLikeEs: '“ü” + n, con los labios redondeados',
    exampleSyllable: 'yún', exampleHanzi: '云',
    exampleMeaningEn: 'cloud', exampleMeaningEs: 'nube',
    audio: 'yun2.mp3'
  },
  {
    symbol: 'ang',
    soundsLikeEn: '“a” of “father” + “ng” of “song”',
    soundsLikeEs: 'como la “an” de «tango» — la n se hace en la garganta',
    exampleSyllable: 'máng', exampleHanzi: '忙',
    exampleMeaningEn: 'busy', exampleMeaningEs: 'ocupado',
    audio: 'mang2.mp3'
  },
  {
    symbol: 'eng',
    soundsLikeEn: 'like “ung” in “hung”',
    soundsLikeEs: 'como la “en” de «vengo» — la n se hace en la garganta',
    exampleSyllable: 'lěng', exampleHanzi: '冷',
    exampleMeaningEn: 'cold', exampleMeaningEs: 'frío',
    audio: 'leng3.mp3'
  },
  {
    symbol: 'ing',
    soundsLikeEn: 'like “ing” in “sing”',
    soundsLikeEs: 'como la “in” de «cinco» — la n se hace en la garganta',
    exampleSyllable: 'tīng', exampleHanzi: '听',
    exampleMeaningEn: 'to listen', exampleMeaningEs: 'escuchar',
    audio: 'ting1.mp3'
  },
  {
    symbol: 'ong',
    soundsLikeEn: '“oo” of “food” + “ng” of “song”',
    soundsLikeEs: 'entre “on” y “un”, terminando en la garganta como en «hongo»',
    exampleSyllable: 'zhōng', exampleHanzi: '中',
    exampleMeaningEn: 'middle, centre', exampleMeaningEs: 'medio, centro',
    audio: 'zhong1.mp3'
  },
];

/** Every sound of the lesson, in the order it appears on the page. */
export const ALL_PINYIN_SOUNDS: PinyinSound[] = [
  ...SIMPLE_VOWELS, ...CONSONANTS, ...DIPHTHONGS, ...NASAL_FINALS
];
