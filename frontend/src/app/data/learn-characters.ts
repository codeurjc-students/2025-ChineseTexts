/**
 * Lesson 3 of the beginner tutorial (/learn/characters): a coherent starter set
 * of 12 HSK1 characters. They are chosen to compose each other's example words
 * (我 → 我们, 你 + 好 → 你好, 中 + 人 → 中国人 …), so by the end of the page the
 * visitor can genuinely read a few real words. Audio files are static mp3
 * assets under assets/audio/learn/ (see frontend/scripts/generate-learn-audio.mjs).
 */

export interface LearnCharacter {
  hanzi: string;
  pinyin: string;
  meaningEn: string;
  meaningEs: string;
  audio: string;                // filename under assets/audio/learn/
  exampleHanzi: string;         // a real word using this character
  examplePinyin: string;
  exampleMeaningEn: string;
  exampleMeaningEs: string;
  exampleAudio: string;
}

export const LEARN_CHARACTERS: LearnCharacter[] = [
  {
    hanzi: '我', pinyin: 'wǒ', meaningEn: 'I, me', meaningEs: 'yo', audio: 'wo3.mp3',
    exampleHanzi: '我们', examplePinyin: 'wǒmen',
    exampleMeaningEn: 'we, us', exampleMeaningEs: 'nosotros', exampleAudio: 'wo3men5.mp3'
  },
  {
    hanzi: '你', pinyin: 'nǐ', meaningEn: 'you', meaningEs: 'tú', audio: 'ni3.mp3',
    exampleHanzi: '你好', examplePinyin: 'nǐ hǎo',
    exampleMeaningEn: 'hello', exampleMeaningEs: 'hola', exampleAudio: 'ni3hao3.mp3'
  },
  {
    hanzi: '好', pinyin: 'hǎo', meaningEn: 'good', meaningEs: 'bueno, bien', audio: 'hao3.mp3',
    exampleHanzi: '很好', examplePinyin: 'hěn hǎo',
    exampleMeaningEn: 'very good', exampleMeaningEs: 'muy bien', exampleAudio: 'hen3hao3.mp3'
  },
  {
    hanzi: '是', pinyin: 'shì', meaningEn: 'to be', meaningEs: 'ser', audio: 'shi4.mp3',
    exampleHanzi: '我是', examplePinyin: 'wǒ shì',
    exampleMeaningEn: 'I am', exampleMeaningEs: 'yo soy', exampleAudio: 'wo3shi4.mp3'
  },
  {
    hanzi: '人', pinyin: 'rén', meaningEn: 'person', meaningEs: 'persona', audio: 'ren2.mp3',
    exampleHanzi: '中国人', examplePinyin: 'Zhōngguórén',
    exampleMeaningEn: 'Chinese person', exampleMeaningEs: 'persona china', exampleAudio: 'zhong1guo2ren2.mp3'
  },
  {
    hanzi: '大', pinyin: 'dà', meaningEn: 'big', meaningEs: 'grande', audio: 'da4.mp3',
    exampleHanzi: '大人', examplePinyin: 'dàrén',
    exampleMeaningEn: 'adult', exampleMeaningEs: 'adulto', exampleAudio: 'da4ren2.mp3'
  },
  {
    hanzi: '小', pinyin: 'xiǎo', meaningEn: 'small', meaningEs: 'pequeño', audio: 'xiao3.mp3',
    exampleHanzi: '大小', examplePinyin: 'dàxiǎo',
    exampleMeaningEn: 'size', exampleMeaningEs: 'tamaño', exampleAudio: 'da4xiao3.mp3'
  },
  {
    hanzi: '水', pinyin: 'shuǐ', meaningEn: 'water', meaningEs: 'agua', audio: 'shui3.mp3',
    exampleHanzi: '喝水', examplePinyin: 'hē shuǐ',
    exampleMeaningEn: 'to drink water', exampleMeaningEs: 'beber agua', exampleAudio: 'he1shui3.mp3'
  },
  {
    hanzi: '一', pinyin: 'yī', meaningEn: 'one', meaningEs: 'uno', audio: 'yi1.mp3',
    exampleHanzi: '一个', examplePinyin: 'yí gè',
    exampleMeaningEn: 'one (thing)', exampleMeaningEs: 'uno (una cosa)', exampleAudio: 'yi1ge4.mp3'
  },
  {
    hanzi: '二', pinyin: 'èr', meaningEn: 'two', meaningEs: 'dos', audio: 'er4.mp3',
    exampleHanzi: '二十', examplePinyin: 'èrshí',
    exampleMeaningEn: 'twenty', exampleMeaningEs: 'veinte', exampleAudio: 'er4shi2.mp3'
  },
  {
    hanzi: '三', pinyin: 'sān', meaningEn: 'three', meaningEs: 'tres', audio: 'san1.mp3',
    exampleHanzi: '三个', examplePinyin: 'sān gè',
    exampleMeaningEn: 'three (things)', exampleMeaningEs: 'tres (cosas)', exampleAudio: 'san1ge4.mp3'
  },
  {
    hanzi: '中', pinyin: 'zhōng', meaningEn: 'middle', meaningEs: 'medio, centro', audio: 'zhong1.mp3',
    exampleHanzi: '中国', examplePinyin: 'Zhōngguó',
    exampleMeaningEn: 'China', exampleMeaningEs: 'China', exampleAudio: 'zhong1guo2.mp3'
  },
];
