/**
 * Lesson 1 of the beginner tutorial (/learn/pinyin): a hand-picked sample of
 * initials and finals — enough to understand how a syllable is built and hear
 * the sounds that trip beginners up, without drowning a first-time visitor in
 * the full pinyin chart. Audio files are static mp3 assets under
 * assets/audio/learn/ (see frontend/scripts/generate-learn-audio.mjs).
 */

export interface PinyinSound {
  symbol: string;               // 'b', 'sh', 'ao', …
  soundsLikeEn: string;         // approximation for an English speaker
  soundsLikeEs: string;         // approximation for a Spanish speaker
  exampleSyllable: string;      // 'bā'
  exampleHanzi: string;         // 八
  exampleMeaningEn: string;
  exampleMeaningEs: string;
  audio: string;                // filename under assets/audio/learn/
}

export const SAMPLE_INITIALS: PinyinSound[] = [
  {
    symbol: 'b',
    soundsLikeEn: 'like “b” in “bay”, but softer — no puff of air',
    soundsLikeEs: 'como la “p” suave de «beso», sin soplo de aire',
    exampleSyllable: 'bā', exampleHanzi: '八',
    exampleMeaningEn: 'eight', exampleMeaningEs: 'ocho',
    audio: 'ba1.mp3'
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
    symbol: 'h',
    soundsLikeEn: 'like “h” in “hot”, slightly rougher',
    soundsLikeEs: 'como una “j” suave, entre la j de «jamón» y una h aspirada',
    exampleSyllable: 'hē', exampleHanzi: '喝',
    exampleMeaningEn: 'to drink', exampleMeaningEs: 'beber',
    audio: 'he1.mp3'
  },
  {
    symbol: 'sh',
    soundsLikeEn: 'like “sh” in “ship”, tongue curled back',
    soundsLikeEs: 'como la “sh” inglesa de «show», con la lengua hacia atrás',
    exampleSyllable: 'shū', exampleHanzi: '书',
    exampleMeaningEn: 'book', exampleMeaningEs: 'libro',
    audio: 'shu1.mp3'
  },
  {
    symbol: 'zh',
    soundsLikeEn: 'like “j” in “judge”, tongue curled back',
    soundsLikeEs: 'como una “ch” con la lengua curvada hacia atrás',
    exampleSyllable: 'zhōng', exampleHanzi: '中',
    exampleMeaningEn: 'middle', exampleMeaningEs: 'medio, centro',
    audio: 'zhong1.mp3'
  },
  {
    symbol: 'x',
    soundsLikeEn: 'like a soft “sh” said while smiling, lips spread wide',
    soundsLikeEs: 'como una “sh” muy suave dicha sonriendo, con los labios estirados',
    exampleSyllable: 'xiǎo', exampleHanzi: '小',
    exampleMeaningEn: 'small', exampleMeaningEs: 'pequeño',
    audio: 'xiao3.mp3'
  },
  {
    symbol: 'q',
    soundsLikeEn: 'like a soft “ch” said while smiling, lips spread wide',
    soundsLikeEs: 'como una “ch” suave dicha sonriendo, con los labios estirados',
    exampleSyllable: 'qī', exampleHanzi: '七',
    exampleMeaningEn: 'seven', exampleMeaningEs: 'siete',
    audio: 'qi1.mp3'
  },
  {
    symbol: 'c',
    soundsLikeEn: 'like “ts” in “cats”',
    soundsLikeEs: 'como “ts”: una t y una s seguidas',
    exampleSyllable: 'cài', exampleHanzi: '菜',
    exampleMeaningEn: 'vegetable, dish', exampleMeaningEs: 'verdura, plato',
    audio: 'cai4.mp3'
  },
];

export const SAMPLE_FINALS: PinyinSound[] = [
  {
    symbol: 'a',
    soundsLikeEn: 'like “a” in “father”',
    soundsLikeEs: 'como la “a” de «casa»',
    exampleSyllable: 'mā', exampleHanzi: '妈',
    exampleMeaningEn: 'mother', exampleMeaningEs: 'madre',
    audio: 'ma1.mp3'
  },
  {
    symbol: 'o',
    soundsLikeEn: 'like “o” in “more”, lips rounded',
    soundsLikeEs: 'como la “o” de «flor»',
    exampleSyllable: 'wǒ', exampleHanzi: '我',
    exampleMeaningEn: 'I, me', exampleMeaningEs: 'yo',
    audio: 'wo3.mp3'
  },
  {
    symbol: 'e',
    soundsLikeEn: 'an unrounded “uh”, like “her” without the r',
    soundsLikeEs: 'un sonido entre “e” y “o”, con la boca relajada',
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
    soundsLikeEn: 'like “oo” in “food”',
    soundsLikeEs: 'como la “u” de «luna»',
    exampleSyllable: 'shū', exampleHanzi: '书',
    exampleMeaningEn: 'book', exampleMeaningEs: 'libro',
    audio: 'shu1.mp3'
  },
  {
    symbol: 'ao',
    soundsLikeEn: 'like “ow” in “cow”',
    soundsLikeEs: 'como “au” en «auto»',
    exampleSyllable: 'hǎo', exampleHanzi: '好',
    exampleMeaningEn: 'good', exampleMeaningEs: 'bueno',
    audio: 'hao3.mp3'
  },
];
