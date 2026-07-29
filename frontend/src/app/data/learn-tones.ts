/**
 * Lesson 2 of the beginner tutorial (/learn/tones): the four Mandarin tones plus
 * the neutral tone, demonstrated with the classic "ma" set, and a 10-question
 * ear-training quiz. Everything is static and client-side — audio files are
 * pre-generated mp3 assets under assets/audio/learn/ (see
 * frontend/scripts/generate-learn-audio.mjs), so playing them costs nothing
 * and needs no account.
 */

export interface ToneInfo {
  tone: 1 | 2 | 3 | 4 | 5;      // 5 = neutral tone
  markedVowel: string;          // the tone mark on "a": ā á ǎ à a
  syllable: string;             // 'mā'
  hanzi: string;                // 妈
  meaningEn: string;
  meaningEs: string;
  contourEn: string;            // how the pitch moves, described for a beginner
  contourEs: string;
  audio: string;                // filename under assets/audio/learn/
  exampleHanzi: string;         // one more real word carrying the same tone
  examplePinyin: string;
  exampleMeaningEn: string;
  exampleMeaningEs: string;
  exampleAudio: string;
}

export const TONES: ToneInfo[] = [
  {
    tone: 1, markedVowel: 'ā', syllable: 'mā', hanzi: '妈',
    meaningEn: 'mother', meaningEs: 'madre',
    contourEn: 'High and flat, like holding a singing note.',
    contourEs: 'Alto y plano, como mantener una nota cantada.',
    audio: 'ma1.mp3',
    exampleHanzi: '天', examplePinyin: 'tiān',
    exampleMeaningEn: 'sky, day', exampleMeaningEs: 'cielo, día',
    exampleAudio: 'tian1.mp3'
  },
  {
    tone: 2, markedVowel: 'á', syllable: 'má', hanzi: '麻',
    meaningEn: 'hemp', meaningEs: 'cáñamo',
    contourEn: 'Rising, like asking “what?” in surprise.',
    contourEs: 'Ascendente, como preguntar «¿qué?» con sorpresa.',
    audio: 'ma2.mp3',
    exampleHanzi: '人', examplePinyin: 'rén',
    exampleMeaningEn: 'person', exampleMeaningEs: 'persona',
    exampleAudio: 'ren2.mp3'
  },
  {
    tone: 3, markedVowel: 'ǎ', syllable: 'mǎ', hanzi: '马',
    meaningEn: 'horse', meaningEs: 'caballo',
    contourEn: 'Dips down low, then rises back up.',
    contourEs: 'Baja hasta lo más grave y luego vuelve a subir.',
    audio: 'ma3.mp3',
    exampleHanzi: '水', examplePinyin: 'shuǐ',
    exampleMeaningEn: 'water', exampleMeaningEs: 'agua',
    exampleAudio: 'shui3.mp3'
  },
  {
    tone: 4, markedVowel: 'à', syllable: 'mà', hanzi: '骂',
    meaningEn: 'to scold', meaningEs: 'regañar',
    contourEn: 'Falls sharply, like a firm “No!”.',
    contourEs: 'Cae en seco, como un «¡No!» rotundo.',
    audio: 'ma4.mp3',
    exampleHanzi: '大', examplePinyin: 'dà',
    exampleMeaningEn: 'big', exampleMeaningEs: 'grande',
    exampleAudio: 'da4.mp3'
  },
  {
    tone: 5, markedVowel: 'a', syllable: 'ma', hanzi: '吗',
    meaningEn: 'question particle', meaningEs: 'partícula interrogativa',
    contourEn: 'Neutral: short, light and unstressed.',
    contourEs: 'Neutro: corto, ligero y sin acento.',
    audio: 'ma5.mp3',
    exampleHanzi: '妈妈', examplePinyin: 'māma',
    exampleMeaningEn: 'mom', exampleMeaningEs: 'mamá',
    exampleAudio: 'ma1ma5.mp3'
  },
];

/**
 * Ear-training item: the listener hears the audio and picks the tone. The
 * toneless `base` is shown while listening; the marked `syllable` is revealed
 * with the answer. The neutral tone is excluded to keep a clean 4-option UI.
 */
export interface ToneQuizItem {
  base: string;                 // 'ma' (no tone mark — shown before answering)
  syllable: string;             // 'mā' (revealed after answering)
  hanzi: string;                // 妈  (revealed after answering)
  tone: 1 | 2 | 3 | 4;
  audio: string;
}

/** Balanced bank: 3× tone 1, 3× tone 2, 2× tone 3, 2× tone 4 — all distinct syllables. */
export const TONE_QUIZ: ToneQuizItem[] = [
  { base: 'ma',   syllable: 'mā',   hanzi: '妈', tone: 1, audio: 'ma1.mp3' },
  { base: 'shu',  syllable: 'shū',  hanzi: '书', tone: 1, audio: 'shu1.mp3' },
  { base: 'ting', syllable: 'tīng', hanzi: '听', tone: 1, audio: 'ting1.mp3' },
  { base: 'lai',  syllable: 'lái',  hanzi: '来', tone: 2, audio: 'lai2.mp3' },
  { base: 'ren',  syllable: 'rén',  hanzi: '人', tone: 2, audio: 'ren2.mp3' },
  { base: 'yu',   syllable: 'yú',   hanzi: '鱼', tone: 2, audio: 'yu2.mp3' },
  { base: 'mai',  syllable: 'mǎi',  hanzi: '买', tone: 3, audio: 'mai3.mp3' },
  { base: 'wo',   syllable: 'wǒ',   hanzi: '我', tone: 3, audio: 'wo3.mp3' },
  { base: 'da',   syllable: 'dà',   hanzi: '大', tone: 4, audio: 'da4.mp3' },
  { base: 'ba',   syllable: 'bà',   hanzi: '爸', tone: 4, audio: 'ba4.mp3' },
];

export const QUIZ_TONE_OPTIONS = [1, 2, 3, 4] as const;

/**
 * Result bucket for the quiz score, used as an i18n key suffix
 * (learn.quiz.result.perfect / .good / .keep).
 */
export function quizResultKey(score: number, total: number): 'perfect' | 'good' | 'keep' {
  if (total <= 0) return 'keep';
  const ratio = score / total;
  if (ratio >= 0.9) return 'perfect';
  if (ratio >= 0.6) return 'good';
  return 'keep';
}
