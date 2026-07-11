/**
 * The HSK level test: a curated, static question bank (10 items per HSK level) plus
 * the pure adaptive-staircase logic. Everything is client-side — the test is a free,
 * public, prerenderable page (SEO magnet) with zero backend cost.
 *
 * Each item is a vocabulary recognition question: the Chinese word (with pinyin) and
 * four translation options in the UI language. Distractors are hand-picked to be
 * plausible (same word class / semantic neighborhood) but unambiguously wrong.
 */

export interface LevelQuestion {
  chinese: string;
  pinyin: string;
  level: number;              // 1..6
  correctEn: string;
  correctEs: string;
  wrongEn: [string, string, string];
  wrongEs: [string, string, string];
}

export const QUESTION_BANK: LevelQuestion[] = [
  // ——— HSK 1 ———
  { chinese: '你好', pinyin: 'nǐ hǎo', level: 1, correctEn: 'hello', correctEs: 'hola',
    wrongEn: ['goodbye', 'thank you', 'sorry'], wrongEs: ['adiós', 'gracias', 'perdón'] },
  { chinese: '水', pinyin: 'shuǐ', level: 1, correctEn: 'water', correctEs: 'agua',
    wrongEn: ['fire', 'tea', 'rice'], wrongEs: ['fuego', 'té', 'arroz'] },
  { chinese: '猫', pinyin: 'māo', level: 1, correctEn: 'cat', correctEs: 'gato',
    wrongEn: ['dog', 'bird', 'horse'], wrongEs: ['perro', 'pájaro', 'caballo'] },
  { chinese: '吃', pinyin: 'chī', level: 1, correctEn: 'to eat', correctEs: 'comer',
    wrongEn: ['to drink', 'to sleep', 'to speak'], wrongEs: ['beber', 'dormir', 'hablar'] },
  { chinese: '大', pinyin: 'dà', level: 1, correctEn: 'big', correctEs: 'grande',
    wrongEn: ['small', 'tall', 'fat'], wrongEs: ['pequeño', 'alto', 'gordo'] },
  { chinese: '人', pinyin: 'rén', level: 1, correctEn: 'person', correctEs: 'persona',
    wrongEn: ['animal', 'child', 'friend'], wrongEs: ['animal', 'niño', 'amigo'] },
  { chinese: '书', pinyin: 'shū', level: 1, correctEn: 'book', correctEs: 'libro',
    wrongEn: ['pen', 'table', 'school'], wrongEs: ['bolígrafo', 'mesa', 'escuela'] },
  { chinese: '今天', pinyin: 'jīntiān', level: 1, correctEn: 'today', correctEs: 'hoy',
    wrongEn: ['tomorrow', 'yesterday', 'now'], wrongEs: ['mañana', 'ayer', 'ahora'] },
  { chinese: '谢谢', pinyin: 'xièxie', level: 1, correctEn: 'thank you', correctEs: 'gracias',
    wrongEn: ['please', 'hello', 'excuse me'], wrongEs: ['por favor', 'hola', 'disculpa'] },
  { chinese: '狗', pinyin: 'gǒu', level: 1, correctEn: 'dog', correctEs: 'perro',
    wrongEn: ['cat', 'pig', 'sheep'], wrongEs: ['gato', 'cerdo', 'oveja'] },

  // ——— HSK 2 ———
  { chinese: '时间', pinyin: 'shíjiān', level: 2, correctEn: 'time', correctEs: 'tiempo',
    wrongEn: ['place', 'clock', 'weather'], wrongEs: ['lugar', 'reloj', 'clima'] },
  { chinese: '颜色', pinyin: 'yánsè', level: 2, correctEn: 'color', correctEs: 'color',
    wrongEn: ['shape', 'light', 'painting'], wrongEs: ['forma', 'luz', 'pintura'] },
  { chinese: '快', pinyin: 'kuài', level: 2, correctEn: 'fast', correctEs: 'rápido',
    wrongEn: ['slow', 'early', 'strong'], wrongEs: ['lento', 'temprano', 'fuerte'] },
  { chinese: '新', pinyin: 'xīn', level: 2, correctEn: 'new', correctEs: 'nuevo',
    wrongEn: ['old', 'clean', 'young'], wrongEs: ['viejo', 'limpio', 'joven'] },
  { chinese: '鱼', pinyin: 'yú', level: 2, correctEn: 'fish', correctEs: 'pescado',
    wrongEn: ['meat', 'chicken', 'egg'], wrongEs: ['carne', 'pollo', 'huevo'] },
  { chinese: '房间', pinyin: 'fángjiān', level: 2, correctEn: 'room', correctEs: 'habitación',
    wrongEn: ['house', 'kitchen', 'door'], wrongEs: ['casa', 'cocina', 'puerta'] },
  { chinese: '跑步', pinyin: 'pǎobù', level: 2, correctEn: 'to run', correctEs: 'correr',
    wrongEn: ['to walk', 'to jump', 'to swim'], wrongEs: ['caminar', 'saltar', 'nadar'] },
  { chinese: '便宜', pinyin: 'piányi', level: 2, correctEn: 'cheap', correctEs: 'barato',
    wrongEn: ['expensive', 'convenient', 'free'], wrongEs: ['caro', 'cómodo', 'gratis'] },
  { chinese: '左边', pinyin: 'zuǒbian', level: 2, correctEn: 'left side', correctEs: 'izquierda',
    wrongEn: ['right side', 'front', 'outside'], wrongEs: ['derecha', 'delante', 'fuera'] },
  { chinese: '帮助', pinyin: 'bāngzhù', level: 2, correctEn: 'to help', correctEs: 'ayudar',
    wrongEn: ['to ask', 'to teach', 'to give'], wrongEs: ['preguntar', 'enseñar', 'dar'] },

  // ——— HSK 3 ———
  { chinese: '环境', pinyin: 'huánjìng', level: 3, correctEn: 'environment', correctEs: 'entorno',
    wrongEn: ['situation', 'nature', 'garden'], wrongEs: ['situación', 'naturaleza', 'jardín'] },
  { chinese: '决定', pinyin: 'juédìng', level: 3, correctEn: 'to decide', correctEs: 'decidir',
    wrongEn: ['to doubt', 'to promise', 'to refuse'], wrongEs: ['dudar', 'prometer', 'rechazar'] },
  { chinese: '健康', pinyin: 'jiànkāng', level: 3, correctEn: 'health', correctEs: 'salud',
    wrongEn: ['strength', 'illness', 'exercise'], wrongEs: ['fuerza', 'enfermedad', 'ejercicio'] },
  { chinese: '安静', pinyin: 'ānjìng', level: 3, correctEn: 'quiet', correctEs: 'tranquilo',
    wrongEn: ['noisy', 'safe', 'lonely'], wrongEs: ['ruidoso', 'seguro', 'solitario'] },
  { chinese: '打扫', pinyin: 'dǎsǎo', level: 3, correctEn: 'to clean', correctEs: 'limpiar',
    wrongEn: ['to fix', 'to move', 'to wash clothes'], wrongEs: ['arreglar', 'mudarse', 'lavar la ropa'] },
  { chinese: '迟到', pinyin: 'chídào', level: 3, correctEn: 'to be late', correctEs: 'llegar tarde',
    wrongEn: ['to arrive early', 'to hurry', 'to wait'], wrongEs: ['llegar temprano', 'darse prisa', 'esperar'] },
  { chinese: '担心', pinyin: 'dānxīn', level: 3, correctEn: 'to worry', correctEs: 'preocuparse',
    wrongEn: ['to relax', 'to hope', 'to remember'], wrongEs: ['relajarse', 'esperar (desear)', 'recordar'] },
  { chinese: '简单', pinyin: 'jiǎndān', level: 3, correctEn: 'simple', correctEs: 'sencillo',
    wrongEn: ['difficult', 'short', 'clear'], wrongEs: ['difícil', 'corto', 'claro'] },
  { chinese: '重要', pinyin: 'zhòngyào', level: 3, correctEn: 'important', correctEs: 'importante',
    wrongEn: ['heavy', 'urgent', 'necessary'], wrongEs: ['pesado', 'urgente', 'necesario'] },
  { chinese: '习惯', pinyin: 'xíguàn', level: 3, correctEn: 'habit', correctEs: 'costumbre',
    wrongEn: ['hobby', 'rule', 'culture'], wrongEs: ['afición', 'norma', 'cultura'] },

  // ——— HSK 4 ———
  { chinese: '经验', pinyin: 'jīngyàn', level: 4, correctEn: 'experience', correctEs: 'experiencia',
    wrongEn: ['experiment', 'memory', 'knowledge'], wrongEs: ['experimento', 'recuerdo', 'conocimiento'] },
  { chinese: '解释', pinyin: 'jiěshì', level: 4, correctEn: 'to explain', correctEs: 'explicar',
    wrongEn: ['to solve', 'to understand', 'to translate'], wrongEs: ['resolver', 'entender', 'traducir'] },
  { chinese: '严重', pinyin: 'yánzhòng', level: 4, correctEn: 'serious (grave)', correctEs: 'grave',
    wrongEn: ['strict', 'heavy', 'dangerous'], wrongEs: ['estricto', 'pesado', 'peligroso'] },
  { chinese: '竞争', pinyin: 'jìngzhēng', level: 4, correctEn: 'competition', correctEs: 'competencia',
    wrongEn: ['cooperation', 'race', 'argument'], wrongEs: ['cooperación', 'carrera', 'discusión'] },
  { chinese: '温度', pinyin: 'wēndù', level: 4, correctEn: 'temperature', correctEs: 'temperatura',
    wrongEn: ['weather', 'warmth', 'degree'], wrongEs: ['clima', 'calidez', 'grado'] },
  { chinese: '放弃', pinyin: 'fàngqì', level: 4, correctEn: 'to give up', correctEs: 'rendirse',
    wrongEn: ['to let go', 'to lose', 'to rest'], wrongEs: ['soltar', 'perder', 'descansar'] },
  { chinese: '顺利', pinyin: 'shùnlì', level: 4, correctEn: 'smooth, without problems', correctEs: 'sin contratiempos',
    wrongEn: ['lucky', 'fast', 'obedient'], wrongEs: ['afortunado', 'rápido', 'obediente'] },
  { chinese: '增加', pinyin: 'zēngjiā', level: 4, correctEn: 'to increase', correctEs: 'aumentar',
    wrongEn: ['to decrease', 'to add up', 'to improve'], wrongEs: ['disminuir', 'sumar', 'mejorar'] },
  { chinese: '态度', pinyin: 'tàidù', level: 4, correctEn: 'attitude', correctEs: 'actitud',
    wrongEn: ['opinion', 'mood', 'behavior'], wrongEs: ['opinión', 'humor', 'comportamiento'] },
  { chinese: '咳嗽', pinyin: 'késou', level: 4, correctEn: 'to cough', correctEs: 'toser',
    wrongEn: ['to sneeze', 'to breathe', 'to shout'], wrongEs: ['estornudar', 'respirar', 'gritar'] },

  // ——— HSK 5 ———
  { chinese: '承认', pinyin: 'chéngrèn', level: 5, correctEn: 'to admit', correctEs: 'admitir',
    wrongEn: ['to deny', 'to accept a job', 'to promise'], wrongEs: ['negar', 'aceptar un trabajo', 'prometer'] },
  { chinese: '措施', pinyin: 'cuòshī', level: 5, correctEn: 'measure (action taken)', correctEs: 'medida (acción)',
    wrongEn: ['mistake', 'plan', 'law'], wrongEs: ['error', 'plan', 'ley'] },
  { chinese: '矛盾', pinyin: 'máodùn', level: 5, correctEn: 'contradiction', correctEs: 'contradicción',
    wrongEn: ['weapon', 'agreement', 'debate'], wrongEs: ['arma', 'acuerdo', 'debate'] },
  { chinese: '谨慎', pinyin: 'jǐnshèn', level: 5, correctEn: 'cautious', correctEs: 'prudente',
    wrongEn: ['nervous', 'serious', 'shy'], wrongEs: ['nervioso', 'serio', 'tímido'] },
  { chinese: '面临', pinyin: 'miànlín', level: 5, correctEn: 'to face (a situation)', correctEs: 'afrontar',
    wrongEn: ['to meet someone', 'to avoid', 'to look at'], wrongEs: ['reunirse', 'evitar', 'mirar'] },
  { chinese: '忽然', pinyin: 'hūrán', level: 5, correctEn: 'suddenly', correctEs: 'de repente',
    wrongEn: ['gradually', 'finally', 'recently'], wrongEs: ['gradualmente', 'finalmente', 'recientemente'] },
  { chinese: '利润', pinyin: 'lìrùn', level: 5, correctEn: 'profit', correctEs: 'beneficio (ganancia)',
    wrongEn: ['salary', 'interest rate', 'price'], wrongEs: ['salario', 'tipo de interés', 'precio'] },
  { chinese: '骄傲', pinyin: 'jiāo’ào', level: 5, correctEn: 'proud', correctEs: 'orgulloso',
    wrongEn: ['brave', 'confident', 'stubborn'], wrongEs: ['valiente', 'seguro de sí', 'terco'] },
  { chinese: '培养', pinyin: 'péiyǎng', level: 5, correctEn: 'to cultivate (a skill/person)', correctEs: 'formar (cultivar)',
    wrongEn: ['to plant', 'to educate children', 'to train animals'], wrongEs: ['plantar', 'criar niños', 'adiestrar'] },
  { chinese: '犹豫', pinyin: 'yóuyù', level: 5, correctEn: 'to hesitate', correctEs: 'dudar (vacilar)',
    wrongEn: ['to refuse', 'to think deeply', 'to regret'], wrongEs: ['rechazar', 'reflexionar', 'arrepentirse'] },

  // ——— HSK 6 ———
  { chinese: '琢磨', pinyin: 'zuómo', level: 6, correctEn: 'to ponder over', correctEs: 'darle vueltas (reflexionar)',
    wrongEn: ['to polish jade', 'to investigate', 'to discuss'], wrongEs: ['pulir jade', 'investigar', 'debatir'] },
  { chinese: '崛起', pinyin: 'juéqǐ', level: 6, correctEn: 'to rise (emerge powerfully)', correctEs: 'emerger (alzarse)',
    wrongEn: ['to stand up', 'to rebel', 'to grow old'], wrongEs: ['ponerse de pie', 'rebelarse', 'envejecer'] },
  { chinese: '债务', pinyin: 'zhàiwù', level: 6, correctEn: 'debt', correctEs: 'deuda',
    wrongEn: ['duty', 'loan application', 'tax'], wrongEs: ['deber', 'solicitud de préstamo', 'impuesto'] },
  { chinese: '慷慨', pinyin: 'kāngkǎi', level: 6, correctEn: 'generous', correctEs: 'generoso',
    wrongEn: ['wealthy', 'wasteful', 'kind-hearted'], wrongEs: ['adinerado', 'derrochador', 'bondadoso'] },
  { chinese: '敷衍', pinyin: 'fūyǎn', level: 6, correctEn: 'to do sth perfunctorily', correctEs: 'salir del paso',
    wrongEn: ['to apply medicine', 'to perform', 'to postpone'], wrongEs: ['aplicar medicina', 'actuar', 'posponer'] },
  { chinese: '疲惫', pinyin: 'píbèi', level: 6, correctEn: 'exhausted', correctEs: 'agotado',
    wrongEn: ['weak', 'sick', 'lazy'], wrongEs: ['débil', 'enfermo', 'perezoso'] },
  { chinese: '辉煌', pinyin: 'huīhuáng', level: 6, correctEn: 'brilliant, glorious', correctEs: 'esplendoroso',
    wrongEn: ['bright (of light)', 'famous', 'expensive'], wrongEs: ['luminoso', 'famoso', 'caro'] },
  { chinese: '遏制', pinyin: 'èzhì', level: 6, correctEn: 'to curb, to contain', correctEs: 'contener (frenar)',
    wrongEn: ['to control a machine', 'to forbid', 'to delay'], wrongEs: ['manejar una máquina', 'prohibir', 'retrasar'] },
  { chinese: '巅峰', pinyin: 'diānfēng', level: 6, correctEn: 'peak, summit (figurative)', correctEs: 'cúspide',
    wrongEn: ['mountain', 'goal', 'record'], wrongEs: ['montaña', 'meta', 'récord'] },
  { chinese: '踊跃', pinyin: 'yǒngyuè', level: 6, correctEn: 'eager, enthusiastic', correctEs: 'entusiasta (con avidez)',
    wrongEn: ['jumping', 'brave', 'crowded'], wrongEs: ['saltarín', 'valiente', 'abarrotado'] },
];

export const TOTAL_QUESTIONS = 12;
export const START_LEVEL = 2;
export const MIN_LEVEL = 1;
export const MAX_LEVEL = 6;

/** Staircase step: climb one level on a correct answer, drop one on a miss. */
export function nextLevel(current: number, correct: boolean): number {
  const next = correct ? current + 1 : current - 1;
  return Math.min(MAX_LEVEL, Math.max(MIN_LEVEL, next));
}

/**
 * Final estimate: the median of the levels visited in the second half of the test.
 * A staircase oscillates around the user's true level once it converges, so the
 * early exploratory steps are ignored and the median absorbs the oscillation.
 */
export function estimateLevel(visited: number[]): number {
  if (visited.length === 0) return MIN_LEVEL;
  const tail = visited.slice(Math.floor(visited.length / 2)).sort((a, b) => a - b);
  return tail[Math.floor(tail.length / 2)];
}
